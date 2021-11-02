package ooo.akito.webmon.ui.home

import android.app.Activity
import android.app.Dialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ooo.akito.webmon.R
import ooo.akito.webmon.data.db.WebSiteEntry
import ooo.akito.webmon.data.metadata.BackupEnvironment.defaultBackupWebsitesVersion
import ooo.akito.webmon.data.model.BackupWebsites
import ooo.akito.webmon.data.model.CustomMonitorData
import ooo.akito.webmon.databinding.ActivityMainBinding
import ooo.akito.webmon.databinding.CustomRefreshInputBinding
import ooo.akito.webmon.ui.createentry.CreateEntryActivity
import ooo.akito.webmon.ui.settings.SettingsActivity
import ooo.akito.webmon.utils.*
import ooo.akito.webmon.utils.Environment.defaultTimeFormat
import ooo.akito.webmon.utils.Environment.getCurrentLocale
import ooo.akito.webmon.utils.Environment.getDefaultDateTimeFormat
import ooo.akito.webmon.utils.Environment.getDefaultDateTimeString
import ooo.akito.webmon.utils.Environment.locale
import ooo.akito.webmon.utils.Utils.appIsVisible
import ooo.akito.webmon.utils.Utils.asUri
import ooo.akito.webmon.utils.Utils.getStringNotWorking
import ooo.akito.webmon.utils.Utils.joinToStringDescription
import ooo.akito.webmon.utils.Utils.openInBrowser
import ooo.akito.webmon.utils.Utils.showAutoStartEnableDialog
import ooo.akito.webmon.utils.Utils.showNotification
import ooo.akito.webmon.utils.Utils.startWorkManager
import java.util.*


class MainActivity : AppCompatActivity(), WebSiteEntryAdapter.WebSiteEntryEvents {

  companion object {
    val mapper: ObjectMapper = jacksonObjectMapper()
  }

  private lateinit var viewModel: MainViewModel
  private lateinit var searchView: SearchView

  private lateinit var webSiteEntryAdapter: WebSiteEntryAdapter
  private lateinit var binding: ActivityMainBinding

  private lateinit var customRefreshInputBinding: CustomRefreshInputBinding

  private lateinit var onEditClickedResultLauncher: ActivityResultLauncher<Intent>
  private lateinit var onBackupWebsiteEntriesResultLauncher: ActivityResultLauncher<String>

  var handler = Handler(Looper.getMainLooper())

  private var runningCount = 0
  private var customMonitorData: CustomMonitorData = CustomMonitorData()

  private lateinit var itemTouchHelper: ItemTouchHelper

  private val runnableTask: Runnable = Runnable {
    if (runningCount == 0) {
      stopTask()
    } else {
      startUpdateTask(isUpdate = true)
    }
  }

