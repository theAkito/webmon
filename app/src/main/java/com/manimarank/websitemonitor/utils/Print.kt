package com.manimarank.websitemonitor.utils

import android.util.Log
import com.manimarank.websitemonitor.utils.Constants.TAG_GLOBAL

object Print {
    fun log(msg : String) {
        Log.e(TAG_GLOBAL, msg)
    }
}