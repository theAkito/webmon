package com.manimarank.websitemonitor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WebSiteEntry::class], version = 2, exportSchema = false)
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
                       .fallbackToDestructiveMigration() //TODO: Implement proper migration strategy.
                       .build()
               }
            }
            return INSTANCE
        }
    }

}