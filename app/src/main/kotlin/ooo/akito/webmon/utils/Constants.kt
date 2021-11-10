package ooo.akito.webmon.utils

object Constants {
  /**
    If you add further global settings, you need to add them to `BackupSettingsManager`, as well!
  */

  const val permissionReadExternalStorage = android.Manifest.permission.READ_EXTERNAL_STORAGE
  const val requestCodeReadExternalStorage = 1 /* Request Code is chosen arbitrarily. */
  const val orbotFQID = "org.torproject.android"

  const val INTENT_OBJECT = "intent_object"
  const val INTENT_CREATE_ENTRY = 1
  const val INTENT_UPDATE_ENTRY = 2
  const val DEFAULT_INTERVAL_MIN = 60

  const val IS_SCHEDULED: String = "is_scheduled"
  const val TAG_GLOBAL: String = "Webmon ##--> "
  const val TAG_WORK_MANAGER: String = "MainWorkManager"

  const val NOTIFICATION_CHANNEL_ID = "WEB_SITE_MONITOR_CHANNEL_ID"
  const val NOTIFICATION_CHANNEL_NAME = "Web Site Monitor"
  const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notification channel for monitoring web sites. In case of any site failed then showing notification."

  const val IS_ADDED_DEFAULT_DATA: String = "is_added_default_data"
  const val MONITORING_INTERVAL: String = "monitoring_interval"
  const val IS_AUTO_START_SHOWN : String = "is_auto_start_shown"
  const val NOTIFY_ONLY_SERVER_ISSUES : String = "notify_only_server_issues"
  const val SETTINGS_TOR_ENABLE : String = "settings_tor_enable"
  const val SETTINGS_AVAILABILITY_LAISSEZFAIRE: String = "settings_availability_laissezfaire"
  const val SETTINGS_TOGGLE_SWIPE_REFRESH: String = "settings_toggle_swipe_refresh"

  const val HIDE_IS_ONION_ADDRESS: String = "hide_is_onion_address"
  const val HIDE_CHECK_DNS_RECORD_A_AAAA: String = "hide_check_dns_record_a_aaaa"

}