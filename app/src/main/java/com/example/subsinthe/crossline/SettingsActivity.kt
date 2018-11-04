package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.Menu

class SettingsActivity : AppCompatActivity() {
    private lateinit var actionBar: ActionBar

    class Settings : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)

            val activity = getActivity() as SettingsActivity

            findPreference("filesystem_button").setOnPreferenceClickListener { _ ->
                activity.pushFragment(FragmentDescriptor.FilesystemSettings)
                true
            }
        }
    }

    class FilesystemSettings : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.filesystem_settings)

            val settings = Application.streamingSettings.filesystem

            val rootEdit = findPreference("root_edit")
            rootEdit.setDefaultValue(settings.root.value)
            rootEdit.setOnPreferenceChangeListener { _, newValue ->
                settings.root.value = newValue!!.toString()
                true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        actionBar = supportActionBar!!
        actionBar.setHomeButtonEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        pushFragment(FragmentDescriptor.Settings)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.title_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.getItemId()) {
            android.R.id.home -> {
                if (tryPopFragment())
                    return true
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    fun pushFragment(descriptor: FragmentDescriptor) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, descriptor.builder())
            .addToBackStack(descriptor.title)
            .commit()
        actionBar.setTitle(descriptor.title)
    }

    fun tryPopFragment(): Boolean {
        val manager = supportFragmentManager
        val fragmentCount = manager.getBackStackEntryCount()
        if (fragmentCount == 1)
            return false

        manager.popBackStack()

        val currentFragment = fragmentCount - 2
        actionBar.setTitle(manager.getBackStackEntryAt(currentFragment).getName())
        return true
    }

    companion object {
        enum class FragmentDescriptor(val builder: () -> Fragment, val title: String) {
            Settings({ Settings() }, "Settings"),
            FilesystemSettings({ FilesystemSettings() }, "Filesystem")
        }
    }
}
