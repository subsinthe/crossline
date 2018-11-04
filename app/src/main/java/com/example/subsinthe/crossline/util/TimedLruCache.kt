package com.example.subsinthe.crossline.util

import org.apache.commons.collections4.map.LRUMap

class TimedLruCache<K, V>(cacheSize: Int, private val lifespan: Long) {
    private val _impl = LRUMap<K, Entry<V>>(cacheSize)

    private class Entry<V>(private val _value: V, lifespan: Long) {
        private val lifespan = Lifespan(lifespan)

        val value: V? get() = if (lifespan.valid) _value else null
    }

    fun get(key: K) = _impl.get(key)?.let { it.value ?: _impl.remove(key).let { null } }

    fun set(key: K, value: V) = _impl.set(key, Entry(value, lifespan))
}
