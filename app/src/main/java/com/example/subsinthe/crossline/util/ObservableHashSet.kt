package com.example.subsinthe.crossline.util

import kotlin.collections.HashSet

class ObservableHashSet<T> : IObservableSet<T> {
    private val impl_ = HashSet<T>()
    private val changed = Multicast<SetOp<T>>()

    override val size get() = impl_.size

    override fun subscribe(handler: (SetOp<T>) -> Unit): Token {
        impl_.forEach { handler(SetOp.Added(it)) }
        return changed.subscribe(handler)
    }

    override fun add(value: T) = impl_.add(value).also { added ->
        if (added)
            changed(SetOp.Added(value))
    }

    override fun clear() {
        val copy = HashSet(impl_)
        impl_.clear()
        copy.forEach { changed(SetOp.Removed(it)) }
    }
}
