package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

abstract class Request {
    abstract val code: Int
    abstract val stream: Iterable<DataType>

    private companion object {
        const val HEADER_LENGTH = 2 * DataType.I32.SIZE
    }

    fun serialize(): ByteBuffer {
        val bufferSize = stream.fold(HEADER_LENGTH) { sum, data -> sum + data.size }
        val buffer = ByteBuffer.allocate(bufferSize).also { it.order(DataType.BYTE_ORDER) }

        DataType.build(bufferSize - DataType.I32.SIZE).serialize(buffer)
        DataType.build(code).serialize(buffer)
        stream.forEach { it.serialize(buffer) }

        return buffer
    }
}

sealed class ServerRequest : Request() {
    class Login(username: String, password: String, digest: String, version: Int, minorVersion: Int)
            : ServerRequest() {
        override val code = 1

        override val stream: Iterable<DataType> = listOf(
            DataType.build(username),
            DataType.build(password),
            DataType.build(version),
            DataType.build(digest),
            DataType.build(minorVersion)
        )
    }

    class FileSearch(ticket: Int, query: String) : ServerRequest() {
        override val code = 26

        override val stream: Iterable<DataType> = listOf(
            DataType.build(ticket), DataType.build(query)
        )
    }
}

sealed class PeerRequest : Request()
