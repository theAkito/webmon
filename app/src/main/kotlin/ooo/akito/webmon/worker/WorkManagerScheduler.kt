package ooo.akito.webmon.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import ooo.akito.webmon.utils.Constants.TAG_WORK_MANAGER
import ooo.akito.webmon.utils.Utils.getMonitorInterval
import java.util.concurrent.TimeUnit


object WorkManagerScheduler {
  /**
    https://developer.android.com/codelabs/android-workmanager-java#0
  */

  fun refreshPeriodicWork(context: Context) {
    val myConstraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()

    val refreshCpnWork = PeriodicWorkRequest
      .Builder(SyncWorker::class.java, getMonitorInterval(), TimeUnit.MINUTES)
      .setConstraints(myConstraints)
      .addTag(TAG_WORK_MANAGER)
      .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
      TAG_WORK_MANAGER,
      ExistingPeriodicWorkPolicy.REPLACE,
      refreshCpnWork
    )
  }
}