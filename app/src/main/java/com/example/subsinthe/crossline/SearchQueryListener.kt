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
    private var searchJobHandle: Job? = null
    private val connection = streamingService_.subscribe { onStreamingServiceChanged(it) }

    val isSearchActive: IObservable<Boolean> = _isSearchActive

    override fun onQueryTextChange(query: String): Boolean {
        search(query, doDelay = true)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        search(query, doDelay = false)
        return true
    }

    override fun close() {
        connection.close()
        searchJobHandle?.cancel()
    }

    private fun onStreamingServiceChanged(streamingService_: IStreamingService) {
        searchJobHandle?.cancel()
        searchResults.clear()

        streamingService = streamingService_
    }

    private fun search(query: String, doDelay: Boolean) {
        if (query.isEmpty())
            return

        searchJobHandle?.cancel()
        searchResults.clear()

        searchJobHandle = scope.launch {
            if (doDelay)
                delay(searchDelayOnQueryChange)
            searchJob(query)
        }
    }

    private suspend fun searchJob(query: String) {
        _isSearchActive.value = true
        try {
            streamingService.search(query).use { iterator ->
                while (true) {
                    for (i in 0 until loadBatchSize)
                        searchResults.add(iterator.receive())
                    searchMore.receive()
                }
            }
        } finally {
            _isSearchActive.value = false
        }
    }
}
