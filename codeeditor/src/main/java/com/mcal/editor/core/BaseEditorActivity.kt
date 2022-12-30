package com.mcal.editor.core

import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.mcal.common.activities.CustomizedLangActivity

typealias Inflate<T> = (LayoutInflater) -> T

abstract class BaseEditorActivity<VB : ViewBinding>(
    bindingInflater: Inflate<VB>
) : CustomizedLangActivity() {

    protected val binding = bindingInflater.invoke(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
