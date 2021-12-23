package ooo.akito.webmon.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object PreferenceHelper {

  val NAME = "NAME"
  val URL = "URL"
  val isDNSChecked = "DNSCheck"
  val isLaissezFaireChecked = "LaissezCheck"
  val isOnionChecked = "OnionCheck"

  fun defaultPreference(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

  fun customPreference(context: Context, name: String): SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

  inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
    val editMe = edit()
    operation(editMe)
    editMe.apply()
  }

  var SharedPreferences.saveIsOnionChecked
    get() = getBoolean(isOnionChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isOnionChecked, value)
      }
    }

  var SharedPreferences.saveURL
    get() = getString(URL, "")
    set(value) {
      editMe {
        it.putString(URL, value)
      }
    }

  var SharedPreferences.saveIsDNSChecked
    get() = getBoolean(isDNSChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isDNSChecked, value)
      }
    }

  var SharedPreferences.saveName
    get() = getString(NAME, "")
    set(value) {
      editMe {
        it.putString(NAME, value)
      }
    }

  var SharedPreferences.saveIsLaissezFaireChecked
    get() = getBoolean(isLaissezFaireChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isLaissezFaireChecked, value)
      }
    }
}