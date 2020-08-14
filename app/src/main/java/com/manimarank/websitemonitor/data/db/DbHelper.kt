package com.manimarank.websitemonitor.data.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(entities = [WebSiteEntry::class], version = 1, exportSchema = false)
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