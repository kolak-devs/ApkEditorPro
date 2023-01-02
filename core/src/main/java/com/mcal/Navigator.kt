package com.mcal

import android.content.Intent
import com.mcal.presentation.base.BaseActivity

interface Navigator {

    fun BaseActivity<*, *>.navigateTo(uiAction: UiAction, onExtras: (Intent) -> Unit)
}
