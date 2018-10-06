package com.example.subsinthe.crossline.network

import com.example.subsinthe.crossline.util.transferTo
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetClientOptions
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.sendBlocking
import java.nio.ByteBuffer

class VertxSocketFactory(val coroutineScope: CoroutineScope) : ISocketFactory {
    init { System.setProperty("vertx.disableFileCPResolving", "true") }
    private val vertxContext = Vertx.vertx()

    override fun close() = vertxContext.close()

    override suspend fun createTcpConnection(host: String, port: Int): IStreamSocket {
        val netClient = vertxContext.createNetClient(NetClientOptions(connectTimeout = 10000))
        val socket: NetSocket = awaitResult { netClient.connect(port, host, it) }
        try {
            return VertxStreamSocket(coroutineScope, socket)
        } catch (throwable: Throwable) {
            socket.close()
            throw throwable
        }
    }
}

private class VertxStreamSocket(
    coroutineScope: CoroutineScope,
    private val wrapped: NetSocket
) : IStreamSocket {
    private val readQueue = Channel<ByteBuffer>()
    private val writer = coroutineScope.actor<ByteBuffer> {
        consumeEach { buffer ->
            wrapped.write(Buffer.buffer(buffer.array()))
        }
    }
    private var readBufferLeftover: ByteBuffer? = null
    init {
        wrapped.handler { buffer ->
            readQueue.sendBlocking(ByteBuffer.wrap(buffer.getBytes()))
        }
        wrapped.closeHandler {
            readQueue.close()
        }
        wrapped.exceptionHandler { throwable ->
            readQueue.close(throwable)
        }
    }

    override fun close() = wrapped.close()

    override suspend fun read(buffer: ByteBuffer): Int {
        var bytesRead = 0

        readBufferLeftover?.let {
            bytesRead += it.transferTo(buffer)
            if (!it.hasRemaining())
                readBufferLeftover = null
        }
        if (!buffer.hasRemaining())
            return bytesRead

        val readBuffer = readQueue.receive()

        bytesRead += readBuffer.transferTo(buffer)
        if (readBuffer.hasRemaining())
            readBufferLeftover = readBuffer

        return bytesRead
    }

    override suspend fun write(buffer: ByteBuffer) {
        writer.send(buffer)
        buffer.flip()
    }
}
