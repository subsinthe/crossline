package com.example.subsinthe.crossline.util

import java.io.Closeable

class TokenPool : Closeable {
    private val impl_ = ArrayList<Token>()

    override fun close() { impl_.forEach { it.close() } }

    operator fun plusAssign(token: Token) { impl_.add(token) }
}
