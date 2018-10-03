package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

private const val MESSAGE_TYPE_LENGTH = Integer.BYTES

fun deserialize(buffer: ByteBuffer): Response {
    if (buffer.remaining() < MESSAGE_TYPE_LENGTH)
        throw IllegalArgumentException("Message length is less than message type length")

    val messageCode = buffer.int
    val deserializer = DESERIALIZER_ROUTINES.get(messageCode)
            ?: throw IllegalArgumentException("Unexpected message code $messageCode")

    return deserializer(buffer)
}

private fun ByteBuffer.getString(): String {
    val length = int
    val storage = ByteArray(length)
    get(storage)
    return String(storage)
}

private val DESERIALIZER_ROUTINES = hashMapOf<Int, (ByteBuffer) -> Response>(
    1 to { buffer ->
        val isSuccess = (buffer.get().compareTo(1) == 0)
        if (isSuccess) {
            val greet = buffer.getString()
            val ip = buffer.int
            val ignored = buffer.get()
            Response.LoginSuccessful(greet, ip)
        } else
            Response.LoginFailed(reason = buffer.getString())
    }
)
