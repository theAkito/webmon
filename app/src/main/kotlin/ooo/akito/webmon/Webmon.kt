package ooo.akito.webmon

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager

class Webmon : Application(), LifecycleEventObserver, LifecycleOwner {
  /**
    https://developer.android.com/reference/kotlin/androidx/lifecycle/Lifecycle
    https://developer.android.com/reference/kotlin/androidx/lifecycle/LifecycleOwner
    https://developer.android.com/reference/kotlin/androidx/lifecycle/LifecycleEventObserver
    https://developer.android.com/reference/kotlin/androidx/lifecycle/Lifecycle.Event
    https://developer.android.com/reference/kotlin/androidx/lifecycle/Lifecycle.State
    https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner
    https://developer.android.com/topic/libraries/architecture/lifecycle
  */

  private lateinit var lifecycleRegistry: LifecycleRegistry

  object AppVisibility {
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
    lifecycleRegistry = LifecycleRegistry(this)
  }

  override fun getLifecycle(): Lifecycle {
    return lifecycleRegistry
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    when (event) {
      Lifecycle.Event.ON_START -> {
        Log.info("""App started.""")
        AppVisibility.resumeApp()
      }
      Lifecycle.Event.ON_STOP -> {
        Log.info("""App stopped.""")
        AppVisibility.pauseApp()
      }
      else -> {}
    }
  }

}