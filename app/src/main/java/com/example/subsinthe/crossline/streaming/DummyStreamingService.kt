package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import kotlinx.coroutines.channels.Channel

class DummyStreamingService() : IStreamingService {
    override val type = ServiceType.Dummy

    override fun close() {}

    override suspend fun search(query: String): AsyncIterator<MusicTrack> {
        val iterator = Channel<MusicTrack>()
        iterator.close()
        return AsyncIterator(iterator)
    }
}
