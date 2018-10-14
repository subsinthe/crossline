package com.example.subsinthe.crossline.util

import java.io.Closeable

inline fun <T : Closeable?, R> T.closeOnError(block: (T) -> R): R {
    try {
        return block(this)
    } catch (ex: Throwable) {
        this?.close()
        throw ex
    }
}
