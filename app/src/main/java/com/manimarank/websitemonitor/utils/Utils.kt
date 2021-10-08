package com.manimarank.websitemonitor.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.android.material.snackbar.Snackbar
import com.manimarank.websitemonitor.MyApplication
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.ui.home.MainActivity
import com.manimarank.websitemonitor.utils.Constants.DEFAULT_INTERVAL_MIN
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_DESCRIPTION
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.manimarank.websitemonitor.utils.Interval.nameList
import com.manimarank.websitemonitor.utils.Interval.valueList
import com.manimarank.websitemonitor.utils.SharedPrefsManager.get
import com.manimarank.websitemonitor.utils.SharedPrefsManager.set
import com.manimarank.websitemonitor.worker.WorkManagerScheduler
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object Utils {
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
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
        val isScheduled: Boolean? = SharedPrefsManager.customPrefs[Constants.IS_SCHEDULED, false]

        isScheduled?.let { scheduled ->
            if (!scheduled || isForce) {
                SharedPrefsManager.customPrefs.set(Constants.IS_SCHEDULED, true)
                WorkManagerScheduler.refreshPeriodicWork(context)
            }
        }
    }

    fun getMonitorInterval() : Long {
        return (SharedPrefsManager.customPrefs[Constants.MONITORING_INTERVAL, DEFAULT_INTERVAL_MIN] ?: DEFAULT_INTERVAL_MIN).toLong()
    }

    fun getMonitorTime() : String {
        val interval = getMonitorInterval().toInt()
        var refreshTime: String? = null
        if (valueList.contains(interval)) {
            val pos = valueList.indexOf(interval)
            if (pos >= 0 && pos < nameList.size)
                refreshTime = nameList[pos]
        }
        return "Checking every ${refreshTime ?: "1 hour once"}"
    }

    fun isCustomRom(): Boolean {
        return listOf("xiaomi", "oppo", "vivo")
            .contains(
                android.os.Build.MANUFACTURER.lowercase(Locale.ROOT)
            )
    }

    fun openAutoStartScreen(context: Context) {
        val intent = Intent()
        when(android.os.Build.MANUFACTURER.lowercase(Locale.ROOT)) {
            "xiaomi" -> intent.component= ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            "oppo" -> intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            "vivo" -> intent.component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        }

        val list = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (list.size > 0) {
            context.startActivity(intent)
        }
    }

    fun showAutoStartEnableDialog(context: Context) {
        if (isCustomRom() && !SharedPrefsManager.customPrefs.getBoolean(Constants.IS_AUTO_START_SHOWN, false)) {
            val alertBuilder = AlertDialog.Builder(context)
            alertBuilder.setTitle(context.getString(R.string.enable_auto_start))
            alertBuilder.setMessage(context.getString(R.string.message_auto_start_reason))
            alertBuilder.setPositiveButton(context.getString(R.string.ok)) { dialog, _ ->
                SharedPrefsManager.customPrefs[Constants.IS_AUTO_START_SHOWN] = true
                openAutoStartScreen(context)
                dialog.dismiss()
            }
            alertBuilder.setNegativeButton(context.getString(R.string.cancel), null)
            val dialog = alertBuilder.create()
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    fun openUrl(context: Context, url: String) {
        try {
            val intents = Intent(Intent.ACTION_VIEW)
            intents.data = Uri.parse(url)
            intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intents)
        } catch (e: Exception) {
            Print.log(e.toString())
            Toast.makeText(context, context.getString(R.string.no_apps_found), Toast.LENGTH_LONG)
                .show()
        }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showSnackBar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    fun getStatusMessage(code: Int?): String {
        return when (code) {
            HttpURLConnection.HTTP_OK -> "Success" // 200
            HttpURLConnection.HTTP_CREATED -> "Created" // 201
            HttpURLConnection.HTTP_ACCEPTED -> "Accepted" // 202
            HttpURLConnection.HTTP_NOT_AUTHORITATIVE -> "Non-Authoritative Information" // 203
            HttpURLConnection.HTTP_NO_CONTENT -> "No Content" // 204
            HttpURLConnection.HTTP_RESET -> "Reset Content" // 205
            HttpURLConnection.HTTP_PARTIAL -> "Partial Content" // 206
            HttpURLConnection.HTTP_MULT_CHOICE -> "Multiple Choices" // 300
            HttpURLConnection.HTTP_MOVED_PERM -> "Moved Permanently" // 301
            HttpURLConnection.HTTP_MOVED_TEMP -> "Temporary Redirect" // 302
            HttpURLConnection.HTTP_SEE_OTHER -> "See Other" // 303
            HttpURLConnection.HTTP_NOT_MODIFIED -> "Not Modified" // 304
            HttpURLConnection.HTTP_USE_PROXY -> "Use Proxy" // 305
            HttpURLConnection.HTTP_BAD_REQUEST -> "Bad Request" // 400
            HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized" // 401
            HttpURLConnection.HTTP_PAYMENT_REQUIRED -> "Payment Required" // 402
            HttpURLConnection.HTTP_FORBIDDEN -> "Forbidden" // 403
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
            else -> "Unknown"
        }
    }

    private fun isServerRelatedFail(status: Int): Boolean {
        return status >= 500
    }

    fun isValidNotifyStatus(status: Int): Boolean {
        val isEnabledServerFailOnly = SharedPrefsManager
            .customPrefs.getBoolean(Constants.NOTIFY_ONLY_SERVER_ISSUES, false)
        return if (isEnabledServerFailOnly) {
            status != HttpURLConnection.HTTP_OK && isServerRelatedFail(status)
        } else {
            status != HttpURLConnection.HTTP_OK
        }
    }

    fun resumeApp() {
        MyApplication.ActivityVisibility.resumeApp()
    }

    fun pauseApp() {
        MyApplication.ActivityVisibility.pauseApp()
    }

    fun appIsVisible(): Boolean {
        return MyApplication.ActivityVisibility.appIsVisible
    }
}