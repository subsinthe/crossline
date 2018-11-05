package com.example.subsinthe.crossline

import com.example.subsinthe.crossline.streaming.FilesystemStreamingService

class StreamingSettings {
    val filesystem = FilesystemStreamingService.Settings(
        root = "storage/0000-0000"
    )
}
