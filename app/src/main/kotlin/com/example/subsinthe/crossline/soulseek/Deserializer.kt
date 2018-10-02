package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

private const val MESSAGE_TYPE_LENGTH = Integer.BYTES

class Deserializer(val buffer: ByteBuffer) {
    init {
        if (buffer.remaining() < MESSAGE_TYPE_LENGTH)
            throw IllegalArgumentException("Message length is less than message type length")
    }

    private val deserializer = buffer.int.let {
        DESERIALIZER_ROUTINES.get(it)
            ?: throw IllegalArgumentException("Unexpected message code $it")
    }

    fun finish() = deserializer(buffer)
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
        if (isSuccess)
            Response.LoginSuccessful(
                buffer.getString(), buffer.int
            ).also { val ignored = buffer.get() }
        else
            Response.LoginFailed(buffer.getString())
    }
)
