package com.example.subsinthe.crossline

import android.support.v7.widget.SearchView
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.util.IObservableList
import com.example.subsinthe.crossline.util.IObservable
import com.example.subsinthe.crossline.util.ObservableValue
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable

class SearchQueryListener(
    streamingService_: IObservable<IStreamingService>,
    private val scope: CoroutineScope,
    private val searchResults: IObservableList<MusicTrack>,
    private val searchMore: ReceiveChannel<Unit>,
    private val loadBatchSize: Int,
    private val searchDelayOnQueryChange: Int
) : SearchView.OnQueryTextListener, Closeable {
    private val _isSearchActive = ObservableValue<Boolean>(false)
    private lateinit var streamingService: IStreamingService
    private var searchJob: Job? = null
    private val connection = streamingService_.subscribe { onStreamingServiceChanged(it) }

    val isSearchActive: IObservable<Boolean> = _isSearchActive

    override fun onQueryTextChange(query: String): Boolean {
        searchJob?.cancel()
        searchResults.clear()

        searchJob = scope.launch {
            delay(searchDelayOnQueryChange)
            search(query)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchJob?.cancel()
        searchResults.clear()

        searchJob = scope.launch { search(query) }
        return true
    }

    override fun close() {
        connection.close()
        searchJob?.cancel()
    }

    private fun onStreamingServiceChanged(streamingService_: IStreamingService) {
        searchJob?.cancel()
        searchResults.clear()

        streamingService = streamingService_
    }

    private suspend fun search(query: String) {
        _isSearchActive.value = true
        try {
            streamingService.search(query).use { iterator ->
                while (true) {
                    for (i in 0..(loadBatchSize - 1))
                        searchResults.add(iterator.receive())
                    searchMore.receive()
                }
            }
        } finally {
            _isSearchActive.value = false
        }
    }
}
