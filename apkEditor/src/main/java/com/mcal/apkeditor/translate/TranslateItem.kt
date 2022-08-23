package com.mcal.apkeditor.translate

import java.io.Serializable

class TranslateItem : Serializable {
    @JvmField
    var name: String

    @JvmField
    var originValue: String

    @JvmField
    var translatedValue: String? = null

    constructor(_n: String, _o: String) {
        name = _n
        originValue = _o
    }

    constructor(_n: String, _o: String, _t: String?) {
        name = _n
        originValue = _o
        translatedValue = _t
    }

    companion object {
        private const val serialVersionUID = -3101805950698159689L
    }
}