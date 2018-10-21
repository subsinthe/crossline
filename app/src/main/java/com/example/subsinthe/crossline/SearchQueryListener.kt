package com.example.subsinthe.crossline

import android.support.v7.widget.SearchView
import com.example.subsinthe.crossline.util.IStreamingService
import com.example.subsinthe.crossline.util.IObservableList
import com.example.subsinthe.crossline.util.IObservableValue
import com.example.subsinthe.crossline.util.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.consumeEach
import java.io.Closeable

class SearchQueryListener private constructor(
    private val scope: CoroutineScope,
    private val searchResults: IObservableList<MusicTrack>
) : SearchView.OnQueryTextListener, Closeable {
    private lateinit var searchEngine: IMusicSearchEngine
    private lateinit var searchEngineConnection: Token
    private var searchJob: Job? = null

    companion object {
        suspend fun build(
            scope: CoroutineScope,
            searchResults: IObservableList<MusicTrack>,
            streamingService: IObservableValue<IStreamingService>
        ) = SearchQueryListener(scope, searchResults).apply {
            searchEngineConnection = streamingService.subscribe { onStreamingServiceChanged(it) }
        }
    }

    override fun onQueryTextChange(query: String): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchJob?.cancel()

        searchJob = scope.launch {
            searchResults.clear()

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

    private suspend fun onStreamingServiceChanged(streamingService: IStreamingService) {
        searchJob?.cancel()
        searchResults.clear()

        searchEngine = streamingService.searchEngine
    }
}
