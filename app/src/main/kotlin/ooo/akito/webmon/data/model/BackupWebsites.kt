package ooo.akito.webmon.data.model

import ooo.akito.webmon.data.db.WebSiteEntry

data class BackupWebsites(
  val version: Int, /* Different model versions will require different import methods. */
  val timestamp: String, /* Date & Time of when backup was saved. */
  val locationSaved: String, /* Absolute path to directory, where backup was saved to. */
  val entries: List<WebSiteEntry>
)
