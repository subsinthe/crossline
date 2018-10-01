package com.example.subsinthe.crossline.soulseek.upstreamMessages

import com.example.subsinthe.crossline.soulseek.DataType
import com.example.subsinthe.crossline.soulseek.Int32Data
import com.example.subsinthe.crossline.soulseek.Int8Data
import com.example.subsinthe.crossline.soulseek.StringData
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val MESSAGE_HEADER_LENGTH = 2 * Integer.BYTES

class Serializer(message: Message) {
    private val buffer =
        ByteBuffer.allocate(message.stream.fold(MESSAGE_HEADER_LENGTH) {
                sum, data -> sum + data.size
            })
            .also { it.order(ByteOrder.LITTLE_ENDIAN) }
            .also { it.putInt(it.array().size - Integer.BYTES) }
            .also { it.putInt(message.code) }

    init {
        if (buffer.array().isEmpty())
            throw IllegalArgumentException("Message '${message::class.simpleName}' stream is empty")
        message.stream.forEach { serialize(it) }
    }

    fun finish(): ByteBuffer {
        return buffer
    }

    private fun serialize(value: DataType) {
        when (value) {
            is Int8Data -> buffer.putChar(value.value)
            is Int32Data -> buffer.putInt(value.value)
            is StringData -> {
                buffer.putInt(value.value.size)
                buffer.put(value.value)
            }
        }
    }
}
