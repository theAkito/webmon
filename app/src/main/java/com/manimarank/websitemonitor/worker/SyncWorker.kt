package com.manimarank.websitemonitor.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manimarank.websitemonitor.data.repository.WebSiteEntryRepository
import com.manimarank.websitemonitor.utils.Print
import com.manimarank.websitemonitor.utils.Utils
import com.manimarank.websitemonitor.utils.Utils.getStringNotWorking
import com.manimarank.websitemonitor.utils.Utils.joinToStringDescription


class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private lateinit var repository: WebSiteEntryRepository

    override suspend fun doWork(): Result {
        /*
          Running according to set interval.
          Shows Notifications for failed Websites, when App is in Background.
        */

        if (Utils.appIsVisible()) {
            /*
              We only want to send notifications, when App is in Background.
            */
            return Result.success()
        }

        val applicationContext: Context = applicationContext

        repository = WebSiteEntryRepository(applicationContext)

        Print.log("Fetching Data from Remote hosts...")
        return try {
            val entriesWithFailedConnection =
                repository.checkWebSiteStatus().filter { Utils.mayNotifyStatusFailure(it.status) }
            if (entriesWithFailedConnection.size == 1) {
                val entryWithFailedConnection = entriesWithFailedConnection.first()
                Utils.showNotification(
                    applicationContext,
                    entryWithFailedConnection.name,
                    applicationContext.getStringNotWorking(entryWithFailedConnection.url)
                )
            } else {
                Utils.showNotification(
                    applicationContext,
                    "Several Websites are not reachable!",
                    entriesWithFailedConnection.joinToStringDescription()
                )
            }
            Result.success()
        } catch (e: Throwable) {
            e.printStackTrace()
            Print.log("Error fetching data : $e")
            Result.failure()
        }
    }
}