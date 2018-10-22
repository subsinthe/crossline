package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.useOutput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private typealias MusicTrack = IStreamingService.MusicTrack

class FilesystemStreamingService(private val scope: CoroutineScope) : IStreamingService {
    override fun close() {}

    override suspend fun search(query: String): AsyncIterator<MusicTrack> {
        LOG.info("search($query)")

        val iterator = Channel<MusicTrack>()
        scope.launch {
            iterator.useOutput {
                iterator.send(MusicTrack("Anthony Linell", "Dissolvement"))
                delay(2000)
                iterator.send(MusicTrack("Anthony Linell", "As They Withdraw"))
                delay(2000)
                iterator.send(MusicTrack("Abdulla Rashim", "Weldiya"))
                delay(2000)
                iterator.send(MusicTrack("Lundin Oil", "Between The Shields"))
                delay(2000)
                iterator.send(MusicTrack("Ulwhednar", "Modern Silver"))
            }
        }
        return AsyncIterator(iterator)
    }

    private companion object {
        val LOG = loggerFor<FilesystemStreamingService>()
    }
}
