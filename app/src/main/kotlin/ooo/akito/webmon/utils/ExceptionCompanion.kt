package ooo.akito.webmon.utils

import org.minidns.dnsmessage.DnsMessage


object ExceptionCompanion {

  val connCodeGenericFail: Int by lazy {
    0
  }
  val connCodeNXDOMAIN: Int by lazy {
    DnsMessage.RESPONSE_CODE.NX_DOMAIN.value.toInt()
  }
  val connCodeTorFail: Int by lazy {
    9404
  }
  val connCodeTorAppUnavailable: Int by lazy {
    9405
  }
  val connCodeTorConnFailed: Int by lazy {
    9409
  }

  val msgGenericFailure: String by lazy {
    "Failure"
  }
  val msgGenericUnknown: String by lazy {
    "Unknown"
  }

  val msgNullNotNull: String by lazy {
    """This variable is null, even though it cannot be null."""
  }
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
  val msgParseBackupDataFail: String by lazy {
    "Could not parse Backup Website Entries File!"
  }
  val msgParseBackupSettingsFail: String by lazy {
    "Could not parse Backup Settings File!"
  }
  val msgCannotOpenOutputStreamBackupWebsiteEntries: String by lazy {
    "Cannot open output stream when trying to write Backup Website Entries File!"
  }
  val msgCannotOpenOutputStreamBackupSettings: String by lazy {
    "Cannot open output stream when trying to write Backup Settings File!"
  }
  val msgCannotGenerateBackupFileContent: String by lazy {
    "Cannot generate BackupFileContent!"
  }
  val msgGenericTorFailure: String by lazy {
    "TOR Failure!"
  }
  val msgTorIsNotInstalled: String by lazy {
    "${nameTorApp} is not installed!"
  }
  val msgTorIsEnabledButNotAvailable: String by lazy {
    "TOR is enabled but Orbot App is not available!"
  }
  val msgCannotConnectToTor: String by lazy {
    "Cannot connect to TOR!"
  }
  val msgCannotOpenOnionInBrowser: String by lazy {
    "Cannot open Onion address in clearnet browser!"
  }
  val msgSpecificToRebirth: String by lazy {
    "Could not initialise Looper for triggering Rebirth!"
  }
  val msgDnsOnlyNXDOMAIN: String by lazy {
    "All DNS responses are NX_DOMAIN!"
  }
  val msgMiniNXDOMAIN: String by lazy {
    "NX_DOMAIN"
  }
  val msgCannotExtractHostOrPortFromUrl: String by lazy {
    """Could not extract Host or Port from URL!"""
  }
  val msgCannotExtractHostFromUrl: String by lazy {
    """Unable to extract Host from URL!"""
  }
  val msgCannotExtractPortFromUrl: String by lazy {
    """Unable to extract Port from URL!"""
  }
  val msgCannotConnectToTCP: String by lazy {
    "Cannot connect to TCP server!"
  }
  val msgCannotConnectToSMTP: String by lazy {
    "Cannot connect to SMTP server!"
  }
  val msgCannotConnectToIMAP: String by lazy {
    "Cannot connect to IMAP server!"
  }
  val msgWebsiteEntriesUnavailable: String by lazy {
    "WebsiteEntries must be available!"
  }
  val msgNotImplemented: String by lazy {
    """Oops! This feature is not implemented, yet."""
  }
  val msgGlideLoadIconFailure: String by lazy {
    """Exception occurred when using Glide to load Website Logo."""
  }

  val msgDnsRootDomain: String by lazy {
    " Root Domain: "
  }
  val msgErrorTryingToFetchData: String by lazy {
    "Error when trying to fetch data: "
  }
  val msgFileContent: String by lazy {
    "File content: "
  }
}