package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import java.io.Closeable

interface IStreamingService : Closeable {
    data class MusicTrack(
        val title: String,
        val artist: String?,
        val album: String?
    )

    suspend fun search(query: String): AsyncIterator<MusicTrack>
}
