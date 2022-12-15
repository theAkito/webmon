package ooo.akito.webmon.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import ooo.akito.webmon.utils.Utils.tryOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


object Environment {

  val manufacturer = android.os.Build.MANUFACTURER.lowercase(Locale.ROOT)

  lateinit var locale: Locale
  lateinit var defaultTimeFormat: SimpleDateFormat

  /* Using Date, because ZonedDateTime and LocalDateTime require at least Android O. */
  fun Context.getCurrentLocale(): Locale = ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.ENGLISH
  fun Locale.getDefaultDateTimeFormat(): SimpleDateFormat = SimpleDateFormat("""yyyy-MM-dd'T'HH-mm-ss""", this)
  fun getDefaultDateTimeString(): String = defaultTimeFormat.format(Date())
  fun Context.getDefaultPathCacheBackup(): File = File(cacheDir, "backups").apply { tryOrNull { mkdir() } }
  fun Context.getDefaultPathCacheLog(): File = File(cacheDir, "logs").apply { tryOrNull { mkdir() } }
  fun getNameFileLog() = "logcat-webmon_${getDefaultDateTimeString()}.log"
  fun getNameFileBackup(backupType: String) = "backup-webmon-${backupType}_${getDefaultDateTimeString()}.json"
}