package com.example.subsinthe.crossline

import android.support.v7.widget.SearchView
import com.example.subsinthe.crossline.streaming.IMusicSearchEngine
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.util.IObservableList
import com.example.subsinthe.crossline.util.IObservableValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.consumeEach
import java.io.Closeable

class SearchQueryListener(
    private val scope: CoroutineScope,
    private val searchResults: IObservableList<MusicTrack>,
    streamingService: IObservableValue<IStreamingService>
) : SearchView.OnQueryTextListener, Closeable {
    private lateinit var searchEngine: IMusicSearchEngine
    private val searchEngineConnection = streamingService.subscribe {
        onStreamingServiceChanged(it)
    }
    private var searchJob: Job? = null

    override fun onQueryTextChange(query: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchJob?.cancel()
        searchResults.clear()

        searchJob = scope.launch {
            searchEngine.search(query).use { iterator ->
                iterator.consumeEach { musicTrack -> searchResults.add(musicTrack) }
            }
        }
        return true
    }

    override fun close() {
        searchEngineConnection.close()
        searchJob?.cancel()
    }

    private fun onStreamingServiceChanged(streamingService: IStreamingService) {
        searchJob?.cancel()
        searchResults.clear()

        searchEngine = streamingService.searchEngine
    }
}