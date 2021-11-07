package ooo.akito.webmon.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.repository.WebSiteEntryRepository
import ooo.akito.webmon.utils.ExceptionCompanion.msgErrorTryingToFetchData
import ooo.akito.webmon.utils.ExceptionCompanion.msgWebsitesNotReachable
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.Utils.associateByUrl
import ooo.akito.webmon.utils.Utils.doIfAppIsVisible
import ooo.akito.webmon.utils.Utils.getStringNotWorking
import ooo.akito.webmon.utils.Utils.joinToStringDescription
import ooo.akito.webmon.utils.Utils.mayNotifyStatusFailure
import ooo.akito.webmon.utils.Utils.showNotification


class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
  CoroutineWorker(appContext, workerParams) {
  /**
    https://developer.android.com/codelabs/android-workmanager-java#0
    https://developer.android.com/reference/androidx/work/ListenableWorker#isStopped()
  */
  private lateinit var repository: WebSiteEntryRepository

  override suspend fun doWork(): Result {
    /*
      Running according to set interval.
      Shows Notifications for failed Websites, when App is in Background.
    */

    repository = WebSiteEntryRepository(applicationContext)

    val websites: List<WebSiteEntry> = repository.getRecordedWebsiteEntry()
    val websiteStates = repository.checkWebSiteStatus()

    Log.info("Fetching Data from Remote hosts...")
    return try {
      val urlToWebsite: Map<String, WebSiteEntry> = websites.associateByUrl()
      val entriesWithFailedConnection =
        websiteStates.filter {
          val currentWebSite = urlToWebsite[it.url] ?: return@filter false
          mayNotifyStatusFailure(currentWebSite)
        }
      if (entriesWithFailedConnection.size == 1) {
        val entryWithFailedConnection = entriesWithFailedConnection.first()
        false.doIfAppIsVisible {
          showNotification(
            applicationContext,
            entryWithFailedConnection.name,
            applicationContext.getStringNotWorking(entryWithFailedConnection.url)
          )
        }
      } else if (entriesWithFailedConnection.size > 1) {
        false.doIfAppIsVisible {
          showNotification(
            applicationContext,
            msgWebsitesNotReachable,
            entriesWithFailedConnection.joinToStringDescription()
          )
        }
      }
      Result.success()
    } catch (e: Throwable) {
      e.printStackTrace()
      Log.error(msgErrorTryingToFetchData + e.message)
      Result.failure()
    }
  }
}