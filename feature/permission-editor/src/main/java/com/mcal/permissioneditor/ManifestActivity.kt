package com.mcal.permissioneditor

import com.mcal.neweditor.R
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.ReactivePreferences.isAnnotationPermission
import com.mcal.common.data.ReactivePreferences.setAnnotationPermission
import com.mcal.common.utils.writeToFile
import com.mcal.permissioneditor.adapter.ManifestAdapter
import com.mcal.permissioneditor.dialogs.PermissionDialog
import com.mcal.permissioneditor.model.Permission
import com.mcal.permissioneditor.utils.ManifestParser.load
import com.mcal.permissioneditor.utils.ManifestUtils
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ManifestActivity : CustomizedLangActivity(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, ActionMode.Callback, ManifestAdapter.Listener {
    private var actionMode: ActionMode? = null
    var isDataChange = false
    private lateinit var cardView: MaterialCardView
    private lateinit var mainAdapter: ManifestAdapter
    private lateinit var manifestFile: File
    private lateinit var manifestView: ListView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        manifestView = findViewById(R.id.manifest_view)
        cardView = findViewById(R.id.card_view)
        setupToolbar(R.id.toolbar, title = getString(R.string.permission_editor), back = true)
        intent.extras?.getString("path")?.let { path ->
            manifestFile = File(path)
            try {
                mainAdapter = ManifestAdapter(this, load(this, manifestFile))
                mainAdapter.setListener(this)
                manifestView.adapter = mainAdapter
                onDataSetChanged()
                manifestView.onItemClickListener = this
                manifestView.onItemLongClickListener = this
            } catch (th: Throwable) {
                Toast.makeText(this, th.message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        actionMode.menuInflater.inflate(R.menu.menu_multiselect_perm, menu)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        mainAdapter.isSelectMode = true
        mainAdapter.notifyDataSetChanged()
        when (item.itemId) {
            R.id.delete_perm -> {
                for (str in mainAdapter.selected.keys) {
                    ManifestAdapter.remove(mainAdapter.currentList, str)
                }
                isDataChange = true
                mainAdapter.notifyDataSetChanged()
            }
            R.id.select_all_perm -> {
                val selected = mainAdapter.selected
                for (i in 0 until mainAdapter.count) {
                    selected[mainAdapter.getItem(i).name] = java.lang.Boolean.TRUE
                }
                mainAdapter.notifyDataSetChanged()
            }
            R.id.unselect_all_perm -> {
                mainAdapter.selected.clear()
                mainAdapter.notifyDataSetChanged()
            }
            R.id.invert_all_perm -> {
                val selected = mainAdapter.selected
                for (i in 0 until mainAdapter.count) {
                    val name = mainAdapter.getItem(i).name
                    if (mainAdapter.isSelected(name)) {
                        selected.remove(name)
                    } else {
                        selected[name] = java.lang.Boolean.TRUE
                    }
                }
                mainAdapter.notifyDataSetChanged()
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_permission, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("WrongConstant")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.save_perm) {
            save(false)
        } else if (id == R.id.add_perm) {
            PermissionDialog(this@ManifestActivity, mainAdapter.currentList) { list ->
                mainAdapter.currentList = list
                mainAdapter.selected.clear()
                mainAdapter.notifyDataSetChanged()
                isDataChange = true
            }.show()
        } else if (id == R.id.comment_perm) {
            item.isChecked = !item.isChecked
            setAnnotationPermission(item.isChecked)
        } else if (id == R.id.multu_select_perm) {
            multipleMode()
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isDataChange) {
            MaterialAlertDialogBuilder(this).apply {
                setTitle(getString(R.string.save_modified))
                setMessage(getString(R.string.save_modified_message))
                setPositiveButton(getString(R.string.perm_save)) { _: DialogInterface?, _: Int -> save(true) }
                setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> finish() }
            }.show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    @SuppressLint("WrongConstant")
    override fun onDataSetChanged() {
        if (mainAdapter.count == 0) {
            cardView.layoutTransition.apply {
                disableTransitionType(LayoutTransition.APPEARING)
                cardView.visibility = View.VISIBLE
                enableTransitionType(LayoutTransition.APPEARING)
            }
        } else {
            cardView.layoutTransition.apply {
                disableTransitionType(LayoutTransition.DISAPPEARING)
                cardView.visibility = View.GONE
                enableTransitionType(LayoutTransition.DISAPPEARING)
            }
        }
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        mainAdapter.isSelectMode = false
        mainAdapter.notifyDataSetChanged()
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View, i: Int, j: Long) {
        val item = mainAdapter.getItem(i)
        if (mainAdapter.isSelectMode) {
            mainAdapter.setSelected(item, (view as ManifestAdapter.ItemView).holder.invertSelection())
            mainAdapter.notifyDataSetChanged()
            return
        }
        MaterialAlertDialogBuilder(this).apply {
            setTitle(item.label)
            setMessage(item.describe)
            setPositiveButton(getString(R.string.delete)) { _: DialogInterface?, _: Int ->
                ManifestAdapter.remove(mainAdapter.currentList, item)
                mainAdapter.notifyDataSetChanged()
                isDataChange = true
            }
            setNegativeButton(android.R.string.cancel, null)
        }.show()
    }

    override fun onItemLongClick(adapterView: AdapterView<*>?, view: View, i: Int, j: Long): Boolean {
        if (mainAdapter.isSelectMode) {
            return false
        }
        multipleMode()
        val viewHolder = (view as ManifestAdapter.ItemView).holder
        val item = mainAdapter.getItem(i)
        viewHolder.setSelection(true)
        mainAdapter.setSelected(item, true)
        mainAdapter.notifyDataSetChanged()
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return true
    }

    private fun multipleMode() {
        actionMode = startSupportActionMode(this)?.apply {
            title = getString(R.string.multi_select)
            subtitle = null
        }
    }

    private fun save(z: Boolean) {
        val save = ManifestUtils.save(mainAdapter.currentList, DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile), isAnnotationPermission())

        writeToFile(manifestFile.path, save)
        isDataChange = false
        Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        if (z) {
            finish()
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onSelection(permission: Permission, z: Boolean) {
        actionMode?.subtitle = String.format("Add permission (%d selected items)", mainAdapter.selected.size)
    }
}