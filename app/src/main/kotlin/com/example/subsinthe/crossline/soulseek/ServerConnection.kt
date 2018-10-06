package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.IStreamSocket
import com.example.subsinthe.crossline.util.Multicast
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.transferTo
import com.example.subsinthe.crossline.util.useOutput
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import java.io.Closeable
import java.nio.ByteBuffer

private const val MESSAGE_LENGTH_LENGTH = DataType.I32.SIZE
private const val RESPONSE_QUEUE_SIZE = 64

class ServerConnection(
    private val scope: CoroutineScope,
    private val socket: IStreamSocket
) : Closeable {
    private val readQueue = Channel<Response>(RESPONSE_QUEUE_SIZE)
    private val notifier = Multicast<Response>(scope, RESPONSE_QUEUE_SIZE)
    private val dispatcher = scope.actor<Response> {
        dispatch(channel, readQueue, notifier.channel)
    }
    private val interpreter = scope.actor<ByteBuffer> { interpret(channel, dispatcher) }
    private val reader = scope.launch { read(scope, interpreter) }

    suspend fun make_request(request: Request): Response {
        socket.write(request.serialize())
        return readQueue.receive()
    }

    suspend fun subscrbe(handler: suspend (Response) -> Unit) = notifier.subscribe(handler)

    override fun close() = socket.close()

    private companion object { val LOG = loggerFor<ServerConnection>() }

    private suspend fun read(scope: CoroutineScope, output: SendChannel<ByteBuffer>) {
        output.useOutput {
            while (scope.isActive) {
                val buffer = socket.read()
                LOG.fine("Read ${buffer.remaining()}")
                output.send(buffer)
            }
        }
    }

    private suspend fun interpret(
        input: ReceiveChannel<ByteBuffer>,
        output: SendChannel<Response>
    ) {
        output.useOutput {
            var buffer = input.receive()
            while (true) {
                val messageLengthData = FixedSizeReader.read(buffer, input, MESSAGE_LENGTH_LENGTH)
                buffer = messageLengthData.leftover
                messageLengthData.product.order(DataType.BYTE_ORDER)
                val messageLength = DataType.I32.deserialize(messageLengthData.product)
                if (messageLength == 0)
                    throw IllegalArgumentException("Unexpected message length: $messageLength")
                LOG.fine("New message length: $messageLength")

                val messageData = FixedSizeReader.read(buffer, input, messageLength)
                buffer = messageData.leftover
                var message: Response? = null
                try {
                    message = Response.deserialize(messageData.product)
                } catch (ex: Throwable) {
                    LOG.warning("Failed to deserialize reponse: $ex")
                }
                message?.let { output.send(it) }
            }
        }
    }

    private suspend fun dispatch(
        input: ReceiveChannel<Response>,
        output: SendChannel<Response>,
        notifier: SendChannel<Response>
    ) {
        notifier.useOutput {
            output.useOutput {
                input.consumeEach {
                    LOG.fine("New response: $it")
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
            if (given.remaining() >= requested)
                return Data(given, given)

            val storage = ByteBuffer.allocate(requested)
            var source = given
            while (true) {
                source.transferTo(storage)
                if (!storage.hasRemaining()) {
                    storage.rewind()
                    return Data(storage, source)
                }
                source = input.receive()
            }
        }
    }
}
