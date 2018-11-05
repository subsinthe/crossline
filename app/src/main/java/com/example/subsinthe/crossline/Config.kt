package com.example.subsinthe.crossline

import android.content.Context
import android.os.Environment

class Config(context: Context) {
    val filesystem = FilesystemConfig(context)
}

class FilesystemConfig(context: Context) : ConfigSection(context, "filesystem") {
    val root = pref("root", Environment.getExternalStorageDirectory().toString())
    val cacheSize = 10000
    val cacheLifespan = 20 * 60 * 60 * 1000L
}
