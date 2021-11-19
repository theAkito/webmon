package ooo.akito.webmon.utils

import ooo.akito.webmon.data.metadata.BackupEnvironment
import ooo.akito.webmon.data.model.BackupSettings
import ooo.akito.webmon.utils.Constants.BACKUP_LAST_SAVED_LOCATION
import ooo.akito.webmon.utils.Constants.DEFAULT_INTERVAL_MIN
import ooo.akito.webmon.utils.Constants.HIDE_IS_ONION_ADDRESS
import ooo.akito.webmon.utils.Constants.IS_ADDED_DEFAULT_DATA
import ooo.akito.webmon.utils.Constants.IS_SCHEDULED
import ooo.akito.webmon.utils.Constants.MONITORING_INTERVAL
import ooo.akito.webmon.utils.Constants.NOTIFY_ONLY_SERVER_ISSUES
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_LOG
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_SWIPE_REFRESH
import ooo.akito.webmon.utils.Constants.SETTINGS_TOR_ENABLE
import ooo.akito.webmon.utils.Constants.WEBSITE_ENTRY_TAG_CLOUD_DATA
import ooo.akito.webmon.utils.Constants.defaultJArrayAsString
import ooo.akito.webmon.utils.SharedPrefsManager.customPrefs

class BackupSettingsManager {

  private fun generateBackupSettings(locationSave: String): BackupSettings =
    with(customPrefs) {
      BackupSettings(
        BackupEnvironment.defaultBackupSettingsVersion,
        Environment.getDefaultDateTimeString(),
        locationSaved = locationSave,
        backup_last_saved_location = getString(BACKUP_LAST_SAVED_LOCATION, "") ?: "",
        website_entry_tag_cloud_data = getString(WEBSITE_ENTRY_TAG_CLOUD_DATA, defaultJArrayAsString) ?: defaultJArrayAsString,
        hide_is_onion_address = getBoolean(HIDE_IS_ONION_ADDRESS, false),
        settings_tor_enable = getBoolean(SETTINGS_TOR_ENABLE, false),
        settings_toggle_swipe_refresh = getBoolean(SETTINGS_TOGGLE_SWIPE_REFRESH, true),
        is_added_default_data = getBoolean(IS_ADDED_DEFAULT_DATA, false),
        monitoring_interval = getInt(MONITORING_INTERVAL, DEFAULT_INTERVAL_MIN),
        is_scheduled = getBoolean(IS_SCHEDULED, false),
        notify_only_server_issues = getBoolean(NOTIFY_ONLY_SERVER_ISSUES, false),
        settings_toggle_log = getBoolean(SETTINGS_TOGGLE_LOG, false),
        settings_toggle_forced_background_service = getBoolean(SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE, false)
      )
    }

  private fun generateBackupWebsitesJString(locationSave: String): jString = mapper.writeValueAsString(generateBackupSettings(locationSave))
  fun generateBackupFileContent(backupFilePathRelative: String) = generateBackupWebsitesJString(backupFilePathRelative)

}