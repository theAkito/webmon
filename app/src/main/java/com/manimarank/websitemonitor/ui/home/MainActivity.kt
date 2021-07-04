package com.manimarank.websitemonitor.ui.home

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.ui.createentry.CreateEntryActivity
import com.manimarank.websitemonitor.ui.settings.SettingsActivity
import com.manimarank.websitemonitor.utils.Constants
import com.manimarank.websitemonitor.utils.Utils
import com.manimarank.websitemonitor.utils.Utils.openUrl
import com.manimarank.websitemonitor.utils.Utils.showAutoStartEnableDialog
import com.manimarank.websitemonitor.utils.Utils.showNotification
import com.manimarank.websitemonitor.utils.Utils.startWorkManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT


class MainActivity : AppCompatActivity(), WebSiteEntryAdapter.WebSiteEntryEvents {

    private lateinit var viewModel: MainViewModel
    private lateinit var searchView: SearchView
    private lateinit var adapter: WebSiteEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Fab click listener
        fabAdd.setOnClickListener {
            resetSearchView()
            val intent = Intent(this, CreateEntryActivity::class.java)
            startActivityForResult(intent, Constants.INTENT_CREATE_ENTRY)
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.checkWebSiteStatus()
        }

        //Setting up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WebSiteEntryAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    fabAdd.hide()
                } else if (dy < 0)
                    fabAdd.show()
            }
        })


        //Setting up ViewModel and LiveData
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.getWebSiteEntryList().observe(this, {
            adapter.setAllTodoItems(it)
            if (it.isEmpty())
                viewModel.addDefaultData()
        })

        viewModel.getAllWebSiteStatusList().observe(this, { it ->
            if (swipeRefresh.isRefreshing)
                swipeRefresh.isRefreshing = false
            it.filter { Utils.isValidNotifyStatus(it.status) }.forEach {
                showNotification(applicationContext, it.name,  String.format(getString(R.string.not_working, it.url)))
            }
        })

        viewModel.getWebSiteStatus().observe(this, {
            if (Utils.isValidNotifyStatus(it.status)) {
                showNotification(applicationContext, it.name,  String.format(getString(R.string.not_working, it.url)))
            }
        })

        startWorkManager(this)

        Handler(Looper.getMainLooper()).postDelayed({ if (!isDestroyed) showAutoStartEnableDialog(this) }, 300)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.action_search)
            ?.actionView as SearchView
        searchView.setSearchableInfo(searchManager
            .getSearchableInfo(componentName))
        searchView.maxWidth = Integer.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }

        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDeleteClicked(webSiteEntry: WebSiteEntry) {
        val builder = AlertDialog.Builder(this)
        with(builder)
        {
            setTitle(getString(R.string.confirmation))
            setMessage(getString(R.string.remove_confirmation_message))
            setPositiveButton(getString(R.string.yes)) { _, _ -> viewModel.deleteWebSiteEntry(webSiteEntry) }
            setNegativeButton(getString(R.string.no)) {_, _ -> }
            show()
        }
    }

    override fun onEditClicked(webSiteEntry: WebSiteEntry) {
        resetSearchView()
        val intent = Intent(this, CreateEntryActivity::class.java)
        intent.putExtra(Constants.INTENT_OBJECT, webSiteEntry)
        startActivityForResult(intent, Constants.INTENT_UPDATE_ENTRY)
    }

    override fun onRefreshClicked(webSiteEntry: WebSiteEntry) {
        viewModel.getWebSiteStatus(webSiteEntry)
        Utils.showSnackBar(swipeRefresh, String.format(getString(R.string.site_refreshing), webSiteEntry.url))
    }

    override fun onViewClicked(webSiteEntry: WebSiteEntry) {
        openUrl(applicationContext, webSiteEntry.url)
    }

    override fun onPauseClicked(webSiteEntry: WebSiteEntry, adapterPosition: Int) {
        viewModel.updateWebSiteEntry(webSiteEntry.apply {
            isPaused = this.isPaused.not()
        })
        Utils.showSnackBar(swipeRefresh, String.format(getString(if (webSiteEntry.isPaused) R.string.monitor_paused else R.string.monitor_resumed), webSiteEntry.url))
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

    /**
     * Activity result callback
     * Triggers when Save button clicked from @CreateEntryActivity
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val webSiteEntry = data?.getParcelableExtra<WebSiteEntry>(Constants.INTENT_OBJECT)!!
            when (requestCode) {
                Constants.INTENT_CREATE_ENTRY -> {
                    viewModel.saveWebSiteEntry(webSiteEntry)
                }
                Constants.INTENT_UPDATE_ENTRY -> {
                    viewModel.updateWebSiteEntry(webSiteEntry)
                }
            }
        }
    }
}