package com.manimarank.websitemonitor.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manimarank.websitemonitor.data.api.ApiAdapter.apiClient
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.data.model.WebSiteStatus
import com.manimarank.websitemonitor.data.repository.WebSiteEntryRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WebSiteEntryRepository = WebSiteEntryRepository(application)
    private val allWebSiteEntryList: LiveData<List<WebSiteEntry>> = repository.getAllWebSiteEntryList()
    private val webSiteStatusList: MutableLiveData<List<WebSiteStatus>> = MutableLiveData()

    fun saveTodo(webSiteEntry: WebSiteEntry) {
        repository.saveWebSiteEntry(webSiteEntry)
    }

    fun updateTodo(webSiteEntry: WebSiteEntry) {
        repository.updateWebSiteEntry(webSiteEntry)
    }

    fun deleteTodo(webSiteEntry: WebSiteEntry) {
        repository.deleteWebSiteEntry(webSiteEntry)
    }

    fun getWebSiteEntryList(): LiveData<List<WebSiteEntry>> {
        return allWebSiteEntryList
    }

    fun getAllWebSiteStatusList(): LiveData<List<WebSiteStatus>> {
        return webSiteStatusList
    }

    fun checkWebSiteStatus() {
        viewModelScope.launch {
            webSiteStatusList.value = repository.checkWebSiteStatus()
        }
    }

    fun addDefaultData() {
        repository.addDefaultData()
    }

}