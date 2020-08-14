package com.manimarank.websitemonitor.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.data.repository.WebSiteEntryRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WebSiteEntryRepository = WebSiteEntryRepository(application)
    private val allTodoList: LiveData<List<WebSiteEntry>> = repository.getAllWebSiteEntryList()

    fun saveTodo(webSiteEntry: WebSiteEntry) {
        repository.saveWebSiteEntry(webSiteEntry)
    }

    fun updateTodo(webSiteEntry: WebSiteEntry) {
        repository.updateWebSiteEntry(webSiteEntry)
    }

    fun deleteTodo(webSiteEntry: WebSiteEntry) {
        repository.deleteWebSiteEntry(webSiteEntry)
    }

    fun getAllTodoList(): LiveData<List<WebSiteEntry>> {
        return allTodoList
    }

}