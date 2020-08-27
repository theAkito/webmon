package com.manimarank.websitemonitor.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
}