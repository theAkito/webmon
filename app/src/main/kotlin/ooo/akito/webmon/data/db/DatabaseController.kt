package ooo.akito.webmon.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(
  entities = [WebSiteEntry::class],
  version = 6,
  exportSchema = true,
  autoMigrations = [
    AutoMigration (from = 1, to = 6),
    AutoMigration (from = 2, to = 6),
    AutoMigration (from = 3, to = 6),
    AutoMigration (from = 4, to = 6),
    AutoMigration (from = 5, to = 6)
//    AutoMigration (from = 6, to = 7)
  ]
)
@TypeConverters(DatabaseTypeConvertorList::class)
abstract class DatabaseController: RoomDatabase() {

  abstract fun webSiteEntryDao(): WebSiteEntryDao

  companion object{
    private var INSTANCE: DatabaseController? = null

    fun getInstance(context: Context): DatabaseController? {
      if (INSTANCE == null) {
        synchronized(DatabaseController::class) {
          INSTANCE = Room.databaseBuilder(context,
            DatabaseController::class.java,
            "web_site_monitor_db")
            .build()
        }
      }
      return INSTANCE
    }
  }

}