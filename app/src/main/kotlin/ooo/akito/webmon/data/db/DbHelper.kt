package ooo.akito.webmon.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [WebSiteEntry::class],
  version = 4,
  exportSchema = true,
  autoMigrations = [
    AutoMigration (from = 1, to = 2),
    AutoMigration (from = 2, to = 3),
    AutoMigration (from = 3, to = 4),
//    AutoMigration (from = 4, to = 5)
  ]
)
abstract class DbHelper: RoomDatabase() {

  abstract fun webSiteEntryDao(): WebSiteEntryDao

  companion object{
    private var INSTANCE: DbHelper? = null

    fun getInstance(context: Context): DbHelper? {
      if (INSTANCE == null) {
        synchronized(DbHelper::class) {
          INSTANCE = Room.databaseBuilder(context,
            DbHelper::class.java,
            "web_site_monitor_db")
            .build()
        }
      }
      return INSTANCE
    }
  }

}