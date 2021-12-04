package ooo.akito.webmon.data.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.data.repository.WebSiteEntryRepository
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.SharedPrefsManager.customPrefs
import ooo.akito.webmon.utils.Utils.retrieveIconUrlFetcher
import ooo.akito.webmon.utils.iconUrlFetcher


class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: WebSiteEntryRepository = WebSiteEntryRepository(application)
  private val allWebSiteEntryList: LiveData<List<WebSiteEntry>> = repository.getAllWebSiteEntryList()
  private val webSiteStatusList: MutableLiveData<List<WebSiteStatus>> = MutableLiveData()
  private val webSiteStatus: MutableLiveData<WebSiteStatus> = MutableLiveData()

  fun saveWebSiteEntry(webSiteEntry: WebSiteEntry) {
    repository.saveWebSiteEntry(webSiteEntry)
  }

  fun updateWebSiteEntry(webSiteEntry: WebSiteEntry) {
    repository.updateWebSiteEntry(webSiteEntry)
  }

  fun deleteWebSiteEntry(webSiteEntry: WebSiteEntry) {
    repository.deleteWebSiteEntry(webSiteEntry)
  }

  fun getWebSiteEntryList(): LiveData<List<WebSiteEntry>> {
    return allWebSiteEntryList
  }

  fun getWebsiteUrlToWebsiteEntry(): Map<String, WebSiteEntry> {
    return getWebSiteEntryList().value?.associateBy { it.url } ?: emptyMap()
  }

  fun checkWebSiteStatus(): LiveData<List<WebSiteStatus>> {
    viewModelScope.launch {
      webSiteStatusList.value = repository.checkWebSiteStatus()
    }
    return webSiteStatusList
  }

  fun getWebSiteStatus(webSiteEntry: WebSiteEntry): LiveData<WebSiteStatus> {
    viewModelScope.launch {
      webSiteStatus.value = repository.getWebsiteStatus(webSiteEntry)
    }
    return webSiteStatus
  }

  fun addDefaultData() {
    if (customPrefs.getBoolean(Constants.IS_ADDED_DEFAULT_DATA, true))
      repository.addDefaultData()
  }

  fun assignIconUrlFetcher() {
    viewModelScope.launch {
      retrieveIconUrlFetcher()?.let {
        iconUrlFetcher = it
      }
    }
  }

}