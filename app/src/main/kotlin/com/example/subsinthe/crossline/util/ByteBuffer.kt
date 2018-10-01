package com.example.subsinthe.crossline.util

import java.nio.ByteBuffer

fun ByteBuffer.transferTo(other: ByteBuffer): Int {
    val toCopy = minOf(remaining(), other.remaining())
    get(other.array(), other.position(), toCopy)
    other.position(other.position() + toCopy)
    return toCopy
}
