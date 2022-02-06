package ooo.akito.webmon.utils

import android.util.Log
import ooo.akito.webmon.utils.Constants.TAG_GLOBAL


object Log {
  fun error(msg : String) {
    Log.e(TAG_GLOBAL, msg)
  }

  fun warn(msg : String) {
    Log.w(TAG_GLOBAL, msg)
  }

  fun info(msg : String) {
    Log.i(TAG_GLOBAL, msg)
  }

  fun debug(msg : String) {
    Log.d(TAG_GLOBAL, msg)
  }
}