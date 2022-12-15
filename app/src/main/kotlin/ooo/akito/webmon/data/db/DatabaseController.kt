package ooo.akito.webmon.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(
  entities = [WebSiteEntry::class],
  version = 7, /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
  exportSchema = true,
  autoMigrations = [
    AutoMigration (from = 1, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    AutoMigration (from = 2, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    AutoMigration (from = 3, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    AutoMigration (from = 4, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    AutoMigration (from = 5, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    AutoMigration (from = 6, to = 7), /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
    //AutoMigration (from = 7, to = 8) /* DATABASE_MIGRATION: Change me to the newest version, when `ooo.akito.webmon.data.db.WebSiteEntry` changes. */
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