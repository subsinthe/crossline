package com.example.subsinthe.crossline.network

import com.example.subsinthe.crossline.util.loggerFor
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.net.NetClientOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.nio.ByteBuffer

class VertxSocketFactory(val coroutineScope: CoroutineScope) : ISocketFactory {
    init { System.setProperty("vertx.disableFileCPResolving", "true") }
    private val vertxContext = Vertx.vertx()

    override fun close() = vertxContext.close()

    override suspend fun createTcpConnection(host: String, port: Int): IStreamSocket {
        val netClient = vertxContext.createNetClient(NetClientOptions(connectTimeout = 10000))
        val socket: NetSocket = vx { netClient.connect(port, host, it) }
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
            val product = Buffer.buffer(buffer.array())

            LOG.fine("Writing ${product.length()}")
            wrapped.write(product)
        }
    }
    init {
        wrapped.handler { buffer ->
            LOG.fine("onRead(${buffer.length()})")
            readQueue.sendBlocking(ByteBuffer.wrap(buffer.getBytes()))
        }
        wrapped.closeHandler {
            LOG.fine("onClose()")
            readQueue.close()
        }
        wrapped.exceptionHandler { ex ->
            LOG.fine("onClose($ex)")
            readQueue.close(ex)
        }
    }

    override fun close() = wrapped.close()

    override suspend fun read() = readQueue.receive()

    override suspend fun write(buffer: ByteBuffer) { writer.send(buffer) }

    private companion object { val LOG = loggerFor<VertxStreamSocket>() }
}

private typealias VertxHandler<T> = Handler<AsyncResult<T>>
private typealias VertxCallback<T> = (VertxHandler<T>) -> Unit

private suspend inline fun <T> vx(crossinline callback: VertxCallback<T>) = suspendCoroutine<T> {
    continuation ->
    callback(object : VertxHandler<T> {
        override fun handle(event: AsyncResult<T>) {
            if (event.succeeded()) {
                continuation.resume(event.result())
            } else {
                continuation.resumeWithException(event.cause())
            }
        }
    })
}
