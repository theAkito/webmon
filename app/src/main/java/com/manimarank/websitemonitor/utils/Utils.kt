package com.manimarank.websitemonitor.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.google.android.material.snackbar.Snackbar
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
import kotlin.random.Random.Default.nextInt

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

    fun isCustomRom(): Boolean { return listOf("xiaomi", "oppo", "vivo").contains(android.os.Build.MANUFACTURER.toLowerCase(Locale.ROOT)) }

    fun openAutoStartScreen(context: Context) {
        val intent = Intent()
        when(android.os.Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
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
            HttpURLConnection.HTTP_ACCEPTED -> "Accepted"
            HttpURLConnection.HTTP_BAD_GATEWAY -> "Bad Gateway"
            HttpURLConnection.HTTP_BAD_METHOD -> "Method Not Allowed"
            HttpURLConnection.HTTP_BAD_REQUEST -> "Bad Request"
            HttpURLConnection.HTTP_CLIENT_TIMEOUT -> "Request Time-Out"
            HttpURLConnection.HTTP_CONFLICT -> "Conflict"
            HttpURLConnection.HTTP_CREATED -> "Created"
            HttpURLConnection.HTTP_ENTITY_TOO_LARGE -> "Request Entity Too Large"
            HttpURLConnection.HTTP_FORBIDDEN -> "Forbidden"
            HttpURLConnection.HTTP_GATEWAY_TIMEOUT -> "Gateway Timeout"
            HttpURLConnection.HTTP_GONE -> "Gone"
            HttpURLConnection.HTTP_INTERNAL_ERROR -> "Internal Server Error"
            HttpURLConnection.HTTP_LENGTH_REQUIRED -> "Length Required"
            HttpURLConnection.HTTP_MOVED_PERM -> "Moved Permanently"
            HttpURLConnection.HTTP_MOVED_TEMP -> "Temporary Redirect"
            HttpURLConnection.HTTP_MULT_CHOICE -> "Multiple Choices"
            HttpURLConnection.HTTP_NOT_ACCEPTABLE -> "Not Acceptable"
            HttpURLConnection.HTTP_NOT_AUTHORITATIVE -> "Non-Authoritative Information"
            HttpURLConnection.HTTP_NOT_FOUND -> "Not Found"
            HttpURLConnection.HTTP_NOT_IMPLEMENTED -> "Not Implemented"
            HttpURLConnection.HTTP_NOT_MODIFIED -> "Not Modified"
            HttpURLConnection.HTTP_NO_CONTENT -> "No Content"
            HttpURLConnection.HTTP_OK -> "Success"
            HttpURLConnection.HTTP_PARTIAL -> "Partial Content"
            HttpURLConnection.HTTP_PAYMENT_REQUIRED -> "Payment Required"
            HttpURLConnection.HTTP_PRECON_FAILED -> "Precondition Failed"
            HttpURLConnection.HTTP_PROXY_AUTH -> "Proxy Authentication Required"
            HttpURLConnection.HTTP_REQ_TOO_LONG -> "Request-URI Too Large"
            HttpURLConnection.HTTP_RESET -> "Reset Content"
            HttpURLConnection.HTTP_SEE_OTHER -> "See Other"
            HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized"
            HttpURLConnection.HTTP_UNAVAILABLE -> "Service Unavailable"
            HttpURLConnection.HTTP_UNSUPPORTED_TYPE -> "Unsupported Media Type"
            HttpURLConnection.HTTP_USE_PROXY -> "Use Proxy"
            HttpURLConnection.HTTP_VERSION -> "HTTP Version Not Supported"
            else -> "Unknown"
        }
    }
}