package com.example.subsinthe.crossline.util

import kotlinx.coroutines.experimental.channels.SendChannel

suspend fun <T> SendChannel<T>.useOutput(body: suspend () -> Unit) {
    var ex: Throwable? = null
    try {
        body()
    } catch (ex_: Throwable) {
        ex = ex_
    } finally {
        close(ex)
    }
}
