package com.example.subsinthe.crossline.util

import kotlinx.coroutines.channels.ReceiveChannel

class AsyncIterator<T>(val iterator: ReceiveChannel<T>) {
    inline fun <R> use(block: (ReceiveChannel<T>) -> R): R {
        try {
            return block(iterator)
        } finally {
            iterator.cancel()
        }
    }
}
