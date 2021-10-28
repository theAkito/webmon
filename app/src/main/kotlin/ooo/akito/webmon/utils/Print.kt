package ooo.akito.webmon.utils

import android.util.Log
import ooo.akito.webmon.utils.Constants.TAG_GLOBAL

object Print {
    fun log(msg : String) {
        Log.e(TAG_GLOBAL, msg)
    }
}