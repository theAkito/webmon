package com.manimarank.websitemonitor

import android.app.Application
import com.manimarank.websitemonitor.utils.SharedPrefsManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SharedPrefsManager.init(this)
    }
}