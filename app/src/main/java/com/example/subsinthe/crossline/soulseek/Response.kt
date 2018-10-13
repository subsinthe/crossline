package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

class UnexpectedMessageCodeException(code: Int) : Exception("Unexpected message code $code")

sealed class Response {
    abstract val isNotification: Boolean

    companion object {
        const val MESSAGE_CODE_LENGTH = DataType.I32.SIZE
        const val MAX_ADEQUATE_MESSAGE_CODE = 1001

        fun deserialize(buffer: ByteBuffer): Response {
            if (buffer.remaining() < MESSAGE_CODE_LENGTH)
                throw IllegalArgumentException("Message length is less than message code length")

            if (buffer.order() != DataType.BYTE_ORDER)
                throw IllegalArgumentException("Wrong byte order")

            val messageCode = DataType.I32.deserialize(buffer)
            if (messageCode > MAX_ADEQUATE_MESSAGE_CODE)
                throw IllegalArgumentException("Not an adequate message code: $messageCode")

            val deserializer = DESERIALIZER_ROUTINES.get(messageCode)
                ?: throw UnexpectedMessageCodeException(messageCode)

            return deserializer(buffer)
        }

        private val DESERIALIZER_ROUTINES = hashMapOf<Int, (ByteBuffer) -> Response>(
            1 to { buffer ->
                val isSuccess = DataType.Bool.deserialize(buffer)
                if (isSuccess)
                    Response.LoginSuccessful.deserialize(buffer)
                else
                    Response.LoginFailed.deserialize(buffer)
            },
            9 to { buffer -> SearchReply.deserialize(buffer) }
        )
    }

    data class LoginSuccessful(val greet: String, val ip: Int) : Response() {
        override val isNotification = false

        companion object {
            fun deserialize(buffer: ByteBuffer) = LoginSuccessful(
                greet = DataType.Str.deserialize(buffer),
                ip = DataType.I32.deserialize(buffer).also {
                    DataType.I8.deserialize(buffer)
                }
            )
        }
    }

    data class LoginFailed(val reason: String) : Response() {
        override val isNotification = false

        companion object {
            fun deserialize(buffer: ByteBuffer) = LoginFailed(
                reason = DataType.Str.deserialize(buffer)
            )
        }
    }

    data class SearchReply(
        val user: String,
        val ticket: Int,
        val results: ArrayList<Result>,
        val slotfree: Boolean,
        val avgspeed: Int,
        val queueLength: Long
    ) : Response() {
        override val isNotification = true

        companion object {
            fun deserialize(buffer: ByteBuffer) = SearchReply(
                user = DataType.Str.deserialize(buffer),
                ticket = DataType.I32.deserialize(buffer),
                results = DataType.List.deserialize<Result>({ Result.deserialize(it) }, buffer),
                slotfree = DataType.Bool.deserialize(buffer),
                avgspeed = DataType.I32.deserialize(buffer),
                queueLength = DataType.I64.deserialize(buffer)
            )
        }
        data class Result(
            val filename: String,
            val size: Long,
            val ext: String,
            val attributes: ArrayList<Attributes>
        ) {
            companion object {
                fun deserialize(buffer: ByteBuffer) = Result(
                    filename = DataType.Str.deserialize(buffer),
                    size = DataType.I64.deserialize(buffer),
                    ext = DataType.Str.deserialize(buffer),
                    attributes = DataType.List.deserialize<Attributes>(
                        { Attributes.deserialize(it) }, buffer
                    )
                )
            }
            data class Attributes(val placeInAttributes: Int, val attribute: Int) {
                companion object {
                    fun deserialize(buffer: ByteBuffer) = Attributes(
                        placeInAttributes = DataType.I32.deserialize(buffer),
                        attribute = DataType.I32.deserialize(buffer)
                    )
                }
            }
        }
    }
}
