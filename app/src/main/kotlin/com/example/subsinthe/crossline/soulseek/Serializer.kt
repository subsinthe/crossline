package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MESSAGE_HEADER_LENGTH = 2 * Integer.BYTES

class Serializer(request: Request) {
    private val buffer =
        ByteBuffer.allocate(request.stream.fold(MESSAGE_HEADER_LENGTH) {
                sum, data -> sum + data.size
            })
            .also { it.order(ByteOrder.LITTLE_ENDIAN) }
            .also { it.putInt(it.array().size - Integer.BYTES) }
            .also { it.putInt(request.code) }

    init {
        if (buffer.array().isEmpty())
            throw IllegalArgumentException("Message '${request::class.simpleName}' stream is empty")
        request.stream.forEach { it.serialize(buffer) }
    }

    fun finish() = buffer
}
