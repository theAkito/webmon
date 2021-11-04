package ooo.akito.webmon

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import ooo.akito.webmon.utils.SharedPrefsManager

class MyApplication : Application(), LifecycleObserver {

  object ActivityVisibility {
    var appIsVisible: Boolean = false
    @JvmStatic
    fun resumeApp() { appIsVisible = true }
    @JvmStatic
    fun pauseApp() { appIsVisible = false }
  }

  override fun onCreate() {
    super.onCreate()
    SharedPrefsManager.init(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  fun onAppBackgrounded() {
    ActivityVisibility.pauseApp()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onAppForegrounded() {
    ActivityVisibility.resumeApp()
  }
}