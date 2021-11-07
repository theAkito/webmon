package ooo.akito.webmon.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.HandlerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import ooo.akito.webmon.R
import ooo.akito.webmon.databinding.ActivitySettingsBinding
import ooo.akito.webmon.ui.home.MainViewModel
import ooo.akito.webmon.utils.BackgroundCheckInterval.nameList
import ooo.akito.webmon.utils.BackgroundCheckInterval.valueList
import ooo.akito.webmon.utils.Constants
import ooo.akito.webmon.utils.Constants.HIDE_IS_ONION_ADDRESS
import ooo.akito.webmon.utils.Constants.MONITORING_INTERVAL
import ooo.akito.webmon.utils.Constants.NOTIFY_ONLY_SERVER_ISSUES
import ooo.akito.webmon.utils.Constants.SETTINGS_TOR_ENABLE
import ooo.akito.webmon.utils.ExceptionCompanion.msgSpecificToRebirth
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.getMonitorTime
import ooo.akito.webmon.utils.Utils.isCustomRom
import ooo.akito.webmon.utils.Utils.openAutoStartScreen
import ooo.akito.webmon.utils.Utils.safelyStartSyncWorker
import ooo.akito.webmon.utils.Utils.startWorkManager
import ooo.akito.webmon.utils.Utils.triggerRebirth
import ooo.akito.webmon.utils.msgGenericRestarting
import ooo.akito.webmon.utils.workaroundRebirthMillis


class SettingsActivity : AppCompatActivity() {

  private lateinit var viewModel: MainViewModel
  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var btnMonitorInterval: LinearLayout
  private lateinit var layoutEnableAutoStart: LinearLayout
  private lateinit var btnEnableAutoStart: TextView
  private lateinit var switchNotifyOnlyServerIssues: SwitchMaterial
  private lateinit var switchSettingsTorEnable: SwitchMaterial
  private lateinit var txtIntervalDetails: AppCompatTextView


  private fun restartApp() {
    /*
      Workaround for Shared Preference not being saved, when App is restarted too quickly.
      https://www.py4u.net/discuss/612951
    */
    Snackbar.make(activitySettingsBinding.root, msgGenericRestarting, Snackbar.LENGTH_LONG).show()
    HandlerCompat.postDelayed(
      Handler(Looper.myLooper() ?: throw Exception(msgSpecificToRebirth)
      ), {
        kotlin.run {
          triggerRebirth(this@SettingsActivity.applicationContext)
        }
      }, 0, workaroundRebirthMillis)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Setting up ViewModel and LiveData
    viewModel = ViewModelProvider(this)[MainViewModel::class.java]

    activitySettingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
    setContentView(activitySettingsBinding.root)
    btnMonitorInterval = activitySettingsBinding.btnMonitorInterval
    layoutEnableAutoStart = activitySettingsBinding.layoutEnableAutoStart
    btnEnableAutoStart = activitySettingsBinding.btnEnableAutoStart
    switchNotifyOnlyServerIssues = activitySettingsBinding.switchNotifyOnlyServerIssues
    switchSettingsTorEnable = activitySettingsBinding.settingsTorEnable
    txtIntervalDetails = activitySettingsBinding.txtIntervalDetails

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    btnMonitorInterval.setOnClickListener { showIntervalChooseDialog() }

    layoutEnableAutoStart.visibility = if (isCustomRom()) View.VISIBLE else View.GONE
    btnEnableAutoStart.setOnClickListener { openAutoStartScreen(this) }

    updateIntervalTimeOnUi()

    switchNotifyOnlyServerIssues.isChecked = SharedPrefsManager.customPrefs.getBoolean(NOTIFY_ONLY_SERVER_ISSUES, false)
    switchNotifyOnlyServerIssues.setOnCheckedChangeListener { _, isChecked ->
      SharedPrefsManager.customPrefs[NOTIFY_ONLY_SERVER_ISSUES] = isChecked
    }

    switchSettingsTorEnable.isChecked = SharedPrefsManager.customPrefs.getBoolean(SETTINGS_TOR_ENABLE, false)
    switchSettingsTorEnable.setOnCheckedChangeListener { _, isChecked ->
      SharedPrefsManager.customPrefs[SETTINGS_TOR_ENABLE] = isChecked
      SharedPrefsManager.customPrefs[HIDE_IS_ONION_ADDRESS] = false
      Log.warn("Tor switched to ${isChecked}!")
      restartApp()
    }

    //region Advanced Settings

    activitySettingsBinding.toggleSettingsAdvanced.isActivated = false

    //region Slide Animation

    fun slideLayoutSettingsAdvanced(show: Boolean) {
      val layout = activitySettingsBinding.layoutSettingsAdvanced
      val parent = activitySettingsBinding.layoutSettingsAdvanced.parent as ViewGroup

      val transition = Slide().apply {
        duration = if (show) {
          500 // Milliseconds
        } else {
          0 // Milliseconds
        }
        addTarget(layout)
        slideEdge = Gravity.TOP
      }

      TransitionManager.beginDelayedTransition(parent, transition)
      layout.visibility = if (show) {
        View.VISIBLE
      } else {
        TransitionManager.endTransitions(parent)
        View.GONE
      }
    }

    //endregion Slide Animation

    /* Hide by default. */
    activitySettingsBinding.layoutSettingsAdvanced.visibility = View.GONE

    activitySettingsBinding.toggleSettingsAdvanced.setOnCheckedChangeListener { _, isActivated ->
      slideLayoutSettingsAdvanced(isActivated)
    }

    activitySettingsBinding.toggleSettingsAdvanced.visibility = View.VISIBLE

    //region Advanced Setting: Delete All Website Entries

    activitySettingsBinding.btnWebsiteEntriesDeleteAll.setOnClickListener {
      AlertDialog.Builder(this).apply {
        setMessage(R.string.text_delete_all_website_entries_are_you_sure)
        setTitle(R.string.text_delete_all_website_entries)
        setPositiveButton(
          R.string.text_delete_all_website_entries_are_you_sure_yes
        ) { _, _ ->
          with(viewModel) {
            getWebSiteEntryList().observe(
              this@SettingsActivity, { websites ->
                websites.forEach { website ->
                  deleteWebSiteEntry(website)
                }
              }
            )
          }
        }
        setNegativeButton(R.string.text_delete_all_website_entries_are_you_sure_no) { _, _ -> }
      }.create().show()
    }

    //endregion Advanced Setting: Delete All Website Entries

    //endregion Advanced Settings
  }

  private fun showIntervalChooseDialog() {
    AlertDialog.Builder(this).apply {
      val checkedItem = valueList.indexOf(SharedPrefsManager.customPrefs.getInt(MONITORING_INTERVAL, 60))
      setTitle(getString(R.string.choose_interval))
      setSingleChoiceItems(
        nameList,
        checkedItem
      ) { dialog: DialogInterface, chosenIntervalPosition: Int ->
        SharedPrefsManager.customPrefs[MONITORING_INTERVAL] = valueList[chosenIntervalPosition]
        updateIntervalTimeOnUi()
        dialog.dismiss()
        /*
          SyncWorker is only started from MainActivity,
          to make sure it is not run by MainActivity and this one, simultaneously.
        */
        restartApp()
      }
      setNegativeButton(getString(R.string.cancel), null)
    }.create().show()
  }

  private fun updateIntervalTimeOnUi() { txtIntervalDetails.text = getMonitorTime() }
}