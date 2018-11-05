package com.example.subsinthe.crossline

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.Menu
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.ObservableArrayList
import com.example.subsinthe.crossline.util.TokenPool
import com.example.subsinthe.crossline.util.Token

class MainActivity : AppCompatActivity() {
    private val permissionListener = PermissionListener(this)
    private val tokens = TokenPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())

        setContentView(R.layout.main_activity)
        Application.initialize(getApplicationContext(), permissionListener)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { onNavigationItemSelected(it) }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_dehaze)
        tokens += Application.streamingService.subscribe { setActionBarTitle(actionBar, it) }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.main, menu)

        val layoutManager = LinearLayoutManager(this)
        val searchResults = ObservableArrayList<MusicTrack>()
        val searchMoreListener = LoadMoreScrollListener(layoutManager)
        val searchQueryListener = SearchQueryListener(
            Application.streamingService,
            Application.uiScope,
            searchResults,
            searchMoreListener.channel,
            loadBatchSize = Application.config.ui.searchLoadBatchSize,
            delayOnQueryChange = Application.config.ui.searchDelayOnQueryChange
        )
        val searchModel = SearchModel(searchResults, searchQueryListener.isSearchActive)

        val searchResultsView = findViewById<RecyclerView>(R.id.search_results)
        val searchView = menu.findItem(R.id.action_search).getActionView() as SearchView

        tokens += Token(searchModel)
        tokens += Token(searchQueryListener)

        searchResultsView.layoutManager = layoutManager
        searchResultsView.adapter = searchModel
        searchResultsView.addOnScrollListener(searchMoreListener)

        searchView.setOnQueryTextListener(searchQueryListener)
        searchView.addOnAttachStateChangeListener(searchQueryListener)

        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        tokens.close()
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionListener.report(requestCode, permissions, grantResults)
    }

    private fun setActionBarTitle(actionBar: ActionBar, service: IStreamingService) {
        val type = when (service.type) {
            StreamingServiceType.Dummy -> null
            StreamingServiceType.Filesystem -> "files"
        }
        actionBar.setTitle(type?.let { "Browse $it" } ?: "")
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.drawer_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }
}
