package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.Menu
import android.widget.Toast
import java.io.File

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

            val activity = getActivity() as SettingsActivity
            val config = Application.config.filesystem

            val rootEdit = findPreference("root_edit") as EditTextPreference
            rootEdit.setText(config.root.value)
            rootEdit.setOnPreferenceChangeListener { _, newValue ->
                val newRoot = newValue!!.toString()
                val isValid = try { File(newRoot).exists() } catch (ex: Throwable) { false }
                if (isValid)
                    config.root.value = newRoot
                else
                    activity.toast("$newRoot does not exist. Please choose valid path")
                isValid
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

        supportFragmentManager.beginTransaction().replace(R.id.content_frame, Settings()).commit()
        actionBar.setTitle("Settings")
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

    override fun onBackPressed() {
        if (tryPopFragment())
            return
        super.onBackPressed()
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
        if (fragmentCount == 0)
            return false

        manager.popBackStack()
        if (fragmentCount == 1)
            return true

        val currentFragment = fragmentCount - 2
        actionBar.setTitle(manager.getBackStackEntryAt(currentFragment).getName())
        return true
    }

    fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        enum class FragmentDescriptor(val builder: () -> Fragment, val title: String) {
            FilesystemSettings({ FilesystemSettings() }, "Filesystem")
        }
    }
}
