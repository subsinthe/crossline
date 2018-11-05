package com.example.subsinthe.crossline

import android.content.Context
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.streaming.DummyStreamingService
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.FilesystemStreamingService
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.ObservableHashMap
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.createScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.android.Main

object Application {
    private var initialized = false

    lateinit var config: Config
    val uiScope = Dispatchers.Main.createScope()
    val ioScope = Dispatchers.IO.createScope()
    val socketFactory = SocketFactory(ioScope)
    val streamingServices = ObservableHashMap<StreamingServiceType, IStreamingService>()
    val streamingService = ObservableValue<IStreamingService>(DummyStreamingService())
    init { streamingServices.put(streamingService.value.type, streamingService.value) }

    fun initialize(context: Context, permissionListener: PermissionListener) {
        if (initialized)
            return

        config = Config(context)

        permissionListener.requestReadExternalStorage {
            val service = FilesystemStreamingService(
                scope = uiScope,
                root = config.filesystem.root,
                cacheSize = config.filesystem.cacheSize,
                cacheLifespan = config.filesystem.cacheLifespan
            )
            streamingServices.put(service.type, service)
            if (streamingService.value.type == StreamingServiceType.Dummy)
                streamingService.value = service
        }

        initialized = true
    }
}
