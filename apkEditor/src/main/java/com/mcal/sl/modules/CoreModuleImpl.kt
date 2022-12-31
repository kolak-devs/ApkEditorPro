package com.mcal.sl.modules

import android.content.Context

class CoreModuleImpl: CoreModule {

    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
    }

    override fun getApplicationContext() = context
}
