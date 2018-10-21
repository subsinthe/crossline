package com.example.subsinthe.crossline.util

import kotlinx.coroutines.channels.ReceiveChannel
import java.io.Closeable

class AsyncIterator<T>(val iterator: ReceiveChannel<T>) {
    val _closeable = object : Closeable { override fun close() { iterator.cancel() } }

    inline fun <R> use(block: (ReceiveChannel<T>) -> R) = _closeable.use { block(iterator) }

    inline fun <R> closeOnError(block: (ReceiveChannel<T>) -> R) = _closeable.closeOnError {
        block(iterator)
    }
}
