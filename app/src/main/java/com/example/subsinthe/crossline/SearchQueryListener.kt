package com.example.subsinthe.crossline

import android.support.v7.widget.SearchView
import android.view.View
import com.example.subsinthe.crossline.streaming.IStreamingService
import com.example.subsinthe.crossline.streaming.MusicTrack
import com.example.subsinthe.crossline.util.IObservable
import com.example.subsinthe.crossline.util.ObservableValue
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.MutableCollection
import java.io.Closeable

class SearchQueryListener(
    streamingService_: IObservable<IStreamingService>,
    private val scope: CoroutineScope,
    private val searchResults: MutableCollection<MusicTrack>,
    private val searchMore: ReceiveChannel<Unit>,
    private val loadBatchSize: Int,
    private val delayOnQueryChange: Int
) : SearchView.OnQueryTextListener, View.OnAttachStateChangeListener, Closeable {
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
        return false
    }

    override fun onViewAttachedToWindow(@Suppress("UNUSED_PARAMETER") v: View) = Unit

    override fun onViewDetachedFromWindow(@Suppress("UNUSED_PARAMETER") v: View) {
        searchJobHandle?.cancel()
        searchResults.clear()
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
                delay(delayOnQueryChange)
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
