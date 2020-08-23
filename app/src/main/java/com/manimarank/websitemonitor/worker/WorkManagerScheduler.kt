package com.manimarank.websitemonitor.worker

import android.content.Context
import androidx.work.*
import com.manimarank.websitemonitor.utils.Constants.TAG_WORK_MANAGER
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    fun refreshPeriodicWork(context: Context) {

        //define constraints
        val myConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val refreshCpnWork = PeriodicWorkRequest
            .Builder(SyncWorker::class.java, 15, TimeUnit.MINUTES)
            .setConstraints(myConstraints)
            .addTag(TAG_WORK_MANAGER)
            .build()


        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG_WORK_MANAGER,
            ExistingPeriodicWorkPolicy.REPLACE, refreshCpnWork
        )

    }
}