package com.example.subsinthe.crossline.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

private class DefaultExceptionHandler {
    companion object {
        fun get() = CoroutineExceptionHandler { _: CoroutineContext, ex: Throwable ->
            val stackTrace = ex.stackTrace.fold("") { trace, frame -> "$trace\n$frame" }
            LOG.severe("Uncaught exception: $ex:$stackTrace")
        }

        private val LOG = loggerFor<DefaultExceptionHandler>()
    }
}

fun CoroutineDispatcher.createScope(
    exceptionHandler: CoroutineExceptionHandler = DefaultExceptionHandler.get()
) = object : CoroutineScope {
    override val coroutineContext: CoroutineContext = this@createScope + exceptionHandler
}
