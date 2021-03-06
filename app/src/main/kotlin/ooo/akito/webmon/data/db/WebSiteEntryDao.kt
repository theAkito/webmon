package ooo.akito.webmon.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface WebSiteEntryDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveWebSiteEntry(webSiteEntry: WebSiteEntry)

  @Delete
  suspend fun deleteWebSiteEntry(webSiteEntry: WebSiteEntry)

  @Update
  suspend fun updateWebSiteEntry(webSiteEntry: WebSiteEntry)

  @Query("SELECT * FROM web_site_entry ORDER BY id ASC")
  fun getAllWebSiteEntryList(): LiveData<List<WebSiteEntry>>

  @Query("SELECT * FROM web_site_entry ORDER BY id ASC")
  suspend fun getAllWebSiteEntryDirectList(): List<WebSiteEntry>

  @Query("SELECT * FROM web_site_entry WHERE is_paused = 0 ORDER BY id ASC")
  suspend fun getAllValidWebSiteEntryDirectList(): List<WebSiteEntry>
}