package com.manimarank.websitemonitor.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WebSiteEntryDao {

    @Insert
    suspend fun saveWebSiteEntry(webSiteEntry: WebSiteEntry)

    @Delete
    suspend fun deleteWebSiteEntry(webSiteEntry: WebSiteEntry)

    @Update
    suspend fun updateWebSiteEntry(webSiteEntry: WebSiteEntry)

    @Query("SELECT * FROM web_site_entry ORDER BY id DESC")
    fun getAllWebSiteEntryList(): LiveData<List<WebSiteEntry>>
}