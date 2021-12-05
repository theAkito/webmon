package ooo.akito.webmon.utils

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.CATEGORY_SERVICE
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_MIN
import ooo.akito.webmon.ui.home.MainActivity
import ooo.akito.webmon.utils.Utils.safelyStartSyncWorker


class AppService : Service() {

  companion object {
    private const val notifID = 19
    private const val notifChannelID = "WebmonLogger"
  }

  override fun onBind(intent: Intent): IBinder? {
    return null
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    safelyStartSyncWorker()
    startPersistentService()
    return START_STICKY
  }

  private fun startPersistentService() {
    val notificationIntent = Intent(applicationContext, MainActivity::class.java)
    /**
      https://developer.android.com/training/notify-user/channels
      https://stackoverflow.com/a/59573032/7061105
      https://stackoverflow.com/a/59572882/7061105
      https://stackoverflow.com/a/67982171/7061105
      https://stackoverflow.com/a/52258125/7061105
    */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(notifChannelID, nameAppCasePascal, NotificationManager.IMPORTANCE_DEFAULT)
      val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
      startForeground(
        notifID,
        NotificationCompat.Builder(
          applicationContext,
          notifChannelID
        )
          .setOngoing(true)
          .setSmallIcon(R.drawable.ic_notification_overlay)
          .setContentTitle("${nameAppCasePascal} Service")
          .setContentText("Running")
          .setCategory(CATEGORY_SERVICE)
          .setPriority(IMPORTANCE_MIN)
          .build()
      )
    } else {
      /** https://stackoverflow.com/a/34573169/7061105 */
      val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        notificationIntent,
        0
      )
      startForeground(
        notifID,
        NotificationCompat.Builder(
          applicationContext,
          notifChannelID
        )
          .setOngoing(true)
          .setSmallIcon(R.drawable.ic_notification_overlay)
          .setContentTitle("${nameAppCasePascal} Service")
          .setContentText("Running")
          .setContentIntent(pendingIntent)
          .setCategory(CATEGORY_SERVICE)
          .setPriority(IMPORTANCE_MIN)
          .build()
      )
    }
  }
}