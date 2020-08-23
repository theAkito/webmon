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
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_DESCRIPTION
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.manimarank.websitemonitor.utils.Constants.NOTIFICATION_CHANNEL_NAME
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random.Default.nextInt

object Utils {
    fun currentDateTime(): String {
        return SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:a", Locale.ENGLISH).format(Date())
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
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(pi)
        mNotificationManager.notify(Random().nextInt(), mBuilder.build())
    }
}