package com.example.subsinthe.crossline.util

import kotlin.collections.HashMap
import java.util.UUID
import java.util.logging.Logger

class Multicast<T> : IObservable<T> {
    private val handlers = HashMap<UUID, CancellableHandler<T>>()

    override fun subscribe(handler: (T) -> Unit): Token {
        val uuid = UUID.randomUUID()
        handlers.put(uuid, CancellableHandler(handler))
        return Token {
            handlers.remove(uuid)?.let { it.cancel() }
                ?: LOG.severe("Internal error: Handler for $uuid was already removed")
        }
    }

    operator fun invoke(value: T) {
        for (handler in handlers.values) {
            try {
                handler(value)
            } catch (ex: Throwable) {
                LOG.warning("Uncaught exception from handler: $ex")
            }
        }
    }

    private companion object {
        val LOG: Logger = Logger.getLogger(Multicast::class.java.name)
    }
}

private class CancellableHandler<T>(val wrapped: (T) -> Unit) {
    private var isCancelled = false

    operator fun invoke(value: T) {
        if (!isCancelled)
            wrapped(value)
    }

    fun cancel() { isCancelled = true }
}
