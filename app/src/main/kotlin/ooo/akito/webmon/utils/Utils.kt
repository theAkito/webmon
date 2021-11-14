package ooo.akito.webmon.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.WorkManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.snackbar.Snackbar
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.ui.home.MainActivity
import ooo.akito.webmon.utils.BackgroundCheckInterval.nameList
import ooo.akito.webmon.utils.BackgroundCheckInterval.valueList
import ooo.akito.webmon.utils.Constants.DEFAULT_INTERVAL_MIN
import ooo.akito.webmon.utils.Constants.IS_SCHEDULED
import ooo.akito.webmon.utils.Constants.MONITORING_INTERVAL
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_DESCRIPTION
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_ID
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_NAME
import ooo.akito.webmon.utils.Constants.WEBSITE_ENTRY_TAG_CLOUD_DATA
import ooo.akito.webmon.utils.Environment.manufacturer
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeNXDOMAIN
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeTorAppUnavailable
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeTorConnFailed
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeTorFail
import ooo.akito.webmon.utils.ExceptionCompanion.msgCannotConnectToTor
import ooo.akito.webmon.utils.ExceptionCompanion.msgGenericTorFailure
import ooo.akito.webmon.utils.ExceptionCompanion.msgMiniNXDOMAIN
import ooo.akito.webmon.utils.ExceptionCompanion.msgNotImplemented
import ooo.akito.webmon.utils.ExceptionCompanion.msgTorIsNotInstalled
import ooo.akito.webmon.utils.SharedPrefsManager.customPrefs
import ooo.akito.webmon.utils.SharedPrefsManager.get
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.worker.WorkManagerScheduler
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


object Utils {

  val lineEnd: String = System.lineSeparator()
  var totalAmountEntry = 0
  var torIsEnabled = false
  var torAppIsAvailable = false
  var swipeRefreshIsEnabled = true
  var isEntryCreated = false /** Do not observe "unavailable" Website, just because it is freshly added and seems "unavailable", when it isn't. */

  var globalEntryTagsNames: List<String> = listOf(msgGenericDefault)
    set(value) {
      val readyValue = value.distinct().sorted()
      field = readyValue
      customPrefs[WEBSITE_ENTRY_TAG_CLOUD_DATA] = mapperUgly.writeValueAsString(readyValue)
    }

