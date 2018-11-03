package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.streaming.DummyStreamingService
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.ObservableHashMap
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.Token
import com.example.subsinthe.crossline.util.createScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.android.Main

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val permissionListener = PermissionListener(this)
    private var settingsLayoutClosedConnection = Token()

    lateinit var drawerLayout: DrawerLayout
    val uiScope = Dispatchers.Main.createScope()
    val ioScope = Dispatchers.IO.createScope()
    val socketFactory = SocketFactory(ioScope)
    val streamingSettings = StreamingSettings()
    val streamingServices = ObservableHashMap<StreamingServiceType, IStreamingService>()
    val streamingService = ObservableValue<IStreamingService>(DummyStreamingService())
    init {
        streamingServices.put(streamingService.value.type, streamingService.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())

        setContentView(R.layout.activity_main)
        permissionListener.requestReadExternalStorage {
            val service = FilesystemStreamingService(uiScope, streamingSettings.filesystem)
            streamingServices.put(service.type, service)
            if (streamingService.value.type == StreamingServiceType.Dummy)
                streamingService.value = service
        }

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        changeLayout(R.layout.main_layout)
    }

    override fun onDestroy() {
        super.onDestroy()

        settingsLayoutClosedConnection.close()
        streamingService.value.close() // TODO: close all services after implementing normal observable collections
        socketFactory.close()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.drawer_settings -> { changeLayout(R.layout.settings_layout) }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }

    fun onSettingsLayoutCreated(layout: SettingsLayout) {
        settingsLayoutClosedConnection.close()
        settingsLayoutClosedConnection = layout.closed { changeLayout(R.layout.main_layout) }
    }

    private fun changeLayout(id: Int) {
        val fragment = when (id) {
            R.layout.main_layout -> { MainLayout() }
            R.layout.settings_layout -> { SettingsLayout() }
            else -> { throw IllegalArgumentException("Unexpected layout id $id") }
        }
        val ft = getSupportFragmentManager().beginTransaction()
        ft.replace(R.id.content_frame, fragment)
        ft.commit()
    }
}
