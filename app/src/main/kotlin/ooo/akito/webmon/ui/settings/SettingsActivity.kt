package ooo.akito.webmon.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.os.HandlerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.environment.LogOS
import ooo.akito.webmon.data.metadata.BackupEnvironment.defaultBackupWebsitesVersion
import ooo.akito.webmon.data.model.BackupSettings
import ooo.akito.webmon.data.model.BackupWebsites
import ooo.akito.webmon.data.viewmodels.MainViewModel
import ooo.akito.webmon.databinding.ActivitySettingsBinding
import ooo.akito.webmon.ui.debug.ActivityDebug
import ooo.akito.webmon.ui.home.MainActivity.Companion.fileTypeFilter
import ooo.akito.webmon.utils.BackgroundCheckInterval.nameList
import ooo.akito.webmon.utils.BackgroundCheckInterval.valueList
import ooo.akito.webmon.utils.BackupSettingsManager
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
import ooo.akito.webmon.utils.Constants.permissionReadExternalStorage
import ooo.akito.webmon.utils.Constants.requestCodeReadExternalStorage
import ooo.akito.webmon.utils.Environment.getDefaultDateTimeString
import ooo.akito.webmon.utils.Environment.getDefaultPathCacheBackup
import ooo.akito.webmon.utils.Environment.getDefaultPathCacheLog
import ooo.akito.webmon.utils.Environment.getNameFileBackup
import ooo.akito.webmon.utils.Environment.getNameFileLog
import ooo.akito.webmon.utils.ExceptionCompanion
import ooo.akito.webmon.utils.ExceptionCompanion.msgBackupUriPathInvalid
import ooo.akito.webmon.utils.ExceptionCompanion.msgCannotOpenOutputStreamBackupSettings
import ooo.akito.webmon.utils.ExceptionCompanion.msgCannotOpenOutputStreamBackupWebsiteEntries
import ooo.akito.webmon.utils.ExceptionCompanion.msgFileContent
import ooo.akito.webmon.utils.ExceptionCompanion.msgInputStreamNullBackupInterrupted
import ooo.akito.webmon.utils.ExceptionCompanion.msgSpecificToRebirth
import ooo.akito.webmon.utils.Log
import ooo.akito.webmon.utils.SharedPrefsManager.customPrefs
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.cleanCustomTags
import ooo.akito.webmon.utils.Utils.getMonitorTime
import ooo.akito.webmon.utils.Utils.isCustomRom
import ooo.akito.webmon.utils.Utils.openAutoStartScreen
import ooo.akito.webmon.utils.Utils.triggerRebirth
import ooo.akito.webmon.utils.defaultShareType
import ooo.akito.webmon.utils.forcedBackgroundServiceEnabled
import ooo.akito.webmon.utils.globalEntryTagsNames
import ooo.akito.webmon.utils.jString
import ooo.akito.webmon.utils.logEnabled
import ooo.akito.webmon.utils.mapper
import ooo.akito.webmon.utils.msgGenericDefault
import ooo.akito.webmon.utils.msgGenericRestarting
import ooo.akito.webmon.utils.nameBackupDataCaseLower
import ooo.akito.webmon.utils.nameBackupDataCasePascal
import ooo.akito.webmon.utils.nameBackupSettingsCaseLower
import ooo.akito.webmon.utils.nameBackupSettingsCasePascal
import ooo.akito.webmon.utils.nameNoneCaseLower
import ooo.akito.webmon.utils.swipeRefreshIsEnabled
import ooo.akito.webmon.utils.workaroundRebirthMillis
import java.io.File
import java.nio.charset.Charset


class SettingsActivity : AppCompatActivity() {

  private lateinit var viewModel: MainViewModel
  private lateinit var activitySettingsBinding: ActivitySettingsBinding
  private lateinit var btnMonitorInterval: LinearLayout
  private lateinit var layoutEnableAutoStart: LinearLayout
  private lateinit var btnEnableAutoStart: TextView
  private lateinit var switchNotifyOnlyServerIssues: SwitchMaterial
  private lateinit var switchSettingsTorEnable: SwitchMaterial
  private lateinit var txtIntervalDetails: AppCompatTextView

