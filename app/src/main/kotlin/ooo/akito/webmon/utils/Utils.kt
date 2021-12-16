package ooo.akito.webmon.utils

import android.annotation.SuppressLint
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
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.data.repository.WebSiteEntryRepository
import ooo.akito.webmon.ui.home.MainActivity
import ooo.akito.webmon.utils.BackgroundCheckInterval.nameList
import ooo.akito.webmon.utils.BackgroundCheckInterval.valueList
import ooo.akito.webmon.utils.Constants.DEFAULT_INTERVAL_MIN
import ooo.akito.webmon.utils.Constants.IS_AUTO_START_SHOWN
import ooo.akito.webmon.utils.Constants.IS_SCHEDULED
import ooo.akito.webmon.utils.Constants.MONITORING_INTERVAL
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_DESCRIPTION
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_ID
import ooo.akito.webmon.utils.Constants.NOTIFICATION_CHANNEL_NAME
import ooo.akito.webmon.utils.Constants.NOTIFY_ONLY_SERVER_ISSUES
import ooo.akito.webmon.utils.Constants.TAG_WORK_MANAGER
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
import org.apache.hc.client5.http.classic.methods.HttpGet
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


object Utils {

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

  @SuppressLint("UnspecifiedImmutableFlag")
  fun showNotification(context: Context, title: String, message: String) {
    val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

  private fun startWorkManager(context: Context, isForce : Boolean = false) {
    val isScheduled: Boolean? = customPrefs[IS_SCHEDULED, false] /* TODO: Remove this option and check, since this is always forced, anyway.*/
    isScheduled?.let { scheduled ->
      if (!scheduled || isForce) {
        customPrefs[IS_SCHEDULED] = true
        WorkManagerScheduler.refreshPeriodicWork(context)
      }
    }
  }

  fun getMonitorInterval() : Long {
    return (customPrefs[MONITORING_INTERVAL, DEFAULT_INTERVAL_MIN] ?: DEFAULT_INTERVAL_MIN).toLong()
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
    if (isCustomRom() && !customPrefs.getBoolean(IS_AUTO_START_SHOWN, false)) {
      AlertDialog.Builder(context).apply {
        setTitle(context.getString(R.string.enable_auto_start))
        setMessage(context.getString(R.string.message_auto_start_reason))
        setPositiveButton(context.getString(R.string.ok)) { dialog, _ ->
          customPrefs[IS_AUTO_START_SHOWN] = true
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

  suspend fun retrieveIconUrlFetcher(): String? {
    return withContext(Dispatchers.IO) {
      iconUrlFetcherList.firstNotNullOfOrNull { url ->
        val urlFull = buildDefaultIconUrlFull(url)
        try {
          WebSiteEntryRepository.http.execute(HttpGet(urlFull)).use {
            val resp = it.entity.content.readBytes()
            val respCode = it.code
            if (respCode.toString().startsWith("20").not()) {
              return@firstNotNullOfOrNull null
            } else {
              return@firstNotNullOfOrNull url
            }
          }
        } catch (e: Exception) {
          return@firstNotNullOfOrNull null
        }
      }
    }
  }

  fun Context.safelyStartSyncWorker(force: Boolean = true) {
    /* Make sure SyncWorker is not run more than once, simultaneously. */
    val workManager = WorkManager.getInstance(this)
    workManager.cancelUniqueWork(TAG_WORK_MANAGER)
    workManager.cancelAllWorkByTag(TAG_WORK_MANAGER)
    startWorkManager(this, force)
  }

  fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  fun View.showSnackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
  }

  fun View.showSnackBarWithAction(message: String, actionMessage: String, action: View.OnClickListener) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).apply {
      setAction(msgGenericDismiss, action)
    }.show()
  }

  fun Context.showToastNotImplemented() {
    showToast(msgNotImplemented)
  }

  fun View.showSnackbarNotImplemented() {
    showSnackBar(msgNotImplemented)
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
    val isEnabledServerFailOnly = customPrefs.getBoolean(NOTIFY_ONLY_SERVER_ISSUES, false)
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

  fun PackageManager.packageIsInstalled(packageName: String): Boolean {
    return try {
      getPackageInfo(packageName, 0)
      Log.info("""Package "${packageName}" is installed.""")
      true
    } catch (e: Exception) {
      Log.warn("""Package "${packageName}" is NOT installed.""")
      false
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

  fun Collection<WebSiteEntry>.sortedByItemPosition(): List<WebSiteEntry> = this.sortedBy { it.itemPosition }

  private fun appIsVisible(): Boolean = ooo.akito.webmon.Webmon.AppVisibility.appIsVisible

  fun String.removeTrailingSlashes(): String = this.replace(Regex("""[/]*$"""), "")
  fun String.removeUrlProto(): String = this.replace(Regex("""^http[s]?://"""), "")
  fun String.addProtoHttp(): String = if (this.startsWith("http").not()) { "http://" + this } else { this }

  inline fun <T> tryOrNull(action: () -> T): T? {
    return try {
      action()
    } catch (ex: Exception) {
      null
    }
  }

  fun String?.asUri(): Uri? = tryOrNull { Uri.parse(this) }

  fun buildIconUrlFull(
    urlIcon: String, /* Where to get the icon from. */
    urlTarget: String, /* Which website's icon to retrieve. */
    urlIconFallback: String, /* Direct URL to fallback icon, in case no original icon was found. */
    iconFormats: String, /* Allowed icon formats. */
    iconSizes: String /* Allowed icon sizes. */
  ): String {
    return "${urlIcon}/icon?url=${urlTarget}&formats=${iconFormats}&size=${iconSizes}&fallback_icon_url=${urlIconFallback}"
  }

  private fun buildDefaultIconUrlFull(urlIcon: String /* Where to get the icon from. */): String {
    /* TODO: Fix dangling Strings. */
    return buildIconUrlFull(
      urlIcon,
      defaultUrlNimHomepage,
      "https://www.zemarch.com/wp-content/uploads/2017/11/cropped-favicon.png",
      "gif,ico,jpg,png,svg",
      "16..64..128"
    )
  }

  fun InputStream.readAllAsString(): String {
    /**
      Read the whole inputStream and return its proper String representation.
    */
    return readBytes().toString(Charset.defaultCharset())
  }

  fun Context.openInBrowser(uri: Uri) = ContextCompat.startActivity(this, Intent(Intent.ACTION_VIEW, uri), null)
}