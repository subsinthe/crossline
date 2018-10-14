package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

abstract class Request {
    abstract val code: Int
    abstract val codeLength: Int
    abstract val stream: Iterable<DataType>

    fun serialize(): ByteBuffer {
        val codeData = when (codeLength) {
            DataType.I8.SIZE -> DataType.I8(code.toByte())
            DataType.I32.SIZE -> DataType.I32(code)
            else -> throw AssertionError("Unexpected code length: $codeLength")
        }
        val messageLength = stream.fold(codeData.size) { sum, data -> sum + data.size }
        val messageLengthData = DataType.I32(messageLength)
        val buffer = ByteBuffer.allocate(messageLengthData.size + messageLength)
        buffer.order(DataType.BYTE_ORDER)

        messageLengthData.serialize(buffer)
        codeData.serialize(buffer)
        stream.forEach { it.serialize(buffer) }

        return buffer
    }
}

sealed class ServerRequest : Request() {
    override val codeLength = DataType.I32.SIZE

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

sealed class PeerRequest : Request() {
    override val codeLength = DataType.I32.SIZE

    class PierceFirewall(token: Long) : PeerRequest() {
        override val code = 0
        override val codeLength = DataType.I8.SIZE

        override val stream: Iterable<DataType> = listOf(DataType.U32(token))
    }
}
