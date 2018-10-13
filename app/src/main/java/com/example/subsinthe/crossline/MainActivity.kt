package com.example.subsinthe.crossline

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.widget.SearchView
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory
import com.example.subsinthe.crossline.soulseek.Client as SoulseekClient
import com.example.subsinthe.crossline.soulseek.Credentials

private class DefaultExceptionHandler {
    companion object {
        fun get() = CoroutineExceptionHandler { _: CoroutineContext, ex: Throwable ->
            val stackTrace = ex.stackTrace.fold("") { trace, frame -> "$trace\n$frame" }
            LOG.severe("Uncaught exception: $ex:$stackTrace")
        }

        private val LOG = loggerFor<DefaultExceptionHandler>()
    }
}

private class UiScope : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        Dispatchers.Main + DefaultExceptionHandler.get()
}

private class IoScope : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        Dispatchers.IO + DefaultExceptionHandler.get()
}

class MainActivity : Activity() {
    private val uiScope = UiScope()
    private val ioScope = IoScope()
    private val socketFactory = SocketFactory(ioScope)
    private val credentials = Credentials(username = "u", password = "p")
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())

        setContentView(R.layout.activity_main)

        val searchView = findViewById<SearchView>(R.id.search_view)
        val recyclerView = findViewById<RecyclerView>(R.id.search_results)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchJob?.cancel()

                searchJob = uiScope.launch {
                    SoulseekClient.build(uiScope, socketFactory).use { client ->
                        client.login(credentials)
                        var iter: ReceiveChannel<String>? = null
                        try {
                            iter = client.fileSearch(query)
                            iter.consumeEach {}
                        } finally {
                            iter?.cancel()
                        }
                    }
                }

                return true
            }

            override fun onQueryTextChange(query: String) = true
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        searchJob?.cancel()
        socketFactory.close()
    }
}
