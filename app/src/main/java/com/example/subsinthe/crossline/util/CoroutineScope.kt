package com.example.subsinthe.crossline.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> CoroutineScope.pack(callable: suspend (T) -> Unit) = {
    it: T -> launch { callable(it) }.discard()
}
