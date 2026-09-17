package com.mcal.colormixer

import com.mcal.neweditor.R
import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ColorMixerDialog(
    context: Context,
    initialColor: Int,
    onSet: ColorMixer.OnColorChangedListener
) {
    init {
        val mixer = ColorMixer(context)
        mixer.setProgressBarColor(initialColor)

        val materialDialog = MaterialAlertDialogBuilder(context)
            .setView(mixer)
            .create()

        materialDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.colormixer_set)
        ) { dialog: DialogInterface, _: Int ->
            if (initialColor != mixer.color) {
                onSet.onColorChange(mixer.color)
            }
            dialog.dismiss()
        }

        materialDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            context.getString(android.R.string.cancel)
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        materialDialog.show()
    }
}