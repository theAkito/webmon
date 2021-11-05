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
import ooo.akito.webmon.net.Utils.onionConnectionIsSuccessful
import ooo.akito.webmon.net.dns.DNS
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.msgGenericSuccess
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeGenericFail
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeNXDOMAIN
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeTorAppUnavailable
import ooo.akito.webmon.utils.ExceptionCompanion.connCodeTorConnFailed
import ooo.akito.webmon.utils.ExceptionCompanion.msgCannotConnectToTor
import ooo.akito.webmon.utils.ExceptionCompanion.msgDnsOnlyNXDOMAIN
import ooo.akito.webmon.utils.ExceptionCompanion.msgDnsRootDomain
import ooo.akito.webmon.utils.ExceptionCompanion.msgGenericFailure
import ooo.akito.webmon.utils.ExceptionCompanion.msgGenericUnknown
import ooo.akito.webmon.utils.ExceptionCompanion.msgMiniNXDOMAIN
import ooo.akito.webmon.utils.ExceptionCompanion.msgTorIsEnabledButNotAvailable
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.addProtoHttp
import ooo.akito.webmon.utils.Utils.currentDateTime
import ooo.akito.webmon.utils.Utils.isStatusAcceptable
import ooo.akito.webmon.utils.Utils.removeTrailingSlashes
import ooo.akito.webmon.utils.Utils.removeUrlProto
import ooo.akito.webmon.utils.Utils.torAppIsAvailable
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.impl.classic.HttpClients
import java.net.HttpURLConnection
import java.net.URI
import java.net.UnknownHostException

class WebSiteEntryRepository(context: Context) {

  companion object {
    private val dns = DNS()
    private val http = HttpClients
      .custom()
      .setRedirectStrategy(DefaultRedirectStrategy())
      .useSystemProperties()
      .build()
  }

  private val webSiteEntryDao: WebSiteEntryDao? by lazy {
    DbHelper.getInstance(context)?.webSiteEntryDao()
  }
  private val allWebSiteEntry: LiveData<List<WebSiteEntry>> = webSiteEntryDao?.getAllWebSiteEntryList()!!

  fun addDefaultData() = runBlocking {
    this.launch(Dispatchers.IO) {
      webSiteEntryDao?.saveWebSiteEntry(
        /* WebsiteEntry Glue */
        WebSiteEntry(
          name = "Nim Homepage",
          url = "https://nim-lang.org/",
          itemPosition = 0,
          isLaissezFaire = false,
          dnsRecordsAAAAA = false,
          isOnionAddress = false
        )
      )
      webSiteEntryDao?.saveWebSiteEntry(
        /* WebsiteEntry Glue */
        WebSiteEntry(
          name = "Unavailable Website",
          url = "https://error.duckduckgo.com/",
          itemPosition = 1,
          isLaissezFaire = false,
          dnsRecordsAAAAA = false,
          isOnionAddress = false
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

  private fun handleDnsRecordRetrieval(url: String): List<String> {
    return try {
      dns.retrieveAllIPsFromDnsRecords(url)
    } catch (nx: DNS.ResolvesToNowhereException) {
      listOf(url)
    }
  }

  suspend fun getWebsiteStatus(website: WebSiteEntry): WebSiteStatus {
    var  webSiteStatus: WebSiteStatus
    withContext(Dispatchers.IO) {
      val connSuccess: Boolean
      var status = HttpURLConnection.HTTP_NOT_FOUND
      var msg: String
      try {
        val rawUrl = website.url
        if (website.isOnionAddress) {
          if (torAppIsAvailable.not()) {
            Log.error(msgTorIsEnabledButNotAvailable)
            connSuccess = false
            status = connCodeTorAppUnavailable
            msg = msgTorIsEnabledButNotAvailable
          } else {
            var excepted = false
            connSuccess = try {
              onionConnectionIsSuccessful(rawUrl.removeUrlProto())
            } catch (e: Exception) {
              excepted = true
              false
            }
            status = if (connSuccess) {
              HttpURLConnection.HTTP_OK /* If other than `HTTP_OK`, the entry is shown as unsuccessful. */
            } else if (excepted) {
              connCodeTorConnFailed
            } else {
              connCodeGenericFail
            }
            msg = if (connSuccess) {
              msgGenericSuccess
            } else if (excepted) {
              msgCannotConnectToTor
            } else {
              msgGenericFailure
            }
          }
        } else {
          val checkRecordsAAAAA = website.dnsRecordsAAAAA
          val ipAddresses = if (checkRecordsAAAAA) {
            handleDnsRecordRetrieval(rawUrl.removeUrlProto().removeTrailingSlashes())
          } else {
            listOf(rawUrl)
          }

          val urls = ipAddresses.map { URI(it.addProtoHttp()) }
          val totalFailureNXDOMAIN = checkRecordsAAAAA && urls.size == 1 && ipAddresses.first() == rawUrl

          if (totalFailureNXDOMAIN) {
            /* We only received NXDOMAIN responses from the nameserver lookup. */
            Log.error(msgDnsOnlyNXDOMAIN + msgDnsRootDomain + rawUrl)
          }

          val connSuccessIfEmpty = if (totalFailureNXDOMAIN) {
            listOf(connCodeNXDOMAIN to msgMiniNXDOMAIN)
          } else {
            urls.mapIndexedNotNull CONNECTIONS@{ index, uri ->
              var conn: CloseableHttpResponse? = null
              try {
                try {
                  conn = http.execute(HttpGet(uri))
                } catch (exUnknownHost: UnknownHostException) {
                  /* Do not spam logs with stacktrace from trying to connect to unreachable host. */
                  exUnknownHost.message?.let { Log.warn(it) }
                }
                if (conn == null) { return@CONNECTIONS null }
                status = conn.code
                msg = conn.reasonPhrase
                val isLastInLine = if (urls.size == 1) {
                  true
                } else {
                  index == urls.size.dec()
                }
                website.status = status
                if (
                  website.isStatusAcceptable().not() && isLastInLine.not()
                ) {
                  Log.warn("""WebsiteEntry with Domain "${rawUrl}" has IP "${uri}" with issues: """ + status)
                  return@CONNECTIONS status to msg
                } else if (isLastInLine.not()) {
                  return@CONNECTIONS null
                }
                if (isLastInLine) {
                  return@CONNECTIONS status to msg
                } else {
                  null
                }
              } catch (exUnknownHost: UnknownHostException) {
                /* Do not spam logs with stacktrace from trying to connect to unreachable host. */
                return@CONNECTIONS null
              } catch (e: Exception) {
                Log.error(e.stackTraceToString())
                return@CONNECTIONS null
              } finally {
                try {
                  conn?.close()
                } catch (e: Exception) {
                  Log.error(e.stackTraceToString())
                }
              } // try@CONNECTIONS
            }
          }

          connSuccess = connSuccessIfEmpty.isEmpty()
          val codeToMsg = connSuccessIfEmpty.firstOrNull()
          status = codeToMsg?.first ?: connCodeGenericFail
          msg = codeToMsg?.second ?: msgGenericUnknown
        }

        webSiteStatus = WebSiteStatus(
          website.name,
          website.url,
          status,
          connSuccess,
          msg
        )
      } catch (e: Exception) {
        Log.error(e.stackTraceToString())
        webSiteStatus = WebSiteStatus(
          website.name,
          website.url,
          status,
          false,
          e.localizedMessage ?: "Please check."
        )
      }

      updateWebSiteEntry(
        website.apply {
          this.status = status
          updatedAt = currentDateTime()
        }
      )
    }

    return webSiteStatus
  }

}