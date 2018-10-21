package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import java.io.Closeable

data class MusicTrack(
    val artist: String,
    val title: String
)

interface IMusicSearchEngine {
    suspend fun search(query: String): AsyncIterator<MusicTrack>
}

interface IStreamingService : Closeable {
    val searchEngine: IMusicSearchEngine
}
