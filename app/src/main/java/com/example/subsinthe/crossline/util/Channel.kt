package com.example.subsinthe.crossline.util

import kotlinx.coroutines.channels.SendChannel

inline fun <T> SendChannel<T>.useOutput(body: () -> Unit) {
    var ex: Throwable? = null
    try {
        body()
    } catch (ex_: Throwable) {
        ex = ex_
    } finally {
        close(ex)
    }
}
