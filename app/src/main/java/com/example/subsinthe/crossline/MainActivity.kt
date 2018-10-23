package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.Menu
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.ObservableArrayList
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.TokenPool
import com.example.subsinthe.crossline.util.Token
import com.example.subsinthe.crossline.util.createScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.android.Main

class MainActivity : AppCompatActivity() {
    private val uiScope = Dispatchers.Main.createScope()
    private val ioScope = Dispatchers.IO.createScope()
    private val socketFactory = SocketFactory(ioScope)
    private val streamingService = ObservableValue<IStreamingService>(
        FilesystemStreamingService(uiScope)
    )
    private val tokens = TokenPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.apply {
            setDisplayShowTitleEnabled(false)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_dehaze)
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(
            object : NavigationView.OnNavigationItemSelectedListener {
                override fun onNavigationItemSelected(item: MenuItem): Boolean {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return true
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        tokens.close()
        streamingService.value.close()
        socketFactory.close()
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.main, menu)

        val mainActivity = this
        uiScope.launch {
            val searchResults = ObservableArrayList<IStreamingService.MusicTrack>()
            val searchQueryListener = SearchQueryListener(
                uiScope, searchResults, streamingService
            )
            val searchModel = SearchModel(searchResults)

            mainActivity.tokens += Token(searchModel)
            mainActivity.tokens += Token(searchQueryListener)

            findViewById<RecyclerView>(R.id.search_results).apply {
                layoutManager = LinearLayoutManager(mainActivity)
                adapter = searchModel
            }

            val searchView = menu.findItem(R.id.action_search).getActionView() as SearchView
            searchView.setOnQueryTextListener(searchQueryListener)
        }
        return true
    }
}
