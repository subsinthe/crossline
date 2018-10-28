package com.example.subsinthe.crossline.util

import kotlin.collections.HashMap

class ObservableHashMap<K, V> : IObservableMap<K, V> {
    private val impl_ = HashMap<K, V>()
    private val changed = Multicast<MappingOp<K, V>>()

    override val size get() = impl_.size

    override fun subscribe(handler: (MappingOp<K, V>) -> Unit): Token {
        impl_.forEach { entry ->
            handler(MappingOp.Added(entry.key, entry.value))
        }
        return changed.subscribe(handler)
    }

    override fun put(key: K, value: V): V? {
        val old = impl_.put(key, value)
        if (old == null)
            changed(MappingOp.Added(key, value))
        else
            changed(MappingOp.Updated(key, old, value))
        return old
    }

    override fun clear() {
        val copy = HashMap(impl_)
        impl_.clear()
        copy.forEach { entry ->
            changed(MappingOp.Removed(entry.key, entry.value))
        }
    }
}