  private fun startUpdateTask(isUpdate: Boolean = true) {
    Print.log("Called on main thread $runningCount")
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

  private fun handleInternetUnavailable(): Boolean {
    return if (!NetworkUtils.isConnected(applicationContext)) {
      if (binding.layout.swipeRefresh.isRefreshing)
        binding.layout.swipeRefresh.isRefreshing = false
      Utils.showToast(applicationContext, getString(R.string.check_internet))
      Print.log("Internet unavailable!")
      true
    } else {
      false
    }
  }

  private fun String?.openInBrowser() {
    val uri = this.asUri()
    if (uri == null) {
      Print.log("URI provided is null!")
      return
    }
    this@MainActivity.openInBrowser(uri)
  }

  private fun generateBackupWebsites(locationSave: String): BackupWebsites {
    return BackupWebsites(
      version = defaultBackupWebsitesVersion,
      timestamp = getDefaultDateTimeString(),
      locationSaved = locationSave,
      entries = viewModel.getWebSiteEntryList().value
        ?: throw IllegalAccessError("Cannot get WebSiteEntryList from LiveData!")
    )
  }

  private fun generateBackupWebsitesJString(locationSave: String): JString {
    return mapper.writeValueAsString(generateBackupWebsites(locationSave))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    locale = getCurrentLocale()
    defaultTimeFormat = locale.getDefaultDateTimeFormat()
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater)
    binding = ActivityMainBinding.inflate(layoutInflater)
    customRefreshInputBinding = CustomRefreshInputBinding.inflate(layoutInflater, binding.root, false)

    setContentView(binding.root)
    setSupportActionBar(binding.toolbar)

    // Edit Website Entry Result Launcher
    onEditClickedResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val currentWebSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
        viewModel.updateWebSiteEntry(currentWebSiteEntry)
      }
    }

    // Fab click listener
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val data: Intent? = result.data
        val webSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
        viewModel.saveWebSiteEntry(webSiteEntry)
      }
    }

    /** Backup Website Entries */
    onBackupWebsiteEntriesResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
      val backupFilePathRelative = uri.path ?: throw IllegalStateException("Backup URI does not provide a valid path!")
      val backupFileContent = generateBackupWebsitesJString(backupFilePathRelative)
      val resolver = this@MainActivity.contentResolver
      /** https://stackoverflow.com/a/64733499/7061105 */
      val out = resolver.openOutputStream(uri) ?: throw IllegalAccessError("Cannot open output stream when trying to write Backup Website Entries File!")
      out.use { stream ->
        stream.write(backupFileContent.toByteArray())
        stream.flush()
      }
    }

    binding.fabAdd.setOnClickListener {
      resetSearchView()
      Utils.totalAmountEntry = webSiteEntryAdapter.itemCount
      val intent = Intent(this, CreateEntryActivity::class.java)
      resultLauncher.launch(intent)
    }

    binding.layout.btnStop.setOnClickListener { stopTask() }

    binding.layout.swipeRefresh.setOnRefreshListener {
      if (!NetworkUtils.isConnected(applicationContext)) {
        if (binding.layout.swipeRefresh.isRefreshing)
          binding.layout.swipeRefresh.isRefreshing = false
        Utils.showToast(applicationContext, getString(R.string.check_internet))
        return@setOnRefreshListener
      }
      viewModel.checkWebSiteStatus()
    }

    // Setting up RecyclerView
    val thisContext = this
    webSiteEntryAdapter = WebSiteEntryAdapter(this)
    binding.layout.recyclerView.apply {
      layoutManager = LinearLayoutManager(thisContext)
      adapter = webSiteEntryAdapter
      addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          if (dy > 0) {
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
    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    viewModel.getWebSiteEntryList().observe(this, {
      webSiteEntryAdapter.setAllTodoItems(it)
      if (it.isEmpty()) { viewModel.addDefaultData() }
    })

    // Setting up Custom Monitor Option
    viewModel.getAllWebSiteStatusList().observe(this, { it ->
      /*
        This block gets executed when Custom Monitor option is used,
        plus when pressing the Refresh option, manually.
      */

      if (binding.layout.swipeRefresh.isRefreshing) {
        binding.layout.swipeRefresh.isRefreshing = false
      }

      if (appIsVisible().not()) {
        /*
          If Custom Monitor option is used,
          we only want to send notifications,
          when App is in Foreground.
        */
        return@observe
      }

      val entriesWithFailedConnection =
        it.filter {
          Utils.mayNotifyStatusFailure(it.status) &&
              customMonitorData.showNotification
        }

      val customMonitorEnabled = runningCount >= 1
      val runningCountText = if (customMonitorEnabled) { "#${runningCount - 1} " } else { "" }
      if (entriesWithFailedConnection.size == 1) {
        val entryWithFailedConnection = entriesWithFailedConnection.first()
        if (customMonitorEnabled) {
          showNotification(
            applicationContext,
            runningCountText + entryWithFailedConnection.name,
            getStringNotWorking(entryWithFailedConnection.url)
          )
        } else {
          Toast.makeText(
            applicationContext,
            getStringNotWorking(entryWithFailedConnection.url),
            Toast.LENGTH_LONG
          ).show()
        }
      } else {
        if (customMonitorEnabled) {
          showNotification(
            applicationContext,
            runningCountText + "Several Websites are not reachable!",
            entriesWithFailedConnection.joinToStringDescription()
          )
        } else {
          Toast.makeText(
            applicationContext,
            "Several Websites are not reachable!",
            Toast.LENGTH_LONG
          ).show()
        }
      }
    })

    startWorkManager(this)

    Handler(Looper.getMainLooper()).postDelayed({
      if (!isDestroyed) showAutoStartEnableDialog(
        this
      )
    }, 1000)

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
      R.id.action_search -> true
      R.id.action_custom_monitor -> {
        showForceRefreshUI()
        true
      }
      R.id.action_refresh -> {
        viewModel.checkWebSiteStatus()
        true
      }
      R.id.action_backup -> {
        onBackupWebsiteEntriesResultLauncher.launch("backup-webmon_${getDefaultDateTimeString()}.json")
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
        if (!NetworkUtils.isConnected(applicationContext)) {
          Utils.showToast(applicationContext, getString(R.string.check_internet))
          return@setOnClickListener
        }
        if (!TextUtils.isEmpty(customRefreshInputBinding.editDuration.text)) {
          if (customRefreshInputBinding.checkboxAgree.isChecked) {
            val durationBy = if (customRefreshInputBinding.rgDurationType.checkedRadioButtonId == R.id.rbDurationMin) 60 * 1000 else 1000
            val duration = customRefreshInputBinding.editDuration.text.toString().toLong()

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
            Utils.showToast(applicationContext, getString(R.string.error_read_and_agree_checkbox))
        } else
          Utils.showToast(applicationContext, getString(R.string.enter_valid_input))
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
    Utils.showSnackBar(
      binding.layout.swipeRefresh, String.format(
        getString(R.string.site_refreshing),
        webSiteEntry.url
      )
    )
  }

  override fun onVisitClicked(webSiteEntry: WebSiteEntry) {
    if (handleInternetUnavailable()) { return }
    webSiteEntry.url.openInBrowser()

  }

  override fun onViewClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int) {}

  override fun onPauseClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int) {
    viewModel.updateWebSiteEntry(webSiteEntry.apply {
      isPaused = this.isPaused.not()
    })
    Utils.showSnackBar(
      binding.layout.swipeRefresh, String.format(
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
      // Hanlde action state changes
      val swiping = actionState == ItemTouchHelper.ACTION_STATE_DRAG
      binding.layout.swipeRefresh.isEnabled = swiping.not()
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
      super.clearView(recyclerView, viewHolder)

      // Called by the ItemTouchHelper when the user interaction with an element is over and it also completed its animation.
      // This is a good place to send an update to your backend about changes.

      val entries = (0..recyclerView.childCount).mapNotNull {
        val holder = try { recyclerView.getChildViewHolder(recyclerView.getChildAt(it)) } catch (e: Exception) { return@mapNotNull null }
        val position = holder.adapterPosition
        Print.log("${holder.itemView.tag} holder.adapterPosition: " + holder.adapterPosition)
        Print.log("holder.itemView.tag: " + (holder.itemView.tag as WebSiteEntry).name)
        (holder.itemView.tag as WebSiteEntry) to position
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

}