package ooo.akito.webmon.utils

object ExceptionMessages {
  val msgWebsitesNotReachable: String by lazy {
    "Several Websites are not reachable!"
  }
  val msgInternetUnavailable: String by lazy {
    "Internet unavailable!"
  }
  val msgUriProvidedIsNull: String by lazy {
    "URI provided is null!"
  }
  val msgCannotGetWebsiteEntryListValue: String by lazy {
    "Cannot get WebSiteEntryList from LiveData!"
  }
  val msgBackupUriPathInvalid: String by lazy {
    "Backup URI does not provide a valid path!"
  }
  val msgInputStreamNullBackupInterrupted: String by lazy {
    "InputStream is null! The Backup Action was probably interrupted."
  }
  val msgParseBackupFail: String by lazy {
    "Could not parse Backup Website Entries File!"
  }

  val msgErrorTryingToFetchData: String by lazy {
    "Error when trying to fetch data: "
  }
  val msgFileContent: String by lazy {
    "File content: "
  }
}