package com.example.subsinthe.crossline.soulseek

import kotlin.collections.HashMap
import java.nio.ByteBuffer

class UnexpectedMessageCodeException(code: Int) : Exception("Unexpected message code $code")

interface Response {
    val isNotification: Boolean
}

sealed class ServerResponse : Response {
    data class LoginSuccessful(val greet: String, val ip: Int) : ServerResponse() {
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

    data class LoginFailed(val reason: String) : ServerResponse() {
        override val isNotification = false

        companion object {
            fun deserialize(buffer: ByteBuffer) = LoginFailed(
                reason = DataType.Str.deserialize(buffer)
            )
        }
    }
}

sealed class PeerInitResponse : Response

sealed class PeerResponse : Response {
    data class SearchReply(
        val user: String,
        val ticket: Int,
        val results: ArrayList<Result>,
        val slotfree: Boolean,
        val avgspeed: Int,
        val queueLength: Long
    ) : PeerResponse() {
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

class ResponseDeserializer<out Response_> private constructor(
    val messageCodeLength: Int,
    private val deserializers: HashMap<Int, (ByteBuffer) -> Response_>
) where Response_ : Response {
    private val messageCodeDeserializer: (ByteBuffer) -> Int = when (messageCodeLength) {
        DataType.I32.SIZE -> { { DataType.I32.deserialize(it) } }
        DataType.I8.SIZE -> { { DataType.I8.deserialize(it).toInt() } }
        else -> throw IllegalArgumentException("Unexpected message code length: $messageCodeLength")
    }
    fun deserialize(buffer: ByteBuffer): Response_ {
        if (buffer.order() != DataType.BYTE_ORDER)
            throw IllegalArgumentException("Wrong byte order")

        if (buffer.remaining() < messageCodeLength)
            throw IllegalArgumentException("Message length is less than message code length")

        val messageCode = messageCodeDeserializer(buffer)
        val deserializer = deserializers.get(messageCode)
            ?: throw UnexpectedMessageCodeException(messageCode)

        return deserializer(buffer)
    }

    companion object {
        fun server() = ResponseDeserializer<ServerResponse>(4, SERVER_DESERIALIZERS)
        fun peerInit() = ResponseDeserializer<PeerInitResponse>(1, PEER_INIT_DESERIALIZERS)
        fun peer() = ResponseDeserializer<PeerResponse>(4, PEER_DESERIALIZERS)

        private val SERVER_DESERIALIZERS = hashMapOf<Int, (ByteBuffer) -> ServerResponse>(
            1 to { buffer ->
                val isSuccess = DataType.Bool.deserialize(buffer)
                if (isSuccess)
                    ServerResponse.LoginSuccessful.deserialize(buffer)
                else
                    ServerResponse.LoginFailed.deserialize(buffer)
            }
        )

        private val PEER_INIT_DESERIALIZERS = hashMapOf<Int, (ByteBuffer) -> PeerInitResponse>()

        private val PEER_DESERIALIZERS = hashMapOf<Int, (ByteBuffer) -> PeerResponse>(
            9 to { buffer -> PeerResponse.SearchReply.deserialize(buffer) }
        )
    }
}
