package ooo.akito.webmon.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.HandlerCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import ooo.akito.webmon.R
import ooo.akito.webmon.databinding.ActivitySettingsBinding
import ooo.akito.webmon.utils.Constants.HIDE_IS_ONION_ADDRESS
import ooo.akito.webmon.utils.Constants.MONITORING_INTERVAL
import ooo.akito.webmon.utils.Constants.NOTIFY_ONLY_SERVER_ISSUES
import ooo.akito.webmon.utils.Constants.SETTINGS_TOR_ENABLE
import ooo.akito.webmon.utils.ExceptionCompanion.msgSpecificToRebirth
import ooo.akito.webmon.utils.Interval.nameList
import ooo.akito.webmon.utils.Interval.valueList
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.getMonitorTime
import ooo.akito.webmon.utils.Utils.isCustomRom
import ooo.akito.webmon.utils.Utils.openAutoStartScreen
import ooo.akito.webmon.utils.Utils.startWorkManager
import ooo.akito.webmon.utils.Utils.triggerRebirth


class SettingsActivity : AppCompatActivity() {

  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var btnMonitorInterval: LinearLayout
  private lateinit var layoutEnableAutoStart: LinearLayout
  private lateinit var btnEnableAutoStart: TextView
  private lateinit var switchNotifyOnlyServerIssues: SwitchMaterial
  private lateinit var switchSettingsTorEnable: SwitchMaterial
  private lateinit var txtIntervalDetails: AppCompatTextView


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
      /*
        Workaround for Shared Preference not being saved, when App is restarted too quickly.
        https://www.py4u.net/discuss/612951
      */
      Snackbar.make(activitySettingsBinding.root, "Restarting!", Snackbar.LENGTH_LONG).show()
      val thisContext = this.applicationContext
      HandlerCompat.postDelayed(
        Handler(Looper.myLooper() ?: throw Exception(msgSpecificToRebirth)
      ), {
        kotlin.run {
          triggerRebirth(thisContext)
        }
      }, 0, 2000)
    }
  }

  private fun showIntervalChooseDialog() {

    val alertBuilder = AlertDialog.Builder(this)
    alertBuilder.setTitle(getString(R.string.choose_interval))

    val checkedItem = valueList.indexOf(SharedPrefsManager.customPrefs.getInt(MONITORING_INTERVAL, 60))
    alertBuilder.setSingleChoiceItems(
      nameList,
      checkedItem
    ) { dialog: DialogInterface, which: Int ->
      SharedPrefsManager.customPrefs[MONITORING_INTERVAL] = valueList[which]
      startWorkManager(this, true)
      updateIntervalTimeOnUi()
      dialog.dismiss()
    }
    alertBuilder.setNegativeButton(getString(R.string.cancel), null)
    val dialog = alertBuilder.create()
    dialog.show()
  }

  private fun updateIntervalTimeOnUi() {
    txtIntervalDetails.text = getMonitorTime()
  }
}