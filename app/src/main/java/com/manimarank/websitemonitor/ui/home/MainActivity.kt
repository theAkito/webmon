package com.manimarank.websitemonitor.ui.home

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.manimarank.websitemonitor.R
import com.manimarank.websitemonitor.data.db.WebSiteEntry
import com.manimarank.websitemonitor.ui.createentry.CreateEntryActivity
import com.manimarank.websitemonitor.ui.settings.SettingsActivity
import com.manimarank.websitemonitor.utils.Constants
import com.manimarank.websitemonitor.utils.SharedPrefsManager
import com.manimarank.websitemonitor.utils.SharedPrefsManager.get
import com.manimarank.websitemonitor.utils.SharedPrefsManager.set
import com.manimarank.websitemonitor.utils.Utils.openUrl
import com.manimarank.websitemonitor.utils.Utils.showAutoStartEnableDialog
import com.manimarank.websitemonitor.utils.Utils.showNotification
import com.manimarank.websitemonitor.utils.Utils.startWorkManager
import com.manimarank.websitemonitor.worker.WorkManagerScheduler
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


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


        //Setting up ViewModel and LiveData
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.getWebSiteEntryList().observe(this, Observer {
            adapter.setAllTodoItems(it)
            if (it.isEmpty())
                viewModel.addDefaultData()
        })

        viewModel.getAllWebSiteStatusList().observe(this, Observer {
            if (swipeRefresh.isRefreshing)
                swipeRefresh.isRefreshing = false
            it.filter { !it.isSuccessful }.forEach {
                showNotification(applicationContext, it.name, it.url + " - Not Working!")
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
            setTitle("Confirmation")
            setMessage("Do you want to remove?")
            setPositiveButton("Yes") { _, _ -> viewModel.deleteWebSiteEntry(webSiteEntry) }
            setNegativeButton("No") {_, _ -> }
            show()
        }
    }

    override fun onEditClicked(webSiteEntry: WebSiteEntry) {
        resetSearchView()
        val intent = Intent(this, CreateEntryActivity::class.java)
        intent.putExtra(Constants.INTENT_OBJECT, webSiteEntry)
        startActivityForResult(intent, Constants.INTENT_UPDATE_ENTRY)
    }

    override fun onViewClicked(webSiteEntry: WebSiteEntry) {
        openUrl(applicationContext, webSiteEntry.url)
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