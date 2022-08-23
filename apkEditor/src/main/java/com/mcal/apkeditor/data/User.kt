package com.mcal.apkeditor.data

import java.io.Serializable

data class User(val id: Int, val email:String, val token: String, val vip: Boolean) : Serializable