package ooo.akito.webmon.ui.home

import android.app.Activity
import android.app.Dialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.*
import android.text.TextUtils
import android.view.*
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.model.CustomMonitorData
import ooo.akito.webmon.databinding.ActivityMainBinding
import ooo.akito.webmon.databinding.CustomRefreshInputBinding
import ooo.akito.webmon.net.Utils.isConnected
import ooo.akito.webmon.ui.createentry.CreateEntryActivity
import ooo.akito.webmon.ui.debug.ActivityDebug
import ooo.akito.webmon.ui.debug.ViewModelLogCat
import ooo.akito.webmon.ui.settings.SettingsActivity
import ooo.akito.webmon.utils.*
import ooo.akito.webmon.utils.AppService
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_LOG
import ooo.akito.webmon.utils.Constants.SETTINGS_TOGGLE_SWIPE_REFRESH
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
import ooo.akito.webmon.utils.Utils.asUri
import ooo.akito.webmon.utils.Utils.forcedBackgroundServiceEnabled
import ooo.akito.webmon.utils.Utils.getStringNotWorking
import ooo.akito.webmon.utils.Utils.globalEntryTagsNames
import ooo.akito.webmon.utils.Utils.isEntryCreated
import ooo.akito.webmon.utils.Utils.joinToStringDescription
import ooo.akito.webmon.utils.Utils.lineEnd
import ooo.akito.webmon.utils.Utils.logContent
import ooo.akito.webmon.utils.Utils.logEnabled
import ooo.akito.webmon.utils.Utils.mapperUgly
import ooo.akito.webmon.utils.Utils.mayNotifyStatusFailure
import ooo.akito.webmon.utils.Utils.openInBrowser
import ooo.akito.webmon.utils.Utils.safelyStartSyncWorker
import ooo.akito.webmon.utils.Utils.showNotification
import ooo.akito.webmon.utils.Utils.showSnackBar
import ooo.akito.webmon.utils.Utils.showSnackbarNotImplemented
import ooo.akito.webmon.utils.Utils.showToast
import ooo.akito.webmon.utils.Utils.swipeRefreshIsEnabled
import ooo.akito.webmon.utils.Utils.torAppIsAvailable
import ooo.akito.webmon.utils.Utils.torIsEnabled
import ooo.akito.webmon.utils.Utils.totalAmountEntry
import java.lang.ref.WeakReference
import java.util.*


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

  private lateinit var viewModel: MainViewModel
  private lateinit var searchView: SearchView

  private lateinit var webSiteEntryAdapter: WebSiteEntryAdapter
  private lateinit var binding: ActivityMainBinding

  private lateinit var customRefreshInputBinding: CustomRefreshInputBinding

  private lateinit var onEditClickedResultLauncher: ActivityResultLauncher<Intent>

  private var handler = Handler(Looper.getMainLooper())

  private var runningCount = 0
  private var customMonitorData: CustomMonitorData = CustomMonitorData()

  private lateinit var itemTouchHelper: ItemTouchHelper
  private lateinit var drawerToggle: ActionBarDrawerToggle

  private val runnableTask: Runnable = Runnable {
    if (runningCount == 0) {
      stopTask()
    } else {
      startUpdateTask(isUpdate = true)
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
    this.setOnRefreshListener LISTENER@{
      disableSwipeRefreshIsRefreshing()
      if (isConnected(applicationContext).not()) {
        applicationContext.showToast(getString(R.string.check_internet))
        return@LISTENER
      }
      viewModel.checkWebSiteStatus()
    }
  }

  private fun packageIsInstalled(packageName: String, pacman: PackageManager): Boolean {
    return try {
      pacman.getPackageInfo(packageName, 0)
      true
    } catch (e: Exception) {
      false
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    locale = getCurrentLocale()
    defaultTimeFormat = locale.getDefaultDateTimeFormat()
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater)
    binding = ActivityMainBinding.inflate(layoutInflater)
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater, binding.root, false)
    globalEntryTagsNames = mapperUgly.readTree(SharedPrefsManager.customPrefs.getString(WEBSITE_ENTRY_TAG_CLOUD_DATA, defaultJArrayAsString)).asIterable().mapNotNull { it.asText() }

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
      }
    }

    /** What happens when the "About" item in the Drawer was clicked. */
    binding.inclGroupAbout.groupAboutNav.menu.apply {
      getItem(0).setOnMenuItemClickListener { about ->
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

    /** Floating Action Button (Add WebsiteEntry) Result Launcher */
    val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val webSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
        viewModel.saveWebSiteEntry(webSiteEntry)
      }
    }

    binding.fabAdd.setOnClickListener {
      resetSearchView()
      totalAmountEntry = webSiteEntryAdapter.itemCount
      val intent = Intent(this, CreateEntryActivity::class.java)
      resultLauncher.launch(intent)
    }

    binding.layout.btnStop.setOnClickListener { stopTask() }

    // Setting up RecyclerView
    val thisContext = this
    webSiteEntryAdapter = WebSiteEntryAdapter(this)
    binding.layout.recyclerView.apply {
      layoutManager = LinearLayoutManager(thisContext)
      adapter = webSiteEntryAdapter
      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          if (dy > 0) {
            //TODO: Fix https://github.com/theAkito/webmon/issues/17
            binding.fabAdd.hide()
          } else if (dy < 0)
            binding.fabAdd.show()
        }
      })

      // Setting up Drag & Drop Re-Order WebsiteEntry List
      itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
      itemTouchHelper.attachToRecyclerView(this)
    }


    // Setting up ViewModel and LiveData
    viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    viewModel.getWebSiteEntryList().observe(this, {
      Log.info("Observed Website Entry List Change.")
      /** https://stackoverflow.com/a/31486382/7061105 */
      if (this::searchView.isInitialized.not() || searchView.isIconified) {
        Log.info("Observed Website Entry List Change and set all TODO Items.")
        webSiteEntryAdapter.setAllTodoItems(it)
      }
      if (it.isEmpty()) { viewModel.addDefaultData() } // TODO: 2021/11/12 Does this need to be called on every observation? No.
    })

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

    //region TOR

    torIsEnabled = customPrefs.getBoolean(SETTINGS_TOR_ENABLE, false)
    torAppIsAvailable = packageIsInstalled(orbotFQID, packageManager)
    if (torIsEnabled && torAppIsAvailable) {
      Log.info("Tor enabled.")
      val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
      StrictMode.setThreadPolicy(policy)
    } else {
      Log.info("Tor disabled.")
    }

    //endregion TOR

    //region Toggle SwipeRefresh

    swipeRefreshIsEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_SWIPE_REFRESH, true)
    binding.layout.swipeRefresh.isEnabled = true
    binding.layout.swipeRefresh.setOnRefreshListener()

    //endregion Toggle SwipeRefresh

    //region Debug Log

    logEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_LOG, false)

    if (logEnabled) {
      Log.info("Log enabled.")
      val viewModelLogCat by viewModels<ViewModelLogCat>()
      viewModelLogCat.logCatOutput().observeForever { rawMsg ->
        if (rawMsg.contains(nameAppCaseLower, true).not()) { return@observeForever }
        try {
          refActivityDebug
            ?.get()
            ?.fragmentLog
            ?.view
            ?.findViewById<TextView>(R.id.log_full)
            ?.append(rawMsg + lineEnd)
        } catch (e: Exception) {}
        val updatedLogContent =
          logContent +
              rawMsg +
              lineEnd +
              logDivider +
              lineEnd
        logContent = updatedLogContent.takeLast(20_000)
      }
    } else {
      Log.info("Log disabled.")
    }

    //endregion Debug Log

    //region Forced Persistent Background Service

    forcedBackgroundServiceEnabled = customPrefs.getBoolean(SETTINGS_TOGGLE_FORCED_BACKGROUND_SERVICE, false)

    if (forcedBackgroundServiceEnabled) {
      Log.info("Foreground Service enabled.")
      val serviceIntent = Intent(applicationContext, AppService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
      } else {
        startService(serviceIntent)
      }
    } else if (forcedBackgroundServiceEnabled.not()) {
      Log.info("Foreground Service disabled.")
      /* Make sure SyncWorker is not run more than once, simultaneously. */
      safelyStartSyncWorker()
    }

    //endregion Forced Persistent Background Service

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
    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    searchView = menu.findItem(R.id.action_search)?.actionView as SearchView
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    searchView.maxWidth = Integer.MAX_VALUE
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        webSiteEntryAdapter.filter.filter(query)
        return false
      }

      override fun onQueryTextChange(newText: String?): Boolean {
        webSiteEntryAdapter.filter.filter(newText)
        return false
      }

    })
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
      else -> super.onOptionsItemSelected(item)
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
        if (!TextUtils.isEmpty(customRefreshInputBinding.editDuration.text)) {
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

  override fun onDeleteClicked(webSiteEntry: WebSiteEntry) {
    val builder = AlertDialog.Builder(this)
    with(builder)
    {
      setTitle(getString(R.string.confirmation))
      setMessage(getString(R.string.remove_confirmation_message))
      setPositiveButton(getString(R.string.yes)) { _, _ -> viewModel.deleteWebSiteEntry(webSiteEntry) }
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

  private fun resetSearchView() {
    if (!searchView.isIconified) {
      searchView.isIconified = true
      return
    }
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
      binding.layout.swipeRefresh.isEnabled = swiping.not()
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

  override fun onDestroy() {
    super.onDestroy()
  }
}