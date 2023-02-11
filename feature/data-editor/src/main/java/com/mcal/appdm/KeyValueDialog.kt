package com.mcal.appdm

import android.annotation.SuppressLint
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.appdm.base.databinding.AppdmDialogKeyvalueBinding

class KeyValueDialog(
    private val activity: PrefDetailActivity,
    values: LinkedHashMap<String?, Any?>?,
    private var index: Int,
    private val editable: Boolean
) {
    private val valueList: MutableList<ValueRecord> = ArrayList()
    private var valueChanged = false

    private lateinit var materialDialog: AlertDialog
    private lateinit var binding: AppdmDialogKeyvalueBinding

    init {
        initData(values)
        initView()
    }

    @SuppressLint("InflateParams")
    private fun initView() {
        binding = AppdmDialogKeyvalueBinding.inflate(activity.layoutInflater)
        if (!editable) {
            binding.etValuey.isEnabled = false
            binding.tvNoteditable.visibility = View.VISIBLE
        } else {
            binding.tvNoteditable.visibility = View.GONE
        }
        showItemByIndex(index)

        materialDialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setPositiveButton("Save") { v, _ ->
                saveValue()
                valueChanged = true
                v.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { v, _ ->
                if (valueChanged) {
                    activity.refresh()
                }
                v.dismiss()
            }
            .create()
        materialDialog.show()
    }

    // Save values to xml
    private fun saveValue() {
        try {
            val record = valueList[index]
            val valueType = record.valueType
            val strValue = binding.etValuey.text.toString()
            val newValue = when (valueType) {
                "Integer" -> {
                    Integer.valueOf(strValue)
                }
                "Float" -> {
                    java.lang.Float.valueOf(strValue)
                }
                "Long" -> {
                    java.lang.Long.valueOf(strValue)
                }
                "String" -> {
                    strValue
                }
                "Boolean" -> {
                    java.lang.Boolean.valueOf(strValue)
                }
                else -> {
                    throw Exception("Value type not supported!")
                }
            }
            record.value = newValue.toString()
            activity.saveValue(record.key, newValue)
            Toast.makeText(activity, "Succeed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val msg = e.message
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showItemByIndex(idx: Int) {
        val rec = valueList[idx]
        binding.tvType.text = "Type: ${rec.valueType}"
        binding.etKey.setText(rec.key)
        binding.etValuey.setText(rec.value)
    }

    private fun showPrevItem() {
        if (index > 0) {
            showItemByIndex(index - 1)
            index -= 1
        } else {
            Toast.makeText(activity, "No more values!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNextItem() {
        if (index + 1 < valueList.size) {
            showItemByIndex(index + 1)
            index += 1
        } else {
            Toast.makeText(activity, "No more values!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initData(values: Map<String?, Any?>?) {
        values?.let {
            for (key in it.keys) {
                val rec = ValueRecord()
                rec.key = key
                val value = it[key]
                if (value != null) {
                    rec.value = value.toString()
                    rec.valueType = value.javaClass.simpleName
                } else {
                    rec.value = ""
                    rec.valueType = "null"
                }
                valueList.add(rec)
            }
        }
    }

    class ValueRecord {
        var key: String? = null
        var value: String? = null
        var valueType: String? = null
    }
}