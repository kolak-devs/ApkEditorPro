package com.mcal.sqliteutil

import android.annotation.SuppressLint
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.appdm.base.databinding.SqlDialogTablerecordBinding
import java.util.*

class TableRecordDialog(
    activity: SqliteRowViewActivity,
    typeList: List<String>, nameList: List<String>,
    pkFlagList: List<String>, valueList: List<String>, index: Int,
    editable: Boolean
) {
    private val mEditable = editable
    private val mActivity = activity
    private var mIndex = index
    private var mDbFilePath: String? = null
    private var mTableName: String? = null
    private val mTypeList = typeList
    private val mNameList = nameList
    private val mValueList = valueList
    private val mPkFlagList = pkFlagList

    private var materialDialog: AlertDialog
    private var binding: SqlDialogTablerecordBinding

    init {
        binding = SqlDialogTablerecordBinding.inflate(activity.layoutInflater)
        val tipTv = binding.tvNoteditable
        if (!mEditable) {
            tipTv.visibility = View.VISIBLE
            binding.etValuey.isEnabled = false
        } else {
            tipTv.visibility = View.INVISIBLE
        }
        showItemByIndex(mIndex)

        materialDialog = MaterialAlertDialogBuilder(activity).apply {
            setView(binding.root)
            setPositiveButton("Save") { v, _ ->
                saveValue()
                v.dismiss()
            }
            setNegativeButton(android.R.string.cancel, null)
        }.create()
        if (!mEditable) {
            materialDialog.setOnShowListener {
                materialDialog.getButton(BUTTON_NEGATIVE).setOnClickListener {
                    materialDialog.dismiss()
                }
                materialDialog.getButton(BUTTON_NEGATIVE).visibility = View.GONE
            }
        }
        materialDialog.show()
    }

    // Save values to DB
    @SuppressLint("DefaultLocale")
    private fun saveValue() {
        val activity = mActivity
        try {
            if (isPrimaryKey(mIndex)) {
                throw Exception("Can not edit primary key!")
            }
            var valueType = mTypeList[mIndex]
            valueType = valueType.uppercase(Locale.getDefault())
            val strValue = binding.etValuey.text.toString()
            val newValue = when {
                SqliteTableViewActivity.isStringType(valueType) -> {
                    strValue
                }
                SqliteTableViewActivity.isIntType(valueType) -> {
                    java.lang.Long.valueOf(strValue)
                }
                SqliteTableViewActivity.isBoolType(valueType) -> {
                    java.lang.Boolean.valueOf(strValue)
                }
                SqliteTableViewActivity.isFloatType(valueType) -> {
                    java.lang.Float.valueOf(strValue)
                }
                SqliteTableViewActivity.isDoubleType(valueType) -> {
                    java.lang.Double.valueOf(strValue)
                }
                SqliteTableViewActivity.isBlobType(valueType) -> {
                    throw Exception("Value type not supported!")
                }
                else -> {
                    strValue
                }
            }
            activity.saveValue(mIndex, newValue)
            Toast.makeText(activity, "Succeed!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPrimaryKey(idx: Int): Boolean {
        return "0" != mPkFlagList[idx]
    }

    @SuppressLint("SetTextI18n")
    private fun showItemByIndex(idx: Int) {
        val isPK = isPrimaryKey(idx)
        binding.tvType.text = "Type: " + mTypeList[idx]
        binding.tvPkflag.text = "Primary Key: $isPK"
        binding.etName.setText(mNameList[idx])
        binding.etValuey.setText(mValueList[idx])
        if (!mEditable) {
            if (isPK) {
                materialDialog.setOnShowListener {
                    materialDialog.getButton(BUTTON_POSITIVE).visibility = View.GONE
                }
                materialDialog.show()
            } else {
                materialDialog.setOnShowListener {
                    materialDialog.getButton(BUTTON_POSITIVE).visibility = View.VISIBLE
                }
                materialDialog.show()
            }
        }
    }

    private fun showPrevItem() {
        if (mIndex > 0) {
            showItemByIndex(mIndex - 1)
            mIndex -= 1
        } else {
            Toast.makeText(mActivity, "No more values!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNextItem() {
        if (mIndex + 1 < mValueList.size) {
            showItemByIndex(mIndex + 1)
            mIndex += 1
        } else {
            Toast.makeText(mActivity, "No more values!", Toast.LENGTH_SHORT).show()
        }
    }

    fun setTableInfo(dbFilePath: String?, tableName: String?) {
        mDbFilePath = dbFilePath
        mTableName = tableName
    }
}