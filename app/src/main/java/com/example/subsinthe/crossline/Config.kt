package com.example.subsinthe.crossline

import android.content.Context
import android.os.Environment

class Config(context: Context) {
    val ui = UiConfig(context)
    val filesystem = FilesystemConfig(context)
}

class UiConfig(context: Context) : ConfigSection(context, "filesystem") {
    val searchLoadBatchSize = 10
    val searchDelayOnQueryChange = 1500
}

class FilesystemConfig(context: Context) : ConfigSection(context, "filesystem") {
    val root = pref("root", Environment.getExternalStorageDirectory().toString())
    val cacheSize = 10000
    val cacheLifespan = 20 * 60 * 60 * 1000L
}