  private lateinit var onBackupWebsiteEntriesResultLauncher: ActivityResultLauncher<String>
  private lateinit var onRestoreWebsiteEntriesResultLauncher: ActivityResultLauncher<String>
  private lateinit var onBackupSettingsResultLauncher: ActivityResultLauncher<String>
  private lateinit var onRestoreSettingsResultLauncher: ActivityResultLauncher<String>

  private var websites: List<WebSiteEntry> = listOf()

  private val backupSettingsManager: BackupSettingsManager by lazy { BackupSettingsManager() }

  private val logOS: LogOS by lazy { LogOS() }


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

  private fun generateBackupWebsites(locationSave: String): BackupWebsites {
      return BackupWebsites(
      version = defaultBackupWebsitesVersion,
      timestamp = getDefaultDateTimeString(),
      locationSaved = locationSave,
      entries = websites
    )
  }

  private fun warnOfBackupImportParsingFailure(backupType: String) {
    AlertDialog.Builder(this).apply {
      setTitle("""Failed to restore ${backupType} Backup from File!""")
      setMessage("""When trying to load the ${backupType} Backup from File, an error was encountered. Are you sure you selected the correct file?""")
      setNeutralButton(R.string.text_backup_import_failure_confirmation, null)
      setCancelable(false)
    }.create().show()
  }

  private fun generateBackupWebsitesJString(locationSave: String): jString = mapper.writeValueAsString(generateBackupWebsites(locationSave))
  private fun permissionIsGranted(permission: String = permissionReadExternalStorage): Boolean = ActivityCompat.checkSelfPermission(this@SettingsActivity, permission) == PackageManager.PERMISSION_GRANTED
  private fun permissionIsDenied(permission: String = permissionReadExternalStorage): Boolean = ActivityCompat.checkSelfPermission(this@SettingsActivity, permission) == PackageManager.PERMISSION_DENIED

  private fun shareButtonAction(
    resStringTitleShare: Int,
    fileName: String,
    fileDir: File,
    fileContent: String
  ) {
    val activity = this@SettingsActivity
    val file = File(fileDir.absolutePath, fileName)
    file.writeText(fileContent)
    val uri = FileProvider.getUriForFile(activity, packageName, file)
    val shareIntent = Intent().apply {
      type = defaultShareType
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_TITLE, fileName)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val destinations = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
    destinations.forEach { info ->
      val packageName = info.packageName
      activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, resources.getText(resStringTitleShare)))
  }

  private fun logcatShareButtonSetOnClickListener() {
    shareButtonAction(
      R.string.title_share_logcat,
      getNameFileLog(),
      getDefaultPathCacheLog(),
      logOS.readLogcat().toString(Charset.defaultCharset())
    )
  }

  private fun backupShareButtonAction(backupType: String, backupFileContent: String) {
    shareButtonAction(
      R.string.title_share_backup,
      getNameFileBackup(backupType),
      getDefaultPathCacheBackup(),
      backupFileContent
    )
  }

  private fun Button.backupImportButtonAction(backupType: String) {
    setOnClickListener {
      /*
        At some point, this will open a new activity instead,
        where the user can read more information about the backup and customise it.
      */
      if (
        permissionIsDenied() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
      ) {
        ActivityCompat.requestPermissions(this@SettingsActivity, arrayOf(permissionReadExternalStorage),
          requestCodeReadExternalStorage
        )
      } else {
        when (backupType.lowercase()) {
          nameBackupDataCaseLower -> onRestoreWebsiteEntriesResultLauncher.launch(fileTypeFilter)
          nameBackupSettingsCaseLower -> onRestoreSettingsResultLauncher.launch(fileTypeFilter)
          else -> throw IllegalArgumentException("[SettingsActivity.generateBackupDataExportLauncher()] Cannot generate BackupFileContent!")
        }
      }
    }
  }

  private fun generateBackupDataExportLauncher(backupType: String, illegalAccessErrorMsg: String): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
      fun logErr() = Log.error(msgBackupUriPathInvalid)
      if (uri == null) {
        logErr()
        return@registerForActivityResult
      }
      val backupFilePathRelative = try {
        uri.path
      } catch (e: Exception) {
        logErr()
        return@registerForActivityResult
      }
      if (backupFilePathRelative == null) {
        logErr()
        return@registerForActivityResult
      }
      /* On next backup, the same destination folder will be opened. */
      customPrefs[BACKUP_LAST_SAVED_LOCATION] = uri.toString()
      val backupFileContent = when (backupType.lowercase()) {
        nameBackupDataCaseLower -> generateBackupWebsitesJString(backupFilePathRelative)
        nameBackupSettingsCaseLower -> backupSettingsManager.generateBackupFileContent(backupFilePathRelative)
        else -> throw IllegalArgumentException("[SettingsActivity.generateBackupDataExportLauncher()] Cannot generate BackupFileContent!")
      }
      val resolver = this@SettingsActivity.contentResolver

      /** https://stackoverflow.com/a/64733499/7061105 */
      val out = resolver.openOutputStream(uri) ?: throw IllegalAccessError(illegalAccessErrorMsg)
      out.use { stream ->
        Log.info("Writing ${backupType} as Backup...")
        Log.info("${backupType}: " + backupFileContent)
        stream.write(backupFileContent.toByteArray())
        stream.flush()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Setting up ViewModel and LiveData
    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    with(viewModel) {
      /*
        This MUST NOT be in its own proc!
        Gets also triggered, when using the UI for switching to MainActivity.
      */
      getWebSiteEntryList().observe(
        this@SettingsActivity, { observedWebsites ->
          websites = observedWebsites
        }
      )
    }

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

    switchNotifyOnlyServerIssues.isChecked = customPrefs.getBoolean(NOTIFY_ONLY_SERVER_ISSUES, false)
    switchNotifyOnlyServerIssues.setOnCheckedChangeListener { _, isChecked ->
      customPrefs[NOTIFY_ONLY_SERVER_ISSUES] = isChecked
    }

    switchSettingsTorEnable.isChecked = customPrefs.getBoolean(SETTINGS_TOR_ENABLE, false)
    switchSettingsTorEnable.setOnCheckedChangeListener { _, isChecked ->
      customPrefs[SETTINGS_TOR_ENABLE] = isChecked
      customPrefs[HIDE_IS_ONION_ADDRESS] = false
      Log.warn("TOR switched to ${isChecked}!")
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
          websites.forEach { website ->
            viewModel.deleteWebSiteEntry(website)
          }
        }
        setNegativeButton(R.string.text_delete_all_website_entries_are_you_sure_no) { _, _ -> }
      }.create().show()
    }

    //endregion Advanced Setting: Delete All Website Entries

    //region Advanced Setting: Delete All Website Entry Tags

    activitySettingsBinding.btnWebsiteEntryTagCloudDeleteAll.setOnClickListener {
      AlertDialog.Builder(this).apply {
        setMessage(R.string.text_delete_all_website_entry_tag_cloud_are_you_sure)
        setTitle(R.string.text_delete_all_website_entry_tag_cloud)
        setPositiveButton(
          R.string.text_delete_all_website_entries_are_you_sure_yes
        ) { _, _ ->
          globalEntryTagsNames = listOf(msgGenericDefault)
          websites
            .cleanCustomTags()
            .forEach { viewModel.saveWebSiteEntry(it) }
        }
        setNegativeButton(R.string.text_delete_all_website_entries_are_you_sure_no) { _, _ -> }
      }.create().show()
    }

    //endregion Advanced Setting: Delete All Website Entry Tags

    //region Advanced Setting: Toggle SwipeRefresh

    activitySettingsBinding.toggleSwipeRefresh.isChecked = swipeRefreshIsEnabled
    activitySettingsBinding.toggleSwipeRefresh.setOnCheckedChangeListener { _, isActivated ->
      isActivated.let {
        Log.warn("SwipeRefresh switched to ${it}!")
        customPrefs[SETTINGS_TOGGLE_SWIPE_REFRESH] = it
        activitySettingsBinding.toggleSwipeRefresh.isChecked = it
      }
    }

    //endregion Advanced Setting: Toggle SwipeRefresh

    //region Advanced Setting: Toggle Forced Background Service

    activitySettingsBinding.toggleServiceForce.isChecked = forcedBackgroundServiceEnabled
    activitySettingsBinding.toggleServiceForce.setOnCheckedChangeListener { _, isActivated ->
      isActivated.let {
        Log.warn("Forced Background Service switched to ${it}!")
        customPrefs[SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE] = it
        activitySettingsBinding.toggleServiceForce.isChecked = it
      }
      restartApp()
    }

    //endregion Advanced Setting: Toggle Forced Background Service

    //region Advanced Setting: Toggle Log

    activitySettingsBinding.toggleLog.isChecked = logEnabled
    activitySettingsBinding.toggleLog.setOnCheckedChangeListener { _, isActivated ->
      isActivated.let {
        Log.warn("Log switched to ${it}!")
        customPrefs[SETTINGS_TOGGLE_LOG] = it
        activitySettingsBinding.toggleLog.isChecked = it
      }
      restartApp()
    }

    //endregion Advanced Setting: Toggle Log

    //region Advanced Setting: Export Backup of Data

    /** Backup Website Entries */
    onBackupWebsiteEntriesResultLauncher = generateBackupDataExportLauncher(
      backupType = nameBackupDataCasePascal,
      illegalAccessErrorMsg = msgCannotOpenOutputStreamBackupWebsiteEntries
    )

    activitySettingsBinding.btnBackupDataExport.setOnClickListener {
      /*
        At some point, this will open a new activity instead,
        where the user can read more information about the backup and customise it.
      */
      onBackupWebsiteEntriesResultLauncher.launch("backup-webmon-${nameBackupDataCaseLower}_${getDefaultDateTimeString()}.json")
    }

    //endregion Advanced Setting: Export Backup of Data

    //region Advanced Setting: Import Backup of Data

    onRestoreWebsiteEntriesResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { rawUri ->
      fun logErr() = Log.error(msgInputStreamNullBackupInterrupted)
      val uri = rawUri ?: return@registerForActivityResult
      val resolver = this@SettingsActivity.contentResolver
      val input = try {
        resolver.openInputStream(uri)
      } catch (e: Exception) {
        logErr()
        return@registerForActivityResult
      }
      if (input == null) {
        logErr()
        return@registerForActivityResult
      }
      val rawContent: ByteArray = input.use { it.readBytes() }
      val backupWebsites = try {
        mapper.readValue<BackupWebsites>(rawContent)
      } catch (e: Exception) {
        Log.error(msgFileContent + rawContent)
        warnOfBackupImportParsingFailure("Data")
        return@registerForActivityResult
      }
      val providedWebsites = backupWebsites.entries
      /**
        Do not import Websites that are already available.
        Filtered by Website URL.
      */
      val newWebsites = providedWebsites.filterNot { provided -> websites.any { it.url == provided.url } }
      Log.info("Restoring WebsiteEntry List from Backup...")
      newWebsites.forEach { website ->
        Log.info("New WebsiteEntry: " + website)
        /* Avoids `UNIQUE constraint failed: web_site_entry.id (code 1555 SQLITE_CONSTRAINT_PRIMARYKEY)`. */
        /* WebsiteEntry Glue */
        val cleanedWebsite = WebSiteEntry(
          name = website.name,
          url = website.url,
          itemPosition = website.itemPosition,
          isLaissezFaire = website.isLaissezFaire,
          dnsRecordsAAAAA = website.dnsRecordsAAAAA,
          isOnionAddress = website.isOnionAddress
        )
        viewModel.saveWebSiteEntry(cleanedWebsite)
      }
      Log.info("Finished restoring WebsiteEntry List from Backup!")
    }

    activitySettingsBinding.btnBackupDataImport.backupImportButtonAction(nameBackupDataCaseLower)

    //endregion Advanced Setting: Import Backup of Data

    //region Advanced Setting: Export Backup of Settings

    /** Backup App Settings */
    onBackupSettingsResultLauncher = generateBackupDataExportLauncher(
      backupType = nameBackupSettingsCasePascal,
      illegalAccessErrorMsg = msgCannotOpenOutputStreamBackupSettings
    )

    activitySettingsBinding.btnBackupSettingsExport.setOnClickListener {
      /*
        At some point, this will open a new activity instead,
        where the user can read more information about the backup and customise it.
      */
      onBackupSettingsResultLauncher.launch("backup-webmon-${nameBackupSettingsCaseLower}_${getDefaultDateTimeString()}.json")
    }

    //endregion Advanced Setting: Export Backup of Settings

    //region Advanced Setting: Import Backup of Settings

    onRestoreSettingsResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      fun logErr() = Log.error(ExceptionCompanion.msgInputStreamNullBackupInterrupted)
      val resolver = this@SettingsActivity.contentResolver
      if (uri == null) {
        logErr()
        return@registerForActivityResult
      }
      val input = try {
        resolver.openInputStream(uri)
      } catch (e: Exception) {
        logErr()
        return@registerForActivityResult
      }
      if (input == null) {
        logErr()
        return@registerForActivityResult
      }
      val rawContent: ByteArray = input.use { it.readBytes() }
      val providedSettings = try {
        mapper.readValue<BackupSettings>(rawContent)
      } catch (e: Exception) {
        Log.error(ExceptionCompanion.msgFileContent + rawContent)
        warnOfBackupImportParsingFailure("Settings")
        return@registerForActivityResult
      }
      Log.info("Restoring Settings from Backup...")
      with(customPrefs) {
        this[WEBSITE_ENTRY_TAG_CLOUD_DATA] = providedSettings.website_entry_tag_cloud_data
        this[BACKUP_LAST_SAVED_LOCATION] = providedSettings.backup_last_saved_location
        this[HIDE_IS_ONION_ADDRESS] = providedSettings.hide_is_onion_address
        this[SETTINGS_TOR_ENABLE] = providedSettings.settings_tor_enable
        this[SETTINGS_TOGGLE_SWIPE_REFRESH] = providedSettings.settings_toggle_swipe_refresh
        this[IS_ADDED_DEFAULT_DATA] = providedSettings.is_added_default_data
        this[MONITORING_INTERVAL] = providedSettings.monitoring_interval
        this[IS_SCHEDULED] = providedSettings.is_scheduled
        this[NOTIFY_ONLY_SERVER_ISSUES] = providedSettings.notify_only_server_issues
        this[SETTINGS_TOGGLE_LOG] = providedSettings.settings_toggle_log
        this[SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE] = providedSettings.settings_toggle_forced_background_service
      }
      Log.info("Finished restoring Settings from Backup!")
    }

    activitySettingsBinding.btnBackupSettingsImport.backupImportButtonAction(nameBackupSettingsCaseLower)

    //endregion Advanced Setting: Import Backup of Settings

    //region Advanced Setting: Share Backup of Data

    /** Share Backup Website Entries */
    activitySettingsBinding.btnBackupDataShare.setOnClickListener {
      backupShareButtonAction(
        nameBackupDataCaseLower,
        generateBackupWebsitesJString(nameNoneCaseLower)
      )
    }

    //endregion Advanced Setting: Share Backup of Data

    //region Advanced Setting: Share Backup of Settings

    /** Share Backup Settings */
    activitySettingsBinding.btnBackupSettingsShare.setOnClickListener {
      backupShareButtonAction(
        nameBackupSettingsCaseLower,
        backupSettingsManager.generateBackupFileContent(nameNoneCaseLower)
      )
    }

    //endregion Advanced Setting: Share Backup of Settings

    //region Advanced Setting: Show Log

    /** UI Divider (Cosmetic) */
    activitySettingsBinding.dividerZoneEndBackupSettingsShare.apply {
      visibility = if (logEnabled) {
        View.VISIBLE
      } else {
        View.GONE
      }
    }

    /** Show Log*/
    activitySettingsBinding.btnLogShow.apply {
      visibility = if (logEnabled) {
        setOnClickListener {
          startActivity(Intent(applicationContext, ActivityDebug::class.java))
        }
        View.VISIBLE
      } else {
        View.GONE
      }
    }

    //endregion Advanced Setting: Show Log

    //region Advanced Setting: Share Log

    /** Share Log */
    activitySettingsBinding.btnLogShare.apply {
      visibility = if (logEnabled) {
        setOnClickListener { logcatShareButtonSetOnClickListener() }
        View.VISIBLE
      } else {
        View.GONE
      }
    }

    //endregion Advanced Setting: Share Log

    //endregion Advanced Settings
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (permissionIsGranted()) {
      onRestoreWebsiteEntriesResultLauncher.launch(fileTypeFilter)
    }
  }

  private fun showIntervalChooseDialog() {
    AlertDialog.Builder(this).apply {
      val checkedItem = valueList.indexOf(customPrefs.getInt(MONITORING_INTERVAL, DEFAULT_INTERVAL_MIN))
      setTitle(getString(R.string.choose_interval))
      setSingleChoiceItems(
        nameList,
        checkedItem
      ) { dialog: DialogInterface, chosenIntervalPosition: Int ->
        customPrefs[MONITORING_INTERVAL] = valueList[chosenIntervalPosition]
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