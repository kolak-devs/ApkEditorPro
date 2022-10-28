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
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ActivityHelper
import ru.svolf.melissa.swipeback.SwipeBackActivity

fun Activity.startFullEditActivity(filePath: String?): Boolean {
    val inflater = this.getSystemService(SwipeBackActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    @SuppressLint("InflateParams") val view: View = inflater.inflate(R.layout.dialog_decode_mode, null)
    val assets: CheckBox = view.findViewById(R.id.decode_assets)
    assets.isChecked = Preferences.isNeedDecodeAssets()
    val resources: CheckBox = view.findViewById(R.id.decode_resources)
    resources.isChecked = Preferences.isNeedDecodeResources()
    val classes: CheckBox = view.findViewById(R.id.decode_classes)
    classes.isChecked = Preferences.isNeedDecodeClasses()
    val dialog = MaterialAlertDialogBuilder(this)
    dialog.setTitle("Режим декодирования")
    dialog.setView(view)
    dialog.setPositiveButton(android.R.string.ok) { _, _ ->
        Preferences.setDecodeAssets(assets.isChecked)
        Preferences.setDecodeResources(resources.isChecked)
        Preferences.setDecodeClasses(classes.isChecked)
        val intent = Intent(this, ApkInfoExActivity::class.java)
        ActivityHelper.attachParam(intent, "apkPath", filePath)
        val fullDecoding = true
        ActivityHelper.attachBoolParam(intent, "isFullDecoding", fullDecoding)
        this.startActivity(intent)
    }
    dialog.create().show()
    return true
}