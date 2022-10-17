package com.mcal.bshengine

import android.app.AlertDialog
import android.os.Bundle
import bsh.Interpreter
import com.mcal.bshengine.api.*
import com.mcal.common.activities.CustomizedLangActivity
import java.io.InputStreamReader

class BshEngineActivity : CustomizedLangActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bshengine_activity)
        setupToolbar(R.id.toolbar, "BSH Patcher", back = true)
        val decodedDir = intent.extras?.getString("filePath")
        try {
            decodedDir?.let {
                val i = Interpreter()
                i["ZActivity"] = this
                // API
                i["ZFileHelper"] = ZFileHelper(this)
                i["ZScopedStorage"] = ZScopedStorage(this, it)
                i["ZMatcher"] = ZMatcher(this)
                i["ZCipher"] = ZCipher(this)
                i["ZToast"] = ZToast(this)
                i["ZLogger"] = ZLogger(this)
                i["ZSignature"] = ZSignature(this, it)
                i.eval(InputStreamReader(assets.open("bin_patch.java")))
            }

        } catch (e: Exception) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Warning")
            dialog.setMessage(e.toString())
            dialog.show()
        }
    }
}