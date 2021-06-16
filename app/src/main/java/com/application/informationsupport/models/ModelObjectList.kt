package com.application.informationsupport.models

import android.graphics.Bitmap

data class ModelObjectList(
    var name: String = "", var creator: String = "", var date: String = "",
    var isFolder: Boolean = false, var image: Bitmap?
)