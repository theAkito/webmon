package ooo.akito.webmon.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.text.SimpleDateFormat
import java.util.*

object Environment {

  lateinit var locale: Locale
  lateinit var defaultTimeFormat: SimpleDateFormat

  /* Using Date, because ZonedDateTime and LocalDateTime require at least Android O. */
  fun Context.getCurrentLocale(): Locale = ConfigurationCompat.getLocales(resources.configuration).get(0)
  fun Locale.getDefaultDateTimeFormat(): SimpleDateFormat = SimpleDateFormat("""yyyy-MM-dd'T'HH-mm-ss""", this)
  fun getDefaultDateTimeString(): String = defaultTimeFormat.format(Date())
}