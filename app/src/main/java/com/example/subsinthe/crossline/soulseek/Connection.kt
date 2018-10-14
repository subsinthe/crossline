package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.ISocketFactory
import com.example.subsinthe.crossline.network.IStreamSocket
import com.example.subsinthe.crossline.util.Multicast
import com.example.subsinthe.crossline.util.closeOnError
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.transferTo
import com.example.subsinthe.crossline.util.useOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.logging.Logger

typealias ServerConnection = Connection<ServerRequest, ServerResponse>
typealias PeerConnection = Connection<PeerRequest, PeerResponse>

class Connection<in Request_, out Response_> private constructor(
    private val scope: CoroutineScope,
    private val socket: IStreamSocket,
    private val responseDeserializer: ResponseDeserializer<Response_>
) : IConnection<Request_, Response_> where Request_ : Request, Response_ : Response {
    private val readQueue = Channel<Response_>(RESPONSE_QUEUE_SIZE)
    private val notifier = Multicast<Response_>(scope)
    private val dispatcher = scope.actor<Response_> {
        dispatch(channel, readQueue, notifier.channel)
    }
    private val interpreter = scope.actor<ByteBuffer> { interpret(channel, dispatcher) }
    private val reader = scope.launch { read(scope, interpreter) }

    companion object {
        suspend fun server(
            scope: CoroutineScope,
            socketFactory: ISocketFactory,
            host: String,
            port: Int
        ) = socketFactory.createTcpConnection(host, port).closeOnError { socket ->
            ServerConnection(
                scope,
                socket,
                ResponseDeserializer.server()
            )
        }

        suspend fun peer(
            scope: CoroutineScope,
            socketFactory: ISocketFactory,
            host: String,
            port: Int,
            token: Long
        ) = socketFactory.createTcpConnection(host, port).closeOnError { socket ->
            PeerConnection(
                scope,
                socket,
                ResponseDeserializer.peer()
            )
        }.closeOnError { connection ->
            connection.apply { write(PeerRequest.PierceFirewall(token)) }
        }

        private val LOG = Logger.getLogger(Connection::class.java.name)

        private const val MESSAGE_LENGTH_LENGTH = DataType.I32.SIZE
        private const val MAX_ADEQUATE_MESSAGE_LENGTH = 100 * 1024 * 1024
        private const val RESPONSE_QUEUE_SIZE = 64
    }

    override suspend fun write(request: Request_) = socket.write(request.serialize())

    override suspend fun read() = readQueue.receive()

    override suspend fun subscribe(handler: suspend (Response_) -> Unit) =
        notifier.subscribe(handler)

    override fun close() = socket.close()

    suspend inline fun <reified T> subscribeTo(
        crossinline handler: suspend (T) -> Unit
    ) = subscribe { response ->
        when (response::class) { T::class -> handler(response as T) }
    }

    suspend inline fun <reified T> forEach(): ReceiveChannel<T> {
        val iterator = Channel<T>()
        val token = subscribeTo<T> { response -> iterator.send(response) }
        iterator.invokeOnClose { token.close() }
        return iterator
    }

    private suspend fun read(scope: CoroutineScope, output: SendChannel<ByteBuffer>) {
        output.useOutput {
            while (scope.isActive) {
                val buffer = socket.read()
                LOG.fine("[Reader] Read ${buffer.remaining()}")
                output.send(buffer)
            }
        }
    }

    private suspend fun interpret(
        input: ReceiveChannel<ByteBuffer>,
        output: SendChannel<Response_>
    ) {
        output.useOutput {
            var buffer = input.receive()
            while (true) {
                val messageLengthData = FixedSizeReader.read(buffer, input, MESSAGE_LENGTH_LENGTH)
                buffer = messageLengthData.leftover
                messageLengthData.product.order(DataType.BYTE_ORDER)
                val messageLength = DataType.I32.deserialize(messageLengthData.product)
                LOG.fine("[Interpreter]: New message length: $messageLength")

                if (messageLength > MAX_ADEQUATE_MESSAGE_LENGTH) {
                    LOG.warning(
                        "[Interpreter]: Message length $messageLength is larger than adequate message length"
                    )
                    continue
                }

                val messageData = FixedSizeReader.read(buffer, input, messageLength)
                buffer = messageData.leftover
                messageData.product.order(DataType.BYTE_ORDER)
                var message: Response_? = null

                if (messageLength < responseDeserializer.messageCodeLength) {
                    LOG.warning(
                        "[Interpreter]: Message length $messageLength is less than message code length"
                    )
                    continue
                }

                try {
                    message = responseDeserializer.deserialize(messageData.product)
                } catch (ex: Throwable) {
                    LOG.warning("[Interpreter] Failed to deserialize reponse: $ex")
                }
                message?.let { output.send(it) }
            }
        }
    }

    private suspend fun dispatch(
        input: ReceiveChannel<Response_>,
        output: SendChannel<Response_>,
        notifier: SendChannel<Response_>
    ) {
        notifier.useOutput {
            output.useOutput {
                input.consumeEach {
                    LOG.fine("[Dispatcher] New response: $it")
                    (if (it.isNotification) notifier else output).send(it)
                }
            }
        }
    }
}

private class FixedSizeReader {
    data class Data(val product: ByteBuffer, val leftover: ByteBuffer)

    companion object {
        suspend fun read(
            given: ByteBuffer,
            input: ReceiveChannel<ByteBuffer>,
            requested: Int
        ): Data {
            if (given.remaining() >= requested) {
                LOG.fine("Given buffer is suitable")
                return Data(given, given)
            }

            LOG.fine("Given buffer is too small. Falling back to allocation")

            val storage = ByteBuffer.allocate(requested)
            var source = given
            while (true) {
                source.transferTo(storage)
                if (!storage.hasRemaining()) {
                    storage.rewind()
                    return Data(storage, source)
                }
                source = input.receive()
                LOG.fine("Read ${source.remaining()}")
            }
        }

        val LOG = loggerFor<FixedSizeReader>()
    }
}
