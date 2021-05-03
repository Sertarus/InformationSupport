package com.application.informationsupport.models

import android.graphics.Bitmap

data class ModelChat (var message: String? = null, var sender: String = "",
                      var timeStamp: String = "",  var image: Bitmap? = null, var id: Int = 0)