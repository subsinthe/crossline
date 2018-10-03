package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MESSAGE_HEADER_LENGTH = 2 * Integer.BYTES

fun serialize(request: Request): ByteBuffer {
    val buffer = ByteBuffer.allocate(request.stream.fold(MESSAGE_HEADER_LENGTH) {
            sum, data -> sum + data.size
        })
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(buffer.array().size - Integer.BYTES)
    buffer.putInt(request.code)

    if (buffer.array().isEmpty())
        throw IllegalArgumentException("Message '${request::class.simpleName}' stream is empty")

    request.stream.forEach { it.serialize(buffer) }

    return buffer
}
