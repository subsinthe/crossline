package com.example.subsinthe.crossline.util

import java.util.logging.Logger

inline fun <reified T : Any> loggerFor() = Logger.getLogger(T::class.java.name)

inline fun Logger.try_(message: String, block: () -> Unit) {
    try_({ message }, block())
}

inline fun Logger.try_(lazyMessage: () -> String, block: () -> Unit) {
    try {
        block()
    } catch (ex: Throwable) {
        val message = try {
            lazyMessage()
        } catch (ex: Throwable) {
            "<Message unavailable>"
        }
        warning("$message:\n$ex")
    }
}
