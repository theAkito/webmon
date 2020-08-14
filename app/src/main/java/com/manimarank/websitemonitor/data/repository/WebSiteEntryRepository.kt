package com.manimarank.websitemonitor.data.repository

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.manimarank.websitemonitor.data.api.ApiAdapter.apiClient
import com.manimarank.websitemonitor.data.db.DbHelper
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.data.db.WebSiteEntryDao
import com.manimarank.websitemonitor.data.model.WebSiteStatus
import com.manimarank.websitemonitor.utils.Utils.currentDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class WebSiteEntryRepository(application: Application) {

    private val webSiteEntryDao: WebSiteEntryDao? by lazy {  DbHelper.getInstance(application.applicationContext)?.webSiteEntryDao() }
    private val allWebSiteEntry: LiveData<List<WebSiteEntry>>

    init {
        allWebSiteEntry = webSiteEntryDao?.getAllWebSiteEntryList()!!
    }

    fun addDefaultData()  = runBlocking {
        this.launch(Dispatchers.IO) {
            webSiteEntryDao?.saveWebSiteEntry(WebSiteEntry(name = "Coopon Website", url = "https://cooponscitech.in/"))
            webSiteEntryDao?.saveWebSiteEntry(WebSiteEntry(name ="Coopon Wiki", url = "https://wiki.cooponscitech.in/"))
            webSiteEntryDao?.saveWebSiteEntry(WebSiteEntry(name ="JIPMER Site", url = "https://pprg.cooponscitech.in/"))
            webSiteEntryDao?.saveWebSiteEntry(WebSiteEntry(name ="Error Site", url = "https://xyz.cooponscitech.in/"))
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


     suspend fun checkWebSiteStatus(): ArrayList<WebSiteStatus>{
        val statusList = ArrayList<WebSiteStatus>()

         withContext(Dispatchers.IO) {

             allWebSiteEntry.value?.forEach {
                 var status = 404
                 try {
                     //val webSiteStatus = apiClient(it.url).getWebsiteStatus()
                     val inputStream: InputStream
                     val result: String

                     // create URL
                     val url: URL = URL(it.url)

                     // create HttpURLConnection
                     val conn: HttpURLConnection = url.openConnection() as HttpURLConnection

                     // make GET request to the given URL
                     conn.connect()

                     status = conn.responseCode
                     statusList.add(
                         WebSiteStatus(
                             it.name,
                             it.url,
                             conn.responseCode,
                             conn.responseCode == 200,
                             conn.responseMessage
                         )
                     )
                 } catch (e: Exception) {
                     statusList.add(
                         WebSiteStatus(
                             it.name,
                             it.url,
                             status,
                             false,
                             e.localizedMessage ?: "Please check"
                         )
                     )
                 }

                 updateWebSiteEntry(it.apply {
                     it.status = status
                     it.updatedAt = currentDateTime()
                 })
             }
         }
        return statusList
    }

}