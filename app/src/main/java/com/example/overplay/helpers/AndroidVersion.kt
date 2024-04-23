package com.example.overplay.helpers

import android.os.Build

object AndroidVersion {
    val isAndroidS: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

}
