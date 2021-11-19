package ooo.akito.webmon.data.model

data class BackupSettings(
  val version: Int, /* Different model versions will require different import methods. */
  val timestamp: String, /* Date & Time of when backup was saved. */
  val locationSaved: String, /* Absolute path to directory, where backup was saved to. */
  var backup_last_saved_location: String,
  var website_entry_tag_cloud_data: String,
  var hide_is_onion_address: Boolean,
  var settings_tor_enable: Boolean,
  var settings_toggle_swipe_refresh: Boolean,
  var is_added_default_data: Boolean,
  var monitoring_interval: Int,
  var is_scheduled: Boolean,
  var notify_only_server_issues: Boolean,
  var settings_toggle_log: Boolean,
  var settings_toggle_forced_background_service: Boolean
)
