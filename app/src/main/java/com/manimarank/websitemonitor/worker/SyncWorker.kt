package com.manimarank.websitemonitor.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manimarank.websitemonitor.data.repository.WebSiteEntryRepository
import com.manimarank.websitemonitor.utils.Print
import com.manimarank.websitemonitor.utils.Utils


class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private lateinit var repository: WebSiteEntryRepository

    override suspend fun doWork(): Result {
        val applicationContext: Context = applicationContext

        repository = WebSiteEntryRepository(applicationContext)

        Print.log("Fetching Data from Remote host")
        return try {
            repository.checkWebSiteStatus().filter { !it.isSuccessful }.forEach {
                Print.log("Error Page : $it")
                Utils.showNotification(applicationContext, it.name, it.url + " - Not Working!")
            }
            Result.success()
        } catch (e: Throwable) {
            e.printStackTrace()
            Print.log("Error fetching data : $e")
            Result.failure()
        }
    }
}