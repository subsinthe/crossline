package com.example.subsinthe.crossline.util

import java.util.logging.Logger

inline fun <reified T : Any> loggerFor() = Logger.getLogger(T::class.java.name)

inline fun <R> Logger.try_(message: String, block: () -> R?): R? {
    return try_({ message }, block())
}

inline fun <R> Logger.try_(lazyMessage: () -> String, block: () -> R?): R? {
    try {
        return block()
    } catch (ex: Throwable) {
        val message = try {
            lazyMessage()
        } catch (ex: Throwable) {
            "<Message unavailable>"
        }
        warning("$message:\n$ex")

        return null
    }
}
