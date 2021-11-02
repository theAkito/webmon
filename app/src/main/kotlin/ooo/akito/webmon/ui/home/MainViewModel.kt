package ooo.akito.webmon.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.WebSiteStatus
import ooo.akito.webmon.data.repository.WebSiteEntryRepository
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: WebSiteEntryRepository = WebSiteEntryRepository(application)
  private val allWebSiteEntryList: LiveData<List<WebSiteEntry>> =
    repository.getAllWebSiteEntryList()
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

  fun getAllWebSiteStatusList(): LiveData<List<WebSiteStatus>> {
    return webSiteStatusList
  }

  fun getWebSiteStatus(): LiveData<WebSiteStatus> {
    return webSiteStatus
  }

  fun checkWebSiteStatus() {
    viewModelScope.launch {
      webSiteStatusList.value = repository.checkWebSiteStatus()
    }
  }

  fun getWebSiteStatus(webSiteEntry: WebSiteEntry) {
    viewModelScope.launch {
      webSiteStatus.value = repository.getWebsiteStatus(webSiteEntry)
    }
  }

  fun addDefaultData() {
    if (SharedPrefsManager.customPrefs.getBoolean(Constants.IS_ADDED_DEFAULT_DATA, true))
      repository.addDefaultData()
  }

}