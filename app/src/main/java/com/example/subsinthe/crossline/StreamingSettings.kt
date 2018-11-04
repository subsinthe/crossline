package com.example.subsinthe.crossline

import android.os.Environment
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService

class StreamingSettings {
    val filesystem = FilesystemStreamingService.Settings(
        root = Environment.getExternalStorageDirectory().toString()
    )
}
