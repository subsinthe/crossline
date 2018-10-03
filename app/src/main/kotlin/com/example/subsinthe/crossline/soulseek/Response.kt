package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

sealed class Response {
    companion object {
        private const val MESSAGE_TYPE_LENGTH = DataType.I32.SIZE

        fun deserialize(buffer: ByteBuffer): Response {
            if (buffer.remaining() < MESSAGE_TYPE_LENGTH)
                throw IllegalArgumentException("Message length is less than message type length")

            val messageCode = DataType.I32.deserialize(buffer)
            val deserializer = DESERIALIZER_ROUTINES.get(messageCode)
                ?: throw IllegalArgumentException("Unexpected message code $messageCode")

            return deserializer(buffer)
        }
    }

    data class LoginSuccessful(val greet: String, val ip: Int) : Response()
    data class LoginFailed(val reason: String) : Response()
}

private val DESERIALIZER_ROUTINES = hashMapOf<Int, (ByteBuffer) -> Response>(
    1 to { buffer ->
        val isSuccess = (DataType.I8.deserialize(buffer).compareTo(1) == 0)
        if (isSuccess) {
            val greet = DataType.Str.deserialize(buffer)
            val ip = DataType.I32.deserialize(buffer)
            val ignored = DataType.I8.deserialize(buffer)
            Response.LoginSuccessful(greet, ip)
        } else
            Response.LoginFailed(reason = DataType.Str.deserialize(buffer))
    }
)
