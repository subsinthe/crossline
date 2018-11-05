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
                activity.pushFragment(FilesystemSettings(), "Files")
                true
            }
        }
    }

    class FilesystemSettings : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.filesystem_settings)

            val activity = getActivity() as SettingsActivity

            val rootEdit = findPreference("root_edit") as EditTextPreference
            rootEdit.registerProperty(Application.config.filesystem.root) {
                try { File(it).exists() } catch (ex: Throwable) { false }.also { valid ->
                    if (!valid)
                        activity.toast("$it does not exist. Please choose valid path")
                }
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

    private fun pushFragment(fragment: Fragment, title: String) {
        supportFragmentManager.pushFragment(R.id.content_frame, fragment, title)
        actionBar.setTitle(title)
    }

    private fun tryPopFragment() = supportFragmentManager.tryPopFragment().let { title ->
        title?.also { actionBar.setTitle(title) }.let { it != null }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
