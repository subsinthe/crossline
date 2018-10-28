package com.example.subsinthe.crossline

import android.os.Environment
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService
import com.example.subsinthe.crossline.streaming.ServiceType
import com.example.subsinthe.crossline.util.IObservableSet
import com.example.subsinthe.crossline.util.ObservableHashSet

class StreamingSettings {
    val filesystem = FilesystemStreamingService.Settings(
        root = Environment.getExternalStorageDirectory().toString()
    )
    val serviceTypes: IObservableSet<ServiceType> = ObservableHashSet<ServiceType>().apply {
        add(ServiceType.Filesystem)
    }
}
