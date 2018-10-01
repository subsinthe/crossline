package com.example.subsinthe.crossline.soulseek

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
import java.io.Closeable
import java.nio.ByteBuffer

class Socket(ioScope: CoroutineScope, private val wrapped: NetSocket, private val vs: Vertx) : Closeable {
    private val readQueue = Channel<ByteBuffer>()
    private val writer = ioScope.actor<ByteBuffer> {
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

    companion object {
        suspend fun build(
            ioScope: CoroutineScope,
            host: String,
            port: Int
        ): Socket {
            System.setProperty("vertx.disableFileCPResolving", "true")
            val vx = Vertx.vertx()
            val netClient = vx.createNetClient(NetClientOptions(connectTimeout = 10000))
            return Socket(ioScope, awaitResult { netClient.connect(port, host, it) }, vx)
        }
    }

    override fun close() = wrapped.close()

    suspend fun read(buffer: ByteBuffer): Int {
        var bytesRead = 0

        val leftover = readBufferLeftover
        if (leftover != null) {
            bytesRead += leftover.transferTo(buffer)
            if (!leftover.hasRemaining())
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

    suspend fun write(buffer: ByteBuffer) {
        writer.send(buffer)
        buffer.flip()
    }
}
