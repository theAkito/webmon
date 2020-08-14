package com.manimarank.websitemonitor.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.manimarank.websitemonitor.data.db.DbHelper
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.data.db.WebSiteEntryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebSiteEntryRepository(application: Application) {

    private val webSiteEntryDao: WebSiteEntryDao? by lazy {  DbHelper.getInstance(application.applicationContext)?.webSiteEntryDao() }
    private val allWebSiteEntry: LiveData<List<WebSiteEntry>>

    init {
        allWebSiteEntry = webSiteEntryDao?.getAllWebSiteEntryList()!!
        if (allWebSiteEntry.value?.size ?:0 == 0)
            addDefaultData()

    }

    private fun addDefaultData()  = runBlocking {
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
}