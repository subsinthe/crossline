package com.example.subsinthe.crossline.network

import kotlinx.coroutines.experimental.CoroutineScope
import java.io.Closeable
import java.nio.ByteBuffer

interface ISocketFactory : Closeable {
    val coroutineScope: CoroutineScope

    suspend fun createTcpConnection(host: String, port: Int): IStreamSocket
}

interface IStreamSocket : Closeable {
    suspend fun read(buffer: ByteBuffer): Int
    suspend fun write(buffer: ByteBuffer)
}
