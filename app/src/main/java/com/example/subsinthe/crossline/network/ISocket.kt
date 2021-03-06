package com.example.subsinthe.crossline.network

import java.io.Closeable
import java.nio.ByteBuffer

interface ISocketFactory : Closeable {
    suspend fun createTcpConnection(host: String, port: Int): IStreamSocket
}

interface IStreamSocket : Closeable {
    suspend fun read(): ByteBuffer
    suspend fun write(buffer: ByteBuffer)
}
