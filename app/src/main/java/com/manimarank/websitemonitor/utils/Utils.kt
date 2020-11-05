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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
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
        return "Checking every ${nameList[valueList.indexOf(getMonitorInterval().toInt())]}"
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
            val uri = Uri.parse(url)
            val intents = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intents)
        } catch (e: Exception) {
            Print.log(e.toString())
            Toast.makeText(context, context.getString(R.string.no_apps_found), Toast.LENGTH_LONG)
                .show()
        }
    }
}