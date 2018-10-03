package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.IStreamSocket
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.transferTo
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.launch
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.logging.Logger

private class ConnectionClosedException : Exception("Connection closed")

private const val MESSAGE_LENGTH_LENGTH = DataType.I32.SIZE

class ServerSocket(
    private val ioScope: CoroutineScope,
    private val socket: IStreamSocket,
    bufferSize: Int
) : Closeable {
    private val readQueue = Channel<Response>()
    private val reader = ioScope.launch { readerFunc(ioScope, bufferSize) }

    override fun close() {
        reader.cancel()
        socket.close()
    }

    suspend fun write(request: Request) = socket.write(request.serialize())

    suspend fun read() = readQueue.receive()

    private companion object {
        private val LOG: Logger = loggerFor<ServerSocket>()
    }

    private suspend fun readerFunc(scope: CoroutineScope, bufferSize: Int) {
        var closeException: Throwable? = null
        val buffer = ByteBuffer.allocate(maxOf(bufferSize, MESSAGE_LENGTH_LENGTH))
        val interpreter = DataInterpreter(readQueue)
        try {
            while (scope.isActive) {
                val bytesRead = socket.read(buffer)
                if (bytesRead == -1) {
                    closeException = ConnectionClosedException()
                    break
                }
                buffer.limit(bytesRead)
                buffer.rewind()
                try {
                    interpreter.consume(buffer)
                } catch (throwable: Throwable) {
                    LOG.warning("Failed to consume read data: $throwable")
                    throwable.printStackTrace()
                }
                buffer.clear()
            }
        } catch (throwable: Throwable) {
            closeException = throwable
        } finally {
            readQueue.close(closeException)
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
                    } catch (throwable: Throwable) {
                        LOG.warning("Failed to deserialize message: $throwable")
                        throwable.printStackTrace()
                    }
                    return State.Vanilla()
                }
            }
        }
        return state
    }
}
