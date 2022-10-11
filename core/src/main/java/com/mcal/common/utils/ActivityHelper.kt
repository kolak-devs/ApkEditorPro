package com.mcal.common.utils

import android.content.Intent
import android.os.Bundle

object ActivityHelper {
    @JvmStatic
    fun attachParam(intent: Intent, key: String?, value: String?): Bundle {
        val bundle = Bundle()
        bundle.putString(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachParam(intent: Intent, key: String?, value: IntArray?): Bundle {
        val bundle = Bundle()
        bundle.putIntArray(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachParam(intent: Intent, key: String?, value: Boolean): Bundle {
        val bundle = Bundle()
        bundle.putBoolean(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachParam(intent: Intent, key: String?, value: ArrayList<String?>?): Bundle {
        val bundle = Bundle()
        bundle.putStringArrayList(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachParam2(intent: Intent, key: String?, value: ArrayList<Int?>?): Bundle {
        val bundle = Bundle()
        bundle.putIntegerArrayList(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachParam(intent: Intent, key: String?, value: Int): Bundle {
        val bundle = Bundle()
        bundle.putInt(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun attachBoolParam(intent: Intent, key: String?, value: Boolean): Bundle {
        val bundle = Bundle()
        bundle.putBoolean(key, value)
        intent.putExtras(bundle)
        return bundle
    }

    @JvmStatic
    fun getParam(intent: Intent, key: String?): String? {
        val bundle = intent.extras
        return bundle?.getString(key)
    }

    @JvmStatic
    fun getBoolParam(intent: Intent, key: String?): Boolean {
        val bundle = intent.extras
        return bundle?.getBoolean(key, false) ?: false
    }

    @JvmStatic
    fun getIntParam(intent: Intent, key: String?): Int {
        val bundle = intent.extras
        return bundle?.getInt(key, 0) ?: 0
    }

    @JvmStatic
    fun getStringArray(intent: Intent, key: String?): ArrayList<String>? {
        val bundle = intent.extras
        return bundle?.getStringArrayList(key)
    }

    @JvmStatic
    fun getIntArray(intent: Intent, key: String?): ArrayList<Int>? {
        val bundle = intent.extras
        return bundle?.getIntegerArrayList(key)
    }

    @JvmStatic
    fun attachParam(
        intent: Intent, key: String,
        mapValue: MutableMap<String, String?>
    ) {
        val bundle = Bundle()
        val keyList = ArrayList<String>()
        val valueList = ArrayList<String?>()
        for (strKey in mapValue.keys) {
            val strVal = mapValue[strKey]
            keyList.add(strKey)
            valueList.add(strVal)
        }
        bundle.putStringArrayList(key + "_keys", keyList)
        bundle.putStringArrayList(key + "_values", valueList)
        intent.putExtras(bundle)
    }

    @JvmStatic
    fun getMapParam(intent: Intent, key: String): MutableMap<String, String>? {
        val bundle = intent.extras
        if (bundle != null) {
            val mapVal: MutableMap<String, String> = HashMap()
            val strKeys = bundle.getStringArrayList(key + "_keys")
            val strValues = bundle.getStringArrayList(key + "_values")
            if (strKeys != null && strValues != null) {
                for (i in strKeys.indices) {
                    mapVal[strKeys[i]] = strValues[i]
                }
                return mapVal
            }
        }
        return null
    }
}