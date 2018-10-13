package com.example.subsinthe.crossline.util

import java.util.logging.Logger

inline fun <reified T : Any> loggerFor() = Logger.getLogger(T::class.java.name)