  val mapper: ObjectMapper = jacksonObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT) /* Always pretty-print. */
  val mapperUgly: ObjectMapper = jacksonObjectMapper()
    .disable(SerializationFeature.INDENT_OUTPUT) /* Always pretty-print. */

  fun triggerRebirth(context: Context) {
    /** https://stackoverflow.com/a/46848226/7061105 */
    val packageManager: PackageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent == null) {
      Log.error("Cannot restart App, because intent is null!")
      return
    }
    val componentName = intent.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
  }

  fun Context.showKeyboard(view: View) {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
      showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
  }

  fun currentDateTime(): String {
    return SimpleDateFormat("dd-MMM-yyyy hh:mm:ss:a", Locale.ENGLISH).format(Date())
  }

  fun showNotification(context: Context, title: String, message: String) {
    val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH)
      channel.description = NOTIFICATION_CHANNEL_DESCRIPTION
      mNotificationManager.createNotificationChannel(channel)
    }
    val mBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_alert) // notification icon
      .setContentTitle(title) // title for notification
      .setContentText(message)// message for notification
      .setDefaults(Notification.DEFAULT_SOUND)
      .setAutoCancel(true) // clear notification after click

    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    /** https://stackoverflow.com/a/67046334/7061105 */
    val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
    } else {
      PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
    }
    mBuilder.setContentIntent(pi)
    mNotificationManager.notify(Random().nextInt(), mBuilder.build())
  }

  fun isValidUrl(url: String) : Boolean{
    try {
      URL(url).toURI()
      return true
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return false
  }

  fun startWorkManager(context: Context, isForce : Boolean = false) {
    val isScheduled: Boolean? = SharedPrefsManager.customPrefs[IS_SCHEDULED, false]
    isScheduled?.let { scheduled ->
      if (!scheduled || isForce) {
        SharedPrefsManager.customPrefs[IS_SCHEDULED] = true
        WorkManagerScheduler.refreshPeriodicWork(context)
      }
    }
  }

  fun getMonitorInterval() : Long {
    return (SharedPrefsManager.customPrefs[MONITORING_INTERVAL, DEFAULT_INTERVAL_MIN] ?: DEFAULT_INTERVAL_MIN).toLong()
  }

  fun getMonitorTime() : String {
    val interval = getMonitorInterval().toInt()
    var refreshTime: String? = null
    if (valueList.contains(interval)) {
      val pos = valueList.indexOf(interval)
      if (pos >= 0 && pos < nameList.size) {
        refreshTime = nameList[pos]
      }
    }
    return "Checking ${refreshTime?.replaceFirst('E', 'e') ?: "every hour"}."
  }

  fun isCustomRom(): Boolean = listOf("xiaomi", "oppo", "vivo").contains(manufacturer)

  fun openAutoStartScreen(context: Context) {
    /* Currently not in use. */
    val intent = Intent()
    when(manufacturer) {
      "xiaomi" -> intent.component= ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
      "oppo" -> intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
      "vivo" -> intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
    }
    val list = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    if (list.size > 0) { context.startActivity(intent) }
  }

  fun showAutoStartEnableDialog(context: Context) {
    /* Currently not in use. */
    if (isCustomRom() && !SharedPrefsManager.customPrefs.getBoolean(Constants.IS_AUTO_START_SHOWN, false)) {
      AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.enable_auto_start))
        setMessage(context.getString(R.string.message_auto_start_reason))
        setPositiveButton(context.getString(R.string.ok)) { dialog, _ ->
          SharedPrefsManager.customPrefs[Constants.IS_AUTO_START_SHOWN] = true
          openAutoStartScreen(context)
          dialog.dismiss()
        }
        setNegativeButton(context.getString(R.string.cancel), null)
        setCancelable(false)
      }.create().show()
    }
  }

  fun openUrl(context: Context, url: String) {
    try {
      val intents = Intent(Intent.ACTION_VIEW)
      Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intents)
    } catch (e: Exception) {
      Log.error(e.toString())
      Toast.makeText(context, context.getString(R.string.no_apps_found), Toast.LENGTH_LONG).show()
    }
  }

  fun Context.safelyStartSyncWorker(force: Boolean = false) {
    /* Make sure SyncWorker is not run more than once, simultaneously. */
    val workManager = WorkManager.getInstance(this)
    workManager.cancelUniqueWork(Constants.TAG_WORK_MANAGER)
    workManager.cancelAllWorkByTag(Constants.TAG_WORK_MANAGER)
    startWorkManager(this, force)
  }

  fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
  }

  fun showSnackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
  }

  fun Context.showToastNotImplemented() {
    showToast(this, msgNotImplemented)
  }

  fun View.showSnackbarNotImplemented() {
    showSnackBar(this, msgNotImplemented)
  }

  fun Int?.isStatusAcceptable(): Boolean {
    /**
      The main focus of the entire app is checking if a Website is reachable.
      It cannot check whether the Website is configured properly, or anything like that.
      If the response code is barely acceptable, we are fine with it,
      as long as the website still seems available.
      Example:
        A "Forbidden" website is fully available and functioning.
        It's just not available to you. It's personal.
    */
    return when (this) {
      HttpURLConnection.HTTP_OK,
      HttpURLConnection.HTTP_CREATED,
      HttpURLConnection.HTTP_ACCEPTED,
      HttpURLConnection.HTTP_UNAUTHORIZED,
      HttpURLConnection.HTTP_NO_CONTENT,
      HttpURLConnection.HTTP_FORBIDDEN -> true
      else -> false
    }
  }

  fun WebSiteEntry.isStatusAcceptable(): Boolean {
    val code = this.status
    return if (this.isLaissezFaire) {
      code.isStatusAcceptable()
    } else {
      code == HttpURLConnection.HTTP_OK
    }
  }

  private fun WebSiteEntry.chooseStatusMsg(actualStatusMsg: String): String {
    return when (this.isLaissezFaire) {
      true -> msgGenericAvailable
      false -> actualStatusMsg
    }
  }

  fun getStatusMessage(website: WebSiteEntry): String {
    return when (website.status) {
      HttpURLConnection.HTTP_OK -> msgGenericSuccess // 200
      HttpURLConnection.HTTP_CREATED -> website.chooseStatusMsg("Created") /* "Created" // 201 */
      HttpURLConnection.HTTP_ACCEPTED -> website.chooseStatusMsg("Accepted") /* "Accepted" // 202 */
      HttpURLConnection.HTTP_NOT_AUTHORITATIVE -> "Non-Authoritative Information" // 203
      HttpURLConnection.HTTP_NO_CONTENT -> website.chooseStatusMsg("No Content") /* "No Content" // 204 */
      HttpURLConnection.HTTP_RESET -> "Reset Content" // 205
      HttpURLConnection.HTTP_PARTIAL -> "Partial Content" // 206
      HttpURLConnection.HTTP_MULT_CHOICE -> "Multiple Choices" // 300
      HttpURLConnection.HTTP_MOVED_PERM -> "Moved Permanently" // 301
      HttpURLConnection.HTTP_MOVED_TEMP -> "Temporary Redirect" // 302
      HttpURLConnection.HTTP_SEE_OTHER -> "See Other" // 303
      HttpURLConnection.HTTP_NOT_MODIFIED -> "Not Modified" // 304
      HttpURLConnection.HTTP_USE_PROXY -> "Use Proxy" // 305
      HttpURLConnection.HTTP_BAD_REQUEST -> "Bad Request" // 400
      HttpURLConnection.HTTP_UNAUTHORIZED -> website.chooseStatusMsg("Unauthorized") /* "Unauthorized" // 401 */
      HttpURLConnection.HTTP_PAYMENT_REQUIRED -> "Payment Required" // 402
      HttpURLConnection.HTTP_FORBIDDEN -> website.chooseStatusMsg("Forbidden") /* "Forbidden" // 403 */
      HttpURLConnection.HTTP_NOT_FOUND -> "Not Found" // 404
      HttpURLConnection.HTTP_BAD_METHOD -> "Method Not Allowed" // 405
      HttpURLConnection.HTTP_NOT_ACCEPTABLE -> "Not Acceptable" // 406
      HttpURLConnection.HTTP_PROXY_AUTH -> "Proxy Authentication Required" // 407
      HttpURLConnection.HTTP_CLIENT_TIMEOUT -> "Request Time-Out" // 408
      HttpURLConnection.HTTP_CONFLICT -> "Conflict" // 409
      HttpURLConnection.HTTP_GONE -> "Gone" // 410
      HttpURLConnection.HTTP_LENGTH_REQUIRED -> "Length Required" // 411
      HttpURLConnection.HTTP_PRECON_FAILED -> "Precondition Failed" // 412
      HttpURLConnection.HTTP_ENTITY_TOO_LARGE -> "Request Entity Too Large" // 413
      HttpURLConnection.HTTP_REQ_TOO_LONG -> "Request-URI Too Large" // 414
      HttpURLConnection.HTTP_UNSUPPORTED_TYPE -> "Unsupported Media Type" // 415
      HttpURLConnection.HTTP_INTERNAL_ERROR -> "Internal Server Error" // 500
      HttpURLConnection.HTTP_NOT_IMPLEMENTED -> "Not Implemented" // 501
      HttpURLConnection.HTTP_BAD_GATEWAY -> "Bad Gateway" // 502
      HttpURLConnection.HTTP_UNAVAILABLE -> "Service Unavailable" // 503
      HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> "Gateway Timeout" // 504
      HttpURLConnection.HTTP_VERSION -> "HTTP Version Not Supported" // 505
      connCodeTorFail -> msgGenericTorFailure
      connCodeTorAppUnavailable -> msgTorIsNotInstalled
      connCodeTorConnFailed -> msgCannotConnectToTor
      connCodeNXDOMAIN  -> msgMiniNXDOMAIN
      else -> "Unknown"
    }
  }

  private fun isServerRelatedFail(status: Int): Boolean {
    return status >= 500
  }

  fun mayNotifyStatusFailure(website: WebSiteEntry): Boolean {
    val status = website.status
    val isEnabledServerFailOnly = SharedPrefsManager
      .customPrefs.getBoolean(Constants.NOTIFY_ONLY_SERVER_ISSUES, false)
    val statusIsNotAcceptable = website.isStatusAcceptable().not()
    return if (isEnabledServerFailOnly) {
      statusIsNotAcceptable && isServerRelatedFail(status ?: 0)
    } else {
      statusIsNotAcceptable
    }
  }

  fun Boolean.doIfAppIsVisible(action: () -> Unit) {
    /* We only want to send notifications, when App is in Background. */
    return if (appIsVisible() == this) {
      action()
    } else {
      Log.info("Skipping job iteration, because app is visible.")
    }
  }

  fun Context.getStringNotWorking(url: String): String {
    return String.format(
      this.getString(
        R.string.not_working,
        url
      )
    )
  }

  fun List<WebSiteStatus>.joinToStringDescription(): String {
    return this.joinToString(lineEnd) { status ->
      status.name
    }
  }

  fun List<WebSiteEntry>.associateByUrl(): Map<String, WebSiteEntry> {
    return this.associateBy { it.url }
  }

  fun List<WebSiteEntry>.cleanCustomTags(): List<WebSiteEntry> = this.map { it.customTags = listOf(); it }

  fun appIsVisible(): Boolean = ooo.akito.webmon.Webmon.ActivityVisibility.appIsVisible

  fun String.removeTrailingSlashes(): String = this.replace(Regex("""[/]*$"""), "")
  fun String.removeUrlProto(): String = this.replace(Regex("""^http[s]?://"""), "")
  fun String.addProtoHttp(): String = if (this.startsWith("http").not()) { "http://" + this } else { this }

  fun String?.asUri(): Uri? {
    return try {
      Uri.parse(this)
    } catch (e: Exception) {
      null
    }
  }

  fun Context.openInBrowser(uri: Uri) = ContextCompat.startActivity(this, Intent(Intent.ACTION_VIEW, uri), null)
}