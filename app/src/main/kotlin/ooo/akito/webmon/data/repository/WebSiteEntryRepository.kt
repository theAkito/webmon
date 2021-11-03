package ooo.akito.webmon.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ooo.akito.webmon.data.db.DbHelper
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.db.WebSiteEntryDao
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.net.dns.DNS
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.addProtoHttp
import ooo.akito.webmon.utils.Utils.currentDateTime
import java.net.HttpURLConnection
import java.net.URL

class WebSiteEntryRepository(context: Context) {

  companion object {
    private val dns = DNS()
  }

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
      var msg = ""
      try {
        val rawUrl = websiteEntry.url
        val checkRecordsAAAAA = websiteEntry.dnsRecordsAAAAA

        val ipAddresses = if (checkRecordsAAAAA) {
          dns.retrieveAllIPsFromDnsRecordsAsStrings(rawUrl)
        } else {
          listOf(rawUrl)
        }

        val urls = ipAddresses.map { URL(it.addProtoHttp()) }

        val connSuccessIfEmpty = urls.mapIndexedNotNull CONNECTIONS@{ index, url ->
          val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
          conn.connect()
          val code = conn.responseCode
          val msg = conn.responseMessage
          val isLastInLine = if (urls.size == 1) {
            true
          } else {
            index == urls.size.dec()
          }
          if (code == HttpURLConnection.HTTP_OK && isLastInLine.not()) {
            Log.warn("WebsiteEntry with Domain ${rawUrl} has unreachable IP: " + url)
            return@CONNECTIONS code to msg
          } else if (isLastInLine.not()) {
            return@CONNECTIONS null
          }
          if (isLastInLine) {
            return@CONNECTIONS code to msg
          } else {
            null
          }
        }

        val connSuccess = connSuccessIfEmpty.isEmpty()
        val codeToMsg = connSuccessIfEmpty.firstOrNull()
        status = codeToMsg?.first ?: 0
        msg = codeToMsg?.second ?: "Unknown"

        webSiteStatus = WebSiteStatus(
          websiteEntry.name,
          websiteEntry.url,
          codeToMsg?.first ?: 0,
          connSuccess,
          msg
        )
      } catch (e: Exception) {
        Log.error(e.stackTraceToString())
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