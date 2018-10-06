package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

class UnexpectedMessageCodeException(code: Int) : Exception("Unexpected message code $code")

sealed class Response {
    abstract val isNotification: Boolean

    companion object {
        private const val MESSAGE_TYPE_LENGTH = DataType.I32.SIZE

        fun deserialize(buffer: ByteBuffer): Response {
            if (buffer.remaining() < MESSAGE_TYPE_LENGTH)
                throw IllegalArgumentException("Message length is less than message type length")

            buffer.order(DataType.BYTE_ORDER)

            val messageCode = DataType.I32.deserialize(buffer)
            val deserializer = DESERIALIZER_ROUTINES.get(messageCode)
                ?: throw UnexpectedMessageCodeException(messageCode)

            return deserializer(buffer)
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
    }

    data class LoginSuccessful(val greet: String, val ip: Int) : Response() {
        override val isNotification = false
    }
    data class LoginFailed(val reason: String) : Response() {
        override val isNotification = false
    }
}
