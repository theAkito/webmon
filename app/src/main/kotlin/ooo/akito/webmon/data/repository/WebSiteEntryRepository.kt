package ooo.akito.webmon.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import ooo.akito.webmon.data.db.DbHelper
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.db.WebSiteEntryDao
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.SharedPrefsManager
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.currentDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class WebSiteEntryRepository(context: Context) {

  private val webSiteEntryDao: WebSiteEntryDao? by lazy {
    DbHelper.getInstance(context)?.webSiteEntryDao()
  }
  private val allWebSiteEntry: LiveData<List<WebSiteEntry>> = webSiteEntryDao?.getAllWebSiteEntryList()!!

  fun addDefaultData() = runBlocking {
    this.launch(Dispatchers.IO) {
      webSiteEntryDao?.saveWebSiteEntry(
        WebSiteEntry(
          name = "Nim Homepage",
          url = "https://nim-lang.org/",
          itemPosition = 0
        )
      )
      webSiteEntryDao?.saveWebSiteEntry(
        WebSiteEntry(
          name = "Unavailable Website",
          url = "https://error.duckduckgo.com/",
          itemPosition = 1
        )
      )
      SharedPrefsManager.customPrefs[Constants.IS_ADDED_DEFAULT_DATA] = false
    }
  }

  fun saveWebSiteEntry(websiteEntry: WebSiteEntry) = runBlocking {
    this.launch(Dispatchers.IO) {
      webSiteEntryDao?.saveWebSiteEntry(websiteEntry)
    }
  }

  fun updateWebSiteEntry(websiteEntry: WebSiteEntry) = runBlocking {
    this.launch(Dispatchers.IO) {
      webSiteEntryDao?.updateWebSiteEntry(websiteEntry)
    }
  }


  fun deleteWebSiteEntry(websiteEntry: WebSiteEntry) {
    runBlocking {
      this.launch(Dispatchers.IO) {
        webSiteEntryDao?.deleteWebSiteEntry(websiteEntry)
      }
    }
  }

  fun getAllWebSiteEntryList(): LiveData<List<WebSiteEntry>> {
    return allWebSiteEntry
  }


  suspend fun checkWebSiteStatus(): ArrayList<WebSiteStatus> {
    val statusList = ArrayList<WebSiteStatus>()

    withContext(Dispatchers.IO) {
      webSiteEntryDao?.getAllValidWebSiteEntryDirectList()?.forEach {
        statusList.add(getWebsiteStatus(it))
      }
    }
    return statusList
  }

  suspend fun getWebsiteStatus(websiteEntry: WebSiteEntry): WebSiteStatus {
    var  webSiteStatus: WebSiteStatus
    withContext(Dispatchers.IO) {
      var status = HttpURLConnection.HTTP_NOT_FOUND
      try {
        val url = URL(websiteEntry.url)
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.connect()

        status = conn.responseCode
        webSiteStatus = WebSiteStatus(
          websiteEntry.name,
          websiteEntry.url,
          conn.responseCode,
          conn.responseCode == HttpURLConnection.HTTP_OK,
          conn.responseMessage
        )
      } catch (e: Exception) {
        webSiteStatus = WebSiteStatus(
          websiteEntry.name,
          websiteEntry.url,
          status,
          false,
          e.localizedMessage ?: "Please check."
        )
      }

      updateWebSiteEntry(websiteEntry.apply {
        this.status = status
        updatedAt = currentDateTime()
      })
    }

    return webSiteStatus
  }

}