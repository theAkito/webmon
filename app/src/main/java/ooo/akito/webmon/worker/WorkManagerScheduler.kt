package ooo.akito.webmon.worker

import android.content.Context
import androidx.work.*
import ooo.akito.webmon.utils.Constants.TAG_WORK_MANAGER
import ooo.akito.webmon.utils.Utils
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    fun refreshPeriodicWork(context: Context) {

        //define constraints
        val myConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshCpnWork = PeriodicWorkRequest
            .Builder(SyncWorker::class.java, Utils.getMonitorInterval(), TimeUnit.MINUTES)
            .setConstraints(myConstraints)
            .addTag(TAG_WORK_MANAGER)
            .build()


        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_WORK_MANAGER,
            ExistingPeriodicWorkPolicy.REPLACE, refreshCpnWork
        )

    }
}