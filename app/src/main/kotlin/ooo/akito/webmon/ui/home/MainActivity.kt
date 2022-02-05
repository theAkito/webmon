package ooo.akito.webmon.ui.home

import android.app.Activity
import android.app.Dialog
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.*
import android.text.TextUtils
import android.view.*
import android.view.DragEvent.*
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.CustomMonitorData
import ooo.akito.webmon.data.viewmodels.MainViewModel
import ooo.akito.webmon.data.viewmodels.ViewModelLogCat
import ooo.akito.webmon.databinding.ActivityMainBinding
import ooo.akito.webmon.databinding.CustomRefreshInputBinding
import ooo.akito.webmon.net.Utils.isConnected
import ooo.akito.webmon.ui.createentry.CreateEntryActivity
import ooo.akito.webmon.ui.debug.ActivityDebug
import ooo.akito.webmon.ui.settings.SettingsActivity
import ooo.akito.webmon.utils.*
import ooo.akito.webmon.utils.AppService
import ooo.akito.webmon.utils.Constants.IS_INIT
import ooo.akito.webmon.utils.Constants.ONESHOT_FAB_DEFAULT_POSITION_IS_SAVED
import ooo.akito.webmon.utils.Constants.ONESHOT_FAB_POSITION_X
import ooo.akito.webmon.utils.Constants.ONESHOT_FAB_POSITION_Y
import ooo.akito.webmon.utils.Constants.SERVICE_IS_RUNNING
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_BACKUP_DATA_IMPORT_OVERWRITE_EXISTING
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_LOG
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_REPLACE_FAB_WITH_MENU_ENTRY
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_SWIPE_REFRESH
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_SWIPE_REFRESH_TRIGGER_DISTANCE_LONG
import ooo.akito.webmon.utils.Constants.SETTINGS_TOR_ENABLE
import ooo.akito.webmon.utils.Constants.WEBSITE_ENTRY_TAG_CLOUD_DATA
import ooo.akito.webmon.utils.Constants.defaultJArrayAsString
import ooo.akito.webmon.utils.Constants.orbotFQID
import ooo.akito.webmon.utils.Environment.defaultTimeFormat
import ooo.akito.webmon.utils.Environment.getCurrentLocale
import ooo.akito.webmon.utils.Environment.getDefaultDateTimeFormat
import ooo.akito.webmon.utils.Environment.locale
import ooo.akito.webmon.utils.ExceptionCompanion.msgCannotOpenOnionInBrowser
import ooo.akito.webmon.utils.ExceptionCompanion.msgInternetUnavailable
import ooo.akito.webmon.utils.ExceptionCompanion.msgUriProvidedIsNull
import ooo.akito.webmon.utils.ExceptionCompanion.msgWebsitesNotReachable
import ooo.akito.webmon.utils.SharedPrefsManager.customPrefs
import ooo.akito.webmon.utils.SharedPrefsManager.get
import ooo.akito.webmon.utils.SharedPrefsManager.set
import ooo.akito.webmon.utils.Utils.asUri
import ooo.akito.webmon.utils.Utils.forceStopSyncWorker
import ooo.akito.webmon.utils.Utils.getStringNotWorking
import ooo.akito.webmon.utils.Utils.joinToStringDescription
import ooo.akito.webmon.utils.Utils.mayNotifyStatusFailure
import ooo.akito.webmon.utils.Utils.openInBrowser
import ooo.akito.webmon.utils.Utils.packageIsInstalled
import ooo.akito.webmon.utils.Utils.safelyStartSyncWorker
import ooo.akito.webmon.utils.Utils.showNotification
import ooo.akito.webmon.utils.Utils.showSnackBar
import ooo.akito.webmon.utils.Utils.showSnackbarNotImplemented
import ooo.akito.webmon.utils.Utils.showToast
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), WebSiteEntryAdapter.WebSiteEntryEvents {

  companion object {
    val fileTypeFilter = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      /* Older Android versions do not support filtering for JSON... */
      """*/*"""
    } else {
      """application/json"""
    }
    /** https://stackoverflow.com/a/29145872/7061105 */
    private var refActivityDebug: WeakReference<ActivityDebug>? = null
    fun updateRefActivityDebug(activityDebug: ActivityDebug) {
      refActivityDebug = WeakReference<ActivityDebug>(activityDebug)
    }
  }

  //region Metadata

  private lateinit var viewModel: MainViewModel
  private lateinit var searchView: SearchView

  private lateinit var webSiteEntryAdapter: WebSiteEntryAdapter
  private lateinit var binding: ActivityMainBinding

  private lateinit var customRefreshInputBinding: CustomRefreshInputBinding

  private lateinit var onEditClickedResultLauncher: ActivityResultLauncher<Intent>
  private lateinit var onFabAddClickedResultLauncher: ActivityResultLauncher<Intent>

  private var handler = Handler(Looper.getMainLooper())

  private lateinit var service: AppService

  private var runningCount = 0
  private var customMonitorData: CustomMonitorData = CustomMonitorData()

  private lateinit var itemTouchHelper: ItemTouchHelper
  private lateinit var drawerToggle: ActionBarDrawerToggle

  //region Not in Use

  private var fabIsDraggedForFirstTime = true
  private var originalFabLocDX by Delegates.notNull<Float>()
  private var originalFabLocDY by Delegates.notNull<Float>()
  private var fabLocDX by Delegates.notNull<Float>()
  private var fabLocDY by Delegates.notNull<Float>()

  //endregion Not in Use

  //endregion Metadata

  //region Custom Methods

  private val runnableTask: Runnable = Runnable {
    if (runningCount == 0) {
      stopTask()
    } else {
      startUpdateTask(isUpdate = true)
    }
  }

  private fun resetSearchView() {
    if (!searchView.isIconified) {
      searchView.isIconified = true
      return
    }
  }

  private fun showForceRefreshUI() {
    val dialog = Dialog(this)
    dialog.setCancelable(true)
    if (customRefreshInputBinding.root.parent != null) {
      /*
        Make sure child does not already have parent.
        https://stackoverflow.com/a/52988517/7061105
      */
      (customRefreshInputBinding.root.parent as ViewGroup).removeView(customRefreshInputBinding.root)
    }
    dialog.setContentView(customRefreshInputBinding.root)

    dialog.run {
      customRefreshInputBinding.btnSave.setOnClickListener {
        if (isConnected(applicationContext).not()) {
          applicationContext.showToast(getString(R.string.check_internet))
          return@setOnClickListener
        }
        if (TextUtils.isEmpty(customRefreshInputBinding.editDuration.text).not()) {
          if (customRefreshInputBinding.checkboxAgree.isChecked) {
            val duration = customRefreshInputBinding.editDuration.text.toString().toLong()
            val durationBy = if (customRefreshInputBinding.rgDurationType.checkedRadioButtonId == R.id.rbDurationMin) {
              60 * 1000
            } else {
              1000
            }
            customMonitorData.apply {
              val durationType =
                if (customRefreshInputBinding.rgDurationType.checkedRadioButtonId == customRefreshInputBinding.rbDurationMin.id) {
                  customRefreshInputBinding.rbDurationMin.text
                } else {
                  customRefreshInputBinding.rbDurationSec.text
                }
              runningDelay = duration * durationBy
              runningDelayValue = "$duration " + durationType
              showNotification = customRefreshInputBinding.switchShowNotification.isChecked
            }

            startUpdateTask(isUpdate = false)
            dialog.dismiss()
          } else
            applicationContext.showToast(getString(R.string.error_read_and_agree_checkbox))
        } else
          applicationContext.showToast(getString(R.string.enter_valid_input))
      }
    }
    dialog.show()
  }

  private fun onFabAddClicked() {
    resetSearchView()
    totalAmountEntry = webSiteEntryAdapter.itemCount
    val intent = Intent(this, CreateEntryActivity::class.java)
    onFabAddClickedResultLauncher.launch(intent)
  }

  private fun setFabDefaultPosition() {
    /** Currently, not in use. */
    with (binding.fabAdd) {
      originalFabLocDX = x - width / 2
      originalFabLocDY = y - height / 2
    }
  }

  private fun makeFabRelocatable() {
    /** Currently, not in use. */
    val root = binding.root
    val fab = binding.fabAdd
    val fabDefaultPositionIsSaved = customPrefs.getBoolean(ONESHOT_FAB_DEFAULT_POSITION_IS_SAVED, false)
    root.setOnDragListener { view, event ->
      return@setOnDragListener when (event?.action) {
        ACTION_DRAG_STARTED -> {
          customPrefs[ONESHOT_FAB_DEFAULT_POSITION_IS_SAVED] = false
          with (event) {
            if (fabDefaultPositionIsSaved.not()) {
              customPrefs[ONESHOT_FAB_POSITION_X] = x - fab.width / 2
              customPrefs[ONESHOT_FAB_POSITION_Y] = y - fab.height / 2
              customPrefs[ONESHOT_FAB_DEFAULT_POSITION_IS_SAVED] = true
            }
          }
          true
        }
        ACTION_DRAG_LOCATION -> {
          with (event) {
            fabLocDX = x
            fabLocDY = y
          }
          true
        }
        ACTION_DRAG_ENDED -> {
          fab.apply {
//            x = fabLocDX - fab.width / 2
//            y = fabLocDY - fab.height / 2
            x = customPrefs[ONESHOT_FAB_POSITION_X, 1.0F] ?: 1.0F
            y = customPrefs[ONESHOT_FAB_POSITION_Y, 1.0F] ?: 1.0F
//            layoutParams.apply {
//              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                foregroundGravity = Gravity.CENTER
//              }
//            }
          }
          true
        }
        else -> true
      }
    }
    fab.apply {
      setOnLongClickListener {
        val shadow: View.DragShadowBuilder = View.DragShadowBuilder(fab)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          it.startDragAndDrop(null, shadow, null, View.DRAG_FLAG_GLOBAL)
        } else {
          TODO("VERSION.SDK_INT < N")
        }
      }
    }
  }

  private fun startUpdateTask(isUpdate: Boolean = true) {
    Log.info("Called on main thread $runningCount")
    binding.layout.layoutForceRefreshInfo.visibility = View.VISIBLE
    if (isUpdate) {
      handler.postDelayed(runnableTask, customMonitorData.runningDelay)
      viewModel.checkWebSiteStatus()
      binding.layout.txtForceRefreshInfo.text = getString(R.string.custom_monitor_running_info, customMonitorData.runningDelayValue, runningCount.toString())
      runningCount += 1
    } else{
      runningCount = 1
      handler.post(runnableTask)
    }
  }

  private fun stopTask() {
    handler.removeCallbacks(runnableTask)
    runningCount = 0
    customMonitorData = CustomMonitorData()
    binding.layout.layoutForceRefreshInfo.visibility = View.GONE
  }

  private fun String?.openInBrowser() {
    val uri = this.asUri()
    if (uri == null) {
      Log.error(msgUriProvidedIsNull)
      return
    }
    this@MainActivity.openInBrowser(uri)
  }

  private fun disableSwipeRefreshIsRefreshing() {
    if (binding.layout.swipeRefresh.isRefreshing || swipeRefreshIsEnabled.not()) {
      binding.layout.swipeRefresh.isRefreshing = false
    }
  }

  private fun handleInternetUnavailable(): Boolean {
    return if (isConnected(applicationContext).not()) {
      disableSwipeRefreshIsRefreshing()
      applicationContext.showToast(getString(R.string.check_internet))
      Log.error(msgInternetUnavailable)
      true
    } else {
      false
    }
  }

  private fun SwipeRefreshLayout.setOnRefreshListener() {
    setOnRefreshListener LISTENER@{
      disableSwipeRefreshIsRefreshing()
      if (isConnected(applicationContext).not()) {
        applicationContext.showToast(getString(R.string.check_internet))
        return@LISTENER
      }
      viewModel.checkWebSiteStatus()
    }
    if (swipeRefreshTriggerDistanceLongIsEnabled) {
      setDistanceToTriggerSync(swipeRefreshTriggerDistanceLong)
    }
  }

  private fun setAppServiceIsRunning() {
    val conn = object : ServiceConnection {
      override fun onServiceConnected(p0: ComponentName?, providedService: IBinder?) {
        val binder: AppService.AppServiceBinder = providedService as AppService.AppServiceBinder
        service = binder.getService()
        forcedBackgroundServiceRunning = service.workerIsRunning()
      }
      override fun onServiceDisconnected(p0: ComponentName?) {}
    }
    val intent = Intent(this, AppService::class.java)
    bindService(intent, conn, Context.BIND_AUTO_CREATE)
  }

  //endregion Custom Methods

  //region Overriden Methods

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setAppServiceIsRunning()

    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    locale = getCurrentLocale()
    defaultTimeFormat = locale.getDefaultDateTimeFormat()
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater)
    binding = ActivityMainBinding.inflate(layoutInflater)
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater, binding.root, false)
    globalEntryTagsNames = mapperUgly.readTree(customPrefs.getString(WEBSITE_ENTRY_TAG_CLOUD_DATA, defaultJArrayAsString)).asIterable().mapNotNull { it.asText() }

    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)

    //region Main Drawer

    /** Set up Drawer */
    /* https://stackoverflow.com/a/32614629/7061105 */
    /* https://stackoverflow.com/a/36677279/7061105 */
    /* https://stackoverflow.com/a/27352273/7061105 */
    binding.toolbar.apply {
      navigationIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_icon, null)
    }
    with(supportActionBar) {
      this?.let {
        val enable = true
        val disable = false
        val drawer = binding.layoutMainDrawer
        drawerToggle = ActionBarDrawerToggle(
          this@MainActivity,
          drawer,
          binding.toolbar,
          R.string.text_delete_all_website_entries_are_you_sure_yes, // TODO: 2021/11/13 Fix randomly chosen text.
          R.string.text_delete_all_website_entries_are_you_sure_no // TODO: 2021/11/13 Fix randomly chosen text.
        ).apply {
          isDrawerIndicatorEnabled = enable
        }
        drawer.apply {
          addDrawerListener(drawerToggle)
          /**
            TODO: Drawer
              Drawer is temporarily locked, as long as it is useless.
              As soon as a feature in the Drawer gets fully implemented,
              we can unlock it by setting its mode to `DrawerLayout.LOCK_MODE_UNLOCKED`.
          */
          setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
        drawerToggle.apply {
          syncState()
        }
        setDisplayHomeAsUpEnabled(disable)
        setHomeButtonEnabled(enable)
        /* https://stackoverflow.com/a/29055845/7061105 */
        /** See "TODO: Drawer". */
        setHomeAsUpIndicator(null)
        Log.info("""Main Drawer is locked and inaccessible.""")
      }
    }

    /** What happens when the "About" item in the Drawer was clicked. */
    binding.inclGroupAbout.groupAboutNav.menu.apply {
      getItem(0).setOnMenuItemClickListener { about ->
        Log.info("""Main Drawer: "About" was clicked.""")
        val viewAbout: View = findViewById(about.itemId)
        viewAbout.showSnackbarNotImplemented()
        true
      }
    }

    //endregion Main Drawer

    /** Edit Website Entry Result Launcher */
    onEditClickedResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val currentWebSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
        viewModel.updateWebSiteEntry(currentWebSiteEntry)
      }
    }

    //region Floating Action Button: Add WebsiteEntry

    /** Floating Action Button (Add WebsiteEntry) Result Launcher */
    onFabAddClickedResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val webSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
        viewModel.saveWebSiteEntry(webSiteEntry)
      }
    }

    replaceFabWithMenuEntryEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_REPLACE_FAB_WITH_MENU_ENTRY, false)

    if (replaceFabWithMenuEntryEnabled) {
      binding.fabAdd.apply {
        visibility = View.GONE
      }
    } else {
      binding.fabAdd.apply {
        visibility = View.VISIBLE
        setOnClickListener {
          onFabAddClicked()
        }
      }
    }

    //endregion Floating Action Button: Add WebsiteEntry

    binding.layout.btnStop.setOnClickListener { stopTask() }

    //region Website Icons

    /* TODO: Do not call on every create. Ideally, just single time per app (re-)start. */
    viewModel.assignIconUrlFetcher()

    //endregion Website Icons

    //region RecyclerView

    // Setting up RecyclerView
    val thisContext = this
    webSiteEntryAdapter = WebSiteEntryAdapter(this)
    binding.layout.recyclerView.apply {
      if (itemAnimator is SimpleItemAnimator) {
        (itemAnimator as SimpleItemAnimator).apply {
          supportsChangeAnimations = false
          changeDuration = 0
        }
      }
      layoutManager = LinearLayoutManager(thisContext)
      adapter = webSiteEntryAdapter
      if (replaceFabWithMenuEntryEnabled.not()) {
        addOnScrollListener(
          object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
              if (dy > 0) {
                binding.fabAdd.hide()
              } else if (dy < 0) {
                binding.fabAdd.show()
              }
            }
          }
        )
      }

      // Setting up Drag & Drop Re-Order WebsiteEntry List
      itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
      itemTouchHelper.attachToRecyclerView(this)
    }

    //endregion RecyclerView

    // Setting up Website Status Refresh on Swipe & Custom Monitoring
    viewModel.checkWebSiteStatus().observe(this, { status ->
      /*
        This block gets executed when Custom Monitor option is used,
        plus when pressing the Refresh option, manually.
      */

      disableSwipeRefreshIsRefreshing()
      if (isEntryCreated) {
        isEntryCreated = false
        return@observe
      }

      val urlToWebsite: Map<String, WebSiteEntry> = viewModel.getWebsiteUrlToWebsiteEntry()
      val entriesWithFailedConnection =
        status.filter {
          val currentWebsite: WebSiteEntry = urlToWebsite[it.url] ?: return@filter false
          mayNotifyStatusFailure(currentWebsite) && customMonitorData.showNotification
        }

      val customMonitorEnabled = runningCount >= 1
      val runningCountText = if (customMonitorEnabled) { "#${runningCount - 1} " } else { "" }
      val viewMain: View = findViewById(R.id.layout_main_drawer)
      if (entriesWithFailedConnection.size == 1) {
        val entryWithFailedConnection = entriesWithFailedConnection.first()
        if (customMonitorEnabled) {
          showNotification(
            applicationContext,
            runningCountText + entryWithFailedConnection.name,
            getStringNotWorking(entryWithFailedConnection.url)
          )
        } else {
          viewMain.showSnackBar(getStringNotWorking(entryWithFailedConnection.url))
        }
      } else if (entriesWithFailedConnection.size > 1) {
        /*
          TODO: 2021/12/14
            Filter running Website Entries, i.e. exclude paused entries,
            so this branch does not get triggered, if only one running entry is unavailable.
        */
        if (customMonitorEnabled) {
          showNotification(
            applicationContext,
            runningCountText + msgWebsitesNotReachable,
            entriesWithFailedConnection.joinToStringDescription()
          )
        } else {
          viewMain.showSnackBar(msgWebsitesNotReachable)
        }
      }
    })

    // Setting up ViewModel and LiveData
    viewModel.getWebSiteEntryList().observe(this, {
      Log.info("Observed Website Entry List Change.")
      /** https://stackoverflow.com/a/31486382/7061105 */
      if ((this::searchView.isInitialized.not() || searchView.isIconified) && doNotObserveWebsiteEntryChangesBecauseRecyclerViewIsRefreshing.not()) {
        Log.info("Set all TODO Items.")
        webSiteEntryAdapter.setAllTodoItems(it)
      } else if (customPrefs.getBoolean(IS_INIT, false)) {
        /**
          When starting up the application, no Website Entries are shown, if the RecyclerView is waiting for `checkWebSiteStatus()` to finish, before showing results.
          Therefore, we need to ignore that restriction once at App Start, so the RecyclerView is filled with items, ASAP.
        */
        Log.info("Set all TODO Items on App Start.")
        webSiteEntryAdapter.setAllTodoItems(it)
        customPrefs[IS_INIT] = false
      }
      if (it.isEmpty()) { viewModel.addDefaultData() } // TODO: 2021/11/12 Does this need to be called on every observation? No.
    })

    //region TOR

    torIsEnabled = customPrefs.getBoolean(SETTINGS_TOR_ENABLE, false)
    torAppIsAvailable = packageManager.packageIsInstalled(orbotFQID)
    if (torIsEnabled && torAppIsAvailable) {
      Log.info("TOR enabled.")
      val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
      StrictMode.setThreadPolicy(policy)
    } else {
      Log.info("TOR disabled.")
    }

    //endregion TOR

    //region Toggle SwipeRefresh Trigger Distance Long

    swipeRefreshTriggerDistanceLongIsEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_SWIPE_REFRESH_TRIGGER_DISTANCE_LONG, false)

    if (swipeRefreshTriggerDistanceLongIsEnabled) {
      Log.info("SwipeRefresh Trigger Distance Long enabled.")
    } else {
      Log.info("SwipeRefresh Trigger Distance Long disabled.")
    }

    //endregion Toggle SwipeRefresh Trigger Distance Long

    //region Toggle SwipeRefresh

    swipeRefreshIsEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_SWIPE_REFRESH, true)
    binding.layout.swipeRefresh.isEnabled = swipeRefreshIsEnabled

    if (swipeRefreshIsEnabled) {
      binding.layout.swipeRefresh.setOnRefreshListener()
      Log.info("SwipeRefresh enabled.")
    } else {
      Log.info("SwipeRefresh disabled.")
    }

    //endregion Toggle SwipeRefresh

    //region Debug Log

    logEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_LOG, false)

    if (logEnabled) {
      Log.info("Debug Log enabled.")
      val logIsReversed = true
      val logMaxChars = 20_000
      val viewModelLogCat by viewModels<ViewModelLogCat>()
      viewModelLogCat.logCatOutput().observeForever { rawMsg ->
        if (rawMsg.contains(nameAppCaseLower, true).not()) { return@observeForever }
        try {
          refActivityDebug
            ?.get()
            ?.fragmentLog
            ?.view
            ?.findViewById<TextView>(R.id.log_full)
            ?.apply {
              editableText.insert(
                0,
                rawMsg +
                  lineEnd +
                  logDivider +
                  lineEnd
              )
            }
        } catch (e: Exception) {}
        val updatedLogContent = if (logIsReversed) {
          rawMsg +
          lineEnd +
          logDivider +
          lineEnd +
          logContent
        } else {
          logContent +
          lineEnd +
          logDivider +
          lineEnd +
          rawMsg
        }
        logContent = with (updatedLogContent) {
          if (logIsReversed) {
            take(logMaxChars)
          } else {
            takeLast(logMaxChars)
          }
        }
      }
    } else {
      Log.info("Debug Log disabled.")
    }

    //endregion Debug Log

    //region Forced Persistent Background Service

    forcedBackgroundServiceEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE, false)

    when (forcedBackgroundServiceEnabled) {
      true -> {
        Log.info("Foreground Service enabled.")
        if (forcedBackgroundServiceRunning.not() || customPrefs[SERVICE_IS_RUNNING, false] == false) {
          Log.info("Foreground Service is not running, but it is enabled.")
          val serviceIntent = Intent(applicationContext, AppService::class.java)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
          } else {
            startService(serviceIntent)
          }
          customPrefs[SERVICE_IS_RUNNING] = true
        } else {
          Log.info("Foreground Service is running and it is enabled.")
        }
        forceStopSyncWorker()
      }
      false -> {
        Log.info("Foreground Service disabled.")
        if (forcedBackgroundServiceRunning || customPrefs[SERVICE_IS_RUNNING, true] == true) {
          Log.info("Foreground Service is running, but it is disabled.")
          val serviceIntent = Intent(applicationContext, AppService::class.java)
          /**
            By default, Service is STICKY. That means, the stopped service will immediately restart.
            Therefore, the user needs to explicitly "Force Stop" the app, to be able to stop the Forced Background Service.
          */
          applicationContext.stopService(serviceIntent)
        } else {
          Log.info("Foreground Service is not running and it is disabled.")
        }
        /* Make sure SyncWorker is not run more than once, simultaneously. */
        safelyStartSyncWorker()
      }
    }

    //endregion Forced Persistent Background Service

    //region Backup Data Import: Overwrite Existing Entries

    backupDataImportOverwriteExisting = customPrefs.getBoolean(SETTINGS_TOGGLE_BACKUP_DATA_IMPORT_OVERWRITE_EXISTING, false)

    //endregion Backup Data Import: Overwrite Existing Entries

  } /* END: onCreate */

  override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    super.onPostCreate(savedInstanceState, persistentState)
    drawerToggle.apply {
      syncState()
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    drawerToggle.apply {
      onConfigurationChanged(newConfig)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
    searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
    searchView.apply {
      setSearchableInfo(searchManager.getSearchableInfo(componentName))
      maxWidth = Integer.MAX_VALUE
      setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
          webSiteEntryAdapter.filter.filter(query)
          return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
          webSiteEntryAdapter.filter.filter(newText)
          return false
        }

      })
      setOnCloseListener {
        webSiteEntryAdapter.setAllTodoItems(null)
        return@setOnCloseListener false
      }
    }
    val fabAddAsMenuItem = menu.findItem(R.id.action_add_entry)
    if (replaceFabWithMenuEntryEnabled) {
      fabAddAsMenuItem?.apply {
        isVisible = true
      }
    } else {
      fabAddAsMenuItem?.apply {
        isVisible = false
      }
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_settings -> {
        startActivity(Intent(this, SettingsActivity::class.java))
        true
      }
      R.id.action_search -> {
        true
      }
      R.id.action_custom_monitor -> {
        showForceRefreshUI()
        true
      }
      R.id.action_refresh -> {
        viewModel.checkWebSiteStatus()
        true
      }
      R.id.action_add_entry -> {
        onFabAddClicked()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onDeleteClicked(webSiteEntry: WebSiteEntry) {
    val builder = AlertDialog.Builder(this)
    with(builder)
    {
      setTitle(getString(R.string.confirmation))
      setMessage(getString(R.string.remove_confirmation_message))
      setPositiveButton(getString(R.string.yes)) { _, _ ->
        viewModel.deleteWebSiteEntry(webSiteEntry)
        if (replaceFabWithMenuEntryEnabled.not()) {
          binding.fabAdd.show()
        }
      }
      setNegativeButton(getString(R.string.no)) { _, _ -> }
      show()
    }
  }

  override fun onEditClicked(webSiteEntry: WebSiteEntry) {
    resetSearchView()
    val intent = Intent(this, CreateEntryActivity::class.java)
    intent.putExtra(Constants.INTENT_OBJECT, webSiteEntry)
    onEditClickedResultLauncher.launch(intent)
  }

  override fun onRefreshClicked(webSiteEntry: WebSiteEntry) {
    if (handleInternetUnavailable()) { return }
    viewModel.getWebSiteStatus(webSiteEntry)
    binding.layout.swipeRefresh.showSnackBar(
      String.format(
        getString(R.string.site_refreshing),
        webSiteEntry.url
      )
    )
  }

  override fun onVisitClicked(webSiteEntry: WebSiteEntry) {
    if (handleInternetUnavailable()) { return }
    if (webSiteEntry.isOnionAddress.not()) {
      webSiteEntry.url.openInBrowser()
    } else {
      showToast(msgCannotOpenOnionInBrowser)
    }

  }

  override fun onViewClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int) {}

  override fun onPauseClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int) {
    viewModel.updateWebSiteEntry(webSiteEntry.apply {
      isPaused = this.isPaused.not()
    })
    binding.layout.swipeRefresh.showSnackBar(
      String.format(
        getString(if (webSiteEntry.isPaused) R.string.monitor_paused else R.string.monitor_resumed),
        webSiteEntry.url
      )
    )
  }

  override fun onBackPressed() {
    resetSearchView()
    super.onBackPressed()
  }

  override fun onResume() {
    super.onResume()
    stopTask()
  }

  private val itemTouchHelperCallback = object: ItemTouchHelper.Callback() {

    override fun getMovementFlags(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder
    ): Int {
      // Available movement directions.
      val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
      return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder,
      target: RecyclerView.ViewHolder
    ): Boolean {
      // Notify your adapter that an item is moved from Position X to position Y.
      webSiteEntryAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
      return true
    }

    override fun isLongPressDragEnabled(): Boolean {
      // true: You want to start dragging on long press.
      // false: You want to handle it yourself.
      return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
      super.onSelectedChanged(viewHolder, actionState)
      // Handle action state changes
      val swiping = actionState == ItemTouchHelper.ACTION_STATE_DRAG
      if (swipeRefreshIsEnabled) {
        binding.layout.swipeRefresh.isEnabled = swiping.not()
      }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
      super.clearView(recyclerView, viewHolder)

      // Called by the ItemTouchHelper when the user interaction with an element is over and it also completed its animation.
      // This is a good place to send an update to your backend about changes.

      val entries = (0..recyclerView.childCount).mapNotNull { childPosition ->
        val holder = try { recyclerView.getChildViewHolder(recyclerView.getChildAt(childPosition)) } catch (e: Exception) { return@mapNotNull null }
        val position = holder.adapterPosition
        val itemViewTag = holder.itemView.tag
        val websiteEntry = itemViewTag as WebSiteEntry
        Log.info("${itemViewTag} holder.adapterPosition: " + holder.adapterPosition)
        Log.info("holder.itemView.tag: " + websiteEntry.name)
        websiteEntry to position
      }.toMap()

      entries.forEach { entryToPosition ->
        val entry = entryToPosition.key
        entry.apply {
          itemPosition = entryToPosition.value
        }
        viewModel.updateWebSiteEntry(entry)
      }
    }
  }

  //endregion Overriden Methods
}