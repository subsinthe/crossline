package com.example.subsinthe.crossline.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutineScopeLogger {
    companion object { val LOG = loggerFor<CoroutineScopeLogger>() }
}

fun <T> CoroutineScope.pack(callable: suspend (T) -> Unit) = {
    it: T -> launch { callable(it) }.discard()
}

fun CoroutineScope.setTimer(interval: Long, body: suspend () -> Unit) = launch {
    while (true) {
        CoroutineScopeLogger.LOG.try_("Uncaught exception in timer") { body() }
        delay(interval)
    }
}
