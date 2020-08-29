package com.manimarank.websitemonitor.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.utils.Constants.MONITORING_INTERVAL
import com.manimarank.websitemonitor.utils.Interval.nameList
import com.manimarank.websitemonitor.utils.Interval.valueList
import com.manimarank.websitemonitor.utils.SharedPrefsManager
import com.manimarank.websitemonitor.utils.SharedPrefsManager.set
import com.manimarank.websitemonitor.utils.Utils.getMonitorTime
import com.manimarank.websitemonitor.utils.Utils.isCustomRom
import com.manimarank.websitemonitor.utils.Utils.openAutoStartScreen
import com.manimarank.websitemonitor.utils.Utils.startWorkManager
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnMonitorInterval.setOnClickListener { showIntervalChooseDialog() }

        layoutEnableAutoStart.visibility = if (isCustomRom()) View.VISIBLE else View.GONE
        btnEnableAutoStart.setOnClickListener { openAutoStartScreen(this) }

        updateIntervalTimeOnUi()
    }

    private fun showIntervalChooseDialog() {

        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setTitle(getString(R.string.choose_interval))

        val checkedItem =
            valueList.indexOf(SharedPrefsManager.customPrefs.getInt(MONITORING_INTERVAL, 60))
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