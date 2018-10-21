package com.example.subsinthe.crossline.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ObservableValue<T>(
    private var impl_: T,
    private val scope: CoroutineScope
) : IObservableValue<T> {
    private val changed = Multicast<T>(scope)

    override val value get() = impl_

    override suspend fun subscribe(handler: suspend (T) -> Unit): Token {
        scope.launch { handler(value) }
        return changed.subscribe(handler)
    }

    override suspend fun set(value: T) {
        impl_ = value
        changed.channel.send(impl_)
    }
}
