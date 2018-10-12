package com.example.subsinthe.crossline

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.subsinthe.crossline.util.AndroidLoggingHandler
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.android.Main
import kotlin.coroutines.experimental.CoroutineContext
import com.example.subsinthe.crossline.network.VertxSocketFactory as SocketFactory

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

class MainActivity : AppCompatActivity() {
    private val uiScope = UiScope()
    private val ioScope = IoScope()
    private val socketFactory = SocketFactory(ioScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
    }

    override fun onDestroy() {
        super.onDestroy()

        socketFactory.close()
    }
}
