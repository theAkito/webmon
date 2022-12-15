package ooo.akito.webmon.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object WebsiteCreationMemoriser {

  const val name = "NAME"
  const val url = "URL"
  const val isDNSChecked = "DNSCheck"
  const val isLaissezFaireChecked = "LaissezCheck"
  const val isOnionChecked = "OnionCheck"
  const val isTcpChecked = "TcpCheck"
  const val isSmtpChecked = "SmtpCheck"
  const val isImapChecked = "ImapCheck"
  const val entryTags = "EntryTags"

  fun defaultPreference(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

  fun remember(context: Context, name: String): SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

  fun SharedPreferences.getSaveEntryTags(): List<String> = mapperUgly.readTree(saveEntryTags).asIterable().mapNotNull { it.asText() }.distinct()
  fun SharedPreferences.setSaveEntryTags(tags: List<String>) { saveEntryTags = mapperUgly.writeValueAsString(tags) }

  private inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
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

  var SharedPreferences.saveIsTcpChecked
    get() = getBoolean(isTcpChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isTcpChecked, value)
      }
    }

  var SharedPreferences.saveIsSmtpChecked
    get() = getBoolean(isSmtpChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isSmtpChecked, value)
      }
    }

  var SharedPreferences.saveIsImapChecked
    get() = getBoolean(isImapChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isImapChecked, value)
      }
    }

  var SharedPreferences.saveURL
    get() = getString(url, "")
    set(value) {
      editMe {
        it.putString(url, value)
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
    get() = getString(name, "")
    set(value) {
      editMe {
        it.putString(name, value)
      }
    }

  var SharedPreferences.saveIsLaissezFaireChecked
    get() = getBoolean(isLaissezFaireChecked, false)
    set(value) {
      editMe {
        it.putBoolean(isLaissezFaireChecked, value)
      }
    }

  private var SharedPreferences.saveEntryTags
    get() = getString(entryTags, "")
    set(value) {
      editMe {
        it.putString(entryTags, value)
      }
    }
}