package com.mcal.bshengine.api

interface XAlertClickListener {

    /**
     * Since: 2.4.7
     */
    fun onPositive()

    /**
     * Since: 2.4.7
     */
    fun onNegative()

    /**
     * Since: 2.4.7
     */
    fun onNeutral()
}