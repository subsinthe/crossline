package com.example.subsinthe.crossline.util

import kotlin.collections.HashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import java.util.UUID
import java.util.logging.Logger

private class CancellableHandler<T>(val wrapped: suspend (T) -> Unit) {
    private var isCancelled = false

    suspend operator fun invoke(value: T) {
        if (!isCancelled)
            wrapped(value)
    }

    fun cancel() { isCancelled = true }
}

class Multicast<T>(private val scope: CoroutineScope) : IObservable<T> {
    private val handlers = HashMap<UUID, CancellableHandler<T>>()
    private val worker = scope.actor<T> {
        consumeEach {
            for (handler in handlers.values) scope.launch {
                try {
                    handler(it)
                } catch (ex: Throwable) {
                    LOG.warning("Uncaught exception from handler: $ex")
                }
            }
        }
    }

    val channel: SendChannel<T> = worker

    override suspend fun subscribe(handler: suspend (T) -> Unit) = scope.async {
        val uuid = UUID.randomUUID()
        handlers.put(uuid, CancellableHandler(handler))
        Token {
            scope.launch {
                val handler = handlers.remove(uuid)
                if (handler != null)
                    handler.cancel()
                else
                    LOG.severe("Internal error: Handler for $uuid was already removed")
            }
        }
    }.await()

    private companion object {
        val LOG: Logger = Logger.getLogger(Multicast::class.java.name)
    }
}
