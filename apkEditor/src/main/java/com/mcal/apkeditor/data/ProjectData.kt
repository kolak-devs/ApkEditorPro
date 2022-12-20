package com.mcal.apkeditor.data

import com.google.gson.annotations.SerializedName

data class ProjectData(
    @field:SerializedName("label") var label: String?,
    @field:SerializedName("package") var packageName: String?,
    @field:SerializedName("path") var path: String?
)