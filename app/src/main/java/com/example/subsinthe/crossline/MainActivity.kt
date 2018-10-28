package com.example.subsinthe.crossline

import android.os.Bundle
import android.os.Environment
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
import android.view.View
import android.widget.ViewFlipper
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.streaming.DummyStreamingService
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.ObservableArrayList
import com.example.subsinthe.crossline.util.ObservableHashMap
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
    private val filesystemStreamingServiceSettings =
        ObservableValue<FilesystemStreamingService.Settings>(FilesystemStreamingService.Settings(
            root = Environment.getExternalStorageDirectory().toString()
        ))
    private val streamingServices = ObservableHashMap<StreamingServiceType, IStreamingService>()
    private val streamingService = ObservableValue<IStreamingService>(DummyStreamingService())
    private val permissionListener = PermissionListener(this)
    private val tokens = TokenPool()
    init {
        streamingServices.put(streamingService.value.type, streamingService.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainActivity = this

        AndroidLoggingHandler.reset(AndroidLoggingHandler())

        setContentView(R.layout.activity_main)
        permissionListener.requestReadExternalStorage {
            val service = FilesystemStreamingService(uiScope, filesystemStreamingServiceSettings)
            streamingServices.put(service.type, service)
            if (streamingService.value.type == StreamingServiceType.Dummy)
                streamingService.value = service
        }

        val mainToolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(mainToolbar)
        supportActionBar!!.apply {
            setDisplayShowTitleEnabled(false)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_dehaze)
        }

        val settingsToolbar = findViewById<Toolbar>(R.id.settings_toolbar)
        settingsToolbar.apply {
            setTitle("Settings")
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View) {
                    changeMainView(R.id.main_layout)
                }
            })
        }

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            mainToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(
            object : NavigationView.OnNavigationItemSelectedListener {
                override fun onNavigationItemSelected(item: MenuItem): Boolean {
                    mainActivity.onNavigationItemSelected(item)
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
            val layoutManager = LinearLayoutManager(mainActivity)
            val searchResults = ObservableArrayList<MusicTrack>()
            val searchMoreListener = LoadMoreScrollListener(layoutManager)
            val searchQueryListener = SearchQueryListener(
                streamingService,
                uiScope,
                searchResults,
                searchMoreListener.channel,
                loadBatchSize = 10,
                searchDelayOnQueryChange = 1500
            )
            val searchModel = SearchModel(searchResults, searchQueryListener.isSearchActive)
            val streamingSettingsModel = StreamingSettingsModel(
                streamingServices, hashSetOf(StreamingServiceType.Dummy)
            )

            val searchResultsView = findViewById<RecyclerView>(R.id.search_results)
            val streamingSettingsView = findViewById<RecyclerView>(R.id.streaming_settings_view)
            val searchView = menu.findItem(R.id.action_search).getActionView() as SearchView

            mainActivity.tokens += Token(searchModel)
            mainActivity.tokens += Token(streamingSettingsModel)
            mainActivity.tokens += Token(searchQueryListener)

            searchResultsView.layoutManager = layoutManager
            searchResultsView.adapter = searchModel
            searchResultsView.addOnScrollListener(searchMoreListener)

            streamingSettingsView.layoutManager = LinearLayoutManager(mainActivity)
            streamingSettingsView.adapter = streamingSettingsModel

            searchView.setOnQueryTextListener(searchQueryListener)
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionListener.report(requestCode, permissions, grantResults)
    }

    private fun onNavigationItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.drawer_settings -> {
                changeMainView(R.id.settings_layout)
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
    }

    private fun changeMainView(id: Int) {
        val viewFlipper = findViewById<ViewFlipper>(R.id.main_view_flipper)
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(id)))
    }
}
