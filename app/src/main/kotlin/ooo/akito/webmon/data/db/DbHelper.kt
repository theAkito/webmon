package ooo.akito.webmon.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WebSiteEntry::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
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