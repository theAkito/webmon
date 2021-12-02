package ooo.akito.webmon.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ooo.akito.webmon.utils.Constants.WEBSITE_ENTRY_TAG_CLOUD_DATA
import ooo.akito.webmon.utils.SharedPrefsManager.set

typealias jString = String

const val nameAppCasePascal = "Webmon"
const val nameAppCaseLower = "webmon"
const val nameTorApp = "Orbot"
const val nameCmdLogcat = "logcat"
const val msgGenericSuccess = "Success"
const val msgGenericAvailable = "Available"
const val msgGenericDismiss = "Dismiss"
const val msgGenericDefault = "default"
const val workaroundRebirthMillis = 2000L
const val amountMaxCharsInNameTag = 16
const val swipeRefreshTriggerDistanceLong = 600

val defaultTitleNimHomepage: String by lazy { "Nim Homepage" }
val defaultUrlNimHomepage: String by lazy { "https://nim-lang.org/" }
val defaultTitleUnavailableWebsite: String by lazy { "Unavailable Website" }
val defaultUrlUnavailableWebsite: String by lazy { "https://error.duckduckgo.com/" }
val msgGenericRestarting: String by lazy { "Restarting!" }
val defaultShareType: String by lazy { "text/plain" }
val nameNoneCaseLower: String by lazy { "none" }
val nameBackupDataCaseLower: String by lazy { "data" }
val nameBackupSettingsCaseLower: String by lazy { "settings" }
val nameBackupDataCasePascal: String by lazy { "Data" }
val nameBackupSettingsCasePascal: String by lazy { "Settings" }
val logDivider: String by lazy { "************************************************************" }

val lineEnd: String = System.lineSeparator()
var totalAmountEntry = 0
var torIsEnabled = false
var torAppIsAvailable = false
var swipeRefreshIsEnabled = true
var swipeRefreshTriggerDistanceLongIsEnabled = false
var forcedBackgroundServiceEnabled = false
var logEnabled = false
var replaceFabWithMenuEntryEnabled = false
var isEntryCreated = false /** Do not observe and notify about "unavailable" Website, just because it is freshly added and seems "unavailable", when it isn't. */
var logContent = ""

var globalEntryTagsNames: List<String> = listOf(msgGenericDefault)
  set(value) {
    val readyValue = value.distinct().sorted()
    field = readyValue
    SharedPrefsManager.customPrefs[WEBSITE_ENTRY_TAG_CLOUD_DATA] = mapperUgly.writeValueAsString(readyValue)
  }

val mapper: ObjectMapper = jacksonObjectMapper()
  .enable(SerializationFeature.INDENT_OUTPUT) /* Always pretty-print. */
val mapperUgly: ObjectMapper = jacksonObjectMapper()
  .disable(SerializationFeature.INDENT_OUTPUT) /* Never pretty-print. */