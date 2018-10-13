package com.example.subsinthe.crossline.util

import java.io.Closeable

class Token(private var impl: Closeable? = null) : Closeable {
    constructor (closer: () -> Unit) : this(object : Closeable { override fun close() = closer() })

    override fun close() {
        impl?.close()
        impl = null
    }
}
