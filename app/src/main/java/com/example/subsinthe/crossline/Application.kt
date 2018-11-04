package com.example.subsinthe.crossline

import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.streaming.DummyStreamingService
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.ServiceType as StreamingServiceType
import com.example.subsinthe.crossline.util.ObservableHashMap
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.createScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.android.Main

object Application {
    val uiScope = Dispatchers.Main.createScope()
    val ioScope = Dispatchers.IO.createScope()
    val socketFactory = SocketFactory(ioScope)
    val streamingSettings = StreamingSettings()
    val streamingServices = ObservableHashMap<StreamingServiceType, IStreamingService>()
    val streamingService = ObservableValue<IStreamingService>(DummyStreamingService())
    init { streamingServices.put(streamingService.value.type, streamingService.value) }
}
