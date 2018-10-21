package com.example.subsinthe.crossline.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ObservableArrayList<T>(private val scope: CoroutineScope) : IObservableList<T> {
    private val impl_ = ArrayList<T>()
    private val changed = Multicast<MappingOp<Int, T>>(scope)

    override val size get() = impl_.size

    override suspend fun subscribe(handler: suspend (MappingOp<Int, T>) -> Unit): Token {
        impl_.forEachIndexed { i, value ->
            scope.launch { handler(MappingOp.Added(i, value)) }
        }
        return changed.subscribe(handler)
    }

    override suspend fun add(value: T) {
        impl_.add(value)
        changed.channel.send(MappingOp.Added(size - 1, value))
    }

    override suspend fun clear() {
        val copy = ArrayList(impl_)
        impl_.clear()
        copy.forEachIndexed { i, value ->
            changed.channel.send(MappingOp.Removed(copy.size - i - 1, value))
        }
    }
}
