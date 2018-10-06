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
import java.util.logging.Logger

private const val MESSAGE_LENGTH_LENGTH = DataType.I32.SIZE
private const val RESPONSE_QUEUE_SIZE = 64
private const val READ_BUFFER_SIZE = 1 * 1024 * 1024

class ServerConnection(
    private val scope: CoroutineScope,
    private val socket: IStreamSocket
) : Closeable {
    private val readQueue = Channel<Response>(RESPONSE_QUEUE_SIZE)
    private val notifier = Multicast<Response>(scope, RESPONSE_QUEUE_SIZE)
    private val dispatcher = scope.actor<Response> {
        dispatch(channel, readQueue, notifier.channel)
    }
    private val reader = scope.launch { read(scope, dispatcher, READ_BUFFER_SIZE) }

    suspend fun make_request(request: Request): Response {
        socket.write(request.serialize())
        return readQueue.receive()
    }

    suspend fun subscrbe(handler: suspend (Response) -> Unit) = notifier.subscribe(handler)

    override fun close() = socket.close()

    private companion object {
        private val LOG: Logger = loggerFor<ServerConnection>()
    }

    private suspend fun read(
        scope: CoroutineScope,
        output: SendChannel<Response>,
        bufferSize: Int
    ) {
        output.useOutput {
        val buffer = ByteBuffer.allocate(maxOf(bufferSize, MESSAGE_LENGTH_LENGTH))
        val interpreter = DataInterpreter(output)
        while (scope.isActive) {
            val bytesRead = socket.read(buffer)
            buffer.limit(bytesRead)
            buffer.rewind()
            try {
                interpreter.consume(buffer)
            } catch (ex: Throwable) {
                LOG.warning("Failed to consume read data: $ex")
            }
            buffer.clear()
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
            if (it.isNotification)
                notifier.send(it)
            else
                output.send(it)
        }
        }
        }
    }
}

private class DataInterpreter(private val output: SendChannel<Response>) {
    private var state: State = State.Vanilla()

    private sealed class State {
        class Vanilla : State()

        class CollectingMessageLength : State() {
            val storage: ByteBuffer = ByteBuffer.allocate(MESSAGE_LENGTH_LENGTH)
        }

        class CollectingMessage(messageLength: Int) : State() {
            val storage: ByteBuffer = ByteBuffer.allocate(messageLength).also {
                if (messageLength == 0)
                    throw IllegalArgumentException("Unexpected message length: $messageLength")
            }
        }
    }

    suspend fun consume(buffer: ByteBuffer) {
        buffer.order(DataType.BYTE_ORDER)
        while (buffer.hasRemaining()) {
            state = doConsume(state, buffer)
        }
    }

    private companion object {
        private val LOG: Logger = loggerFor<DataInterpreter>()
    }

    private suspend fun doConsume(state: State, buffer: ByteBuffer): State {
        when (state) {
            is State.Vanilla -> {
                if (buffer.remaining() < MESSAGE_LENGTH_LENGTH)
                    return State.CollectingMessageLength()
                return State.CollectingMessage(DataType.I32.deserialize(buffer))
            }
            is State.CollectingMessageLength -> {
                buffer.transferTo(state.storage)
                if (!state.storage.hasRemaining()) {
                    state.storage.rewind()
                    return State.CollectingMessage(DataType.I32.deserialize(state.storage))
                }
            }
            is State.CollectingMessage -> {
                buffer.transferTo(state.storage)
                if (!state.storage.hasRemaining()) {
                    state.storage.rewind()
                    try {
                        output.send(Response.deserialize(state.storage))
                    } catch (ex: Throwable) {
                        LOG.warning("Failed to deserialize message: $ex")
                    }
                    return State.Vanilla()
                }
            }
        }
        return state
    }
}
