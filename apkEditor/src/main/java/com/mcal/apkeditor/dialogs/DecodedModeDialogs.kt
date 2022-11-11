package com.mcal.apkeditor.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.ApkInfoExActivity
import com.mcal.common.data.LegacyPreferences
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ActivityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.svolf.melissa.swipeback.SwipeBackActivity
fun Activity.startFullEditActivity(filePath: String?): Boolean {
    val inflater = this.getSystemService(SwipeBackActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    @SuppressLint("InflateParams") val view: View = inflater.inflate(R.layout.dialog_decode_mode, null)
    val assets: CheckBox = view.findViewById(R.id.decode_assets)
    val resources: CheckBox = view.findViewById(R.id.decode_resources)
    val classes: CheckBox = view.findViewById(R.id.decode_classes)
    CoroutineScope(Dispatchers.Main).launch {
        assets.isChecked = ReactivePreferences.isNeedDecodeAssets()
        resources.isChecked = ReactivePreferences.isNeedDecodeResources()
        classes.isChecked = ReactivePreferences.isNeedDecodeClasses()
    }
    val dialog = MaterialAlertDialogBuilder(this)
    dialog.setTitle("Режим декодирования")
    dialog.setView(view)
    dialog.setPositiveButton(android.R.string.ok) { _, _ ->
        CoroutineScope(Dispatchers.Main).launch {
            ReactivePreferences.setDecodeAssets(assets.isChecked)
            ReactivePreferences.setDecodeResources(resources.isChecked)
            ReactivePreferences.setDecodeClasses(classes.isChecked)
        }
        val intent = Intent(this, ApkInfoExActivity::class.java)
        ActivityHelper.attachParam(intent, "apkPath", filePath)
        val fullDecoding = true
        ActivityHelper.attachBoolParam(intent, "isFullDecoding", fullDecoding)
        this.startActivity(intent)
    }
    dialog.create().show()
    return true
}