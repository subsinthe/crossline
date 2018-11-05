package com.example.subsinthe.crossline.util

import kotlin.collections.HashMap

class ObservableHashMap<K, V> : IMutableObservableMap<K, V> {
    private var _impl = HashMap<K, V>()
    private val changed = Multicast<MappingOp<K, V>>()

    override val entries get() = _impl.entries
    override val keys get() = _impl.keys
    override val values get() = _impl.values
    override val size get() = _impl.size

    override fun containsKey(key: K) = _impl.containsKey(key)
    override fun containsValue(value: V) = _impl.containsValue(value)

    override operator fun get(key: K) = _impl.get(key)

    override fun getOrDefault(key: K, defaultValue: V) = _impl.getOrDefault(key, defaultValue)

    override fun isEmpty() = _impl.isEmpty()

    override fun clear() {
        val oldImpl = _impl
        _impl = HashMap<K, V>()
        for (entry in oldImpl)
            changed(MappingOp.Removed(entry.key, entry.value))
    }

    override fun put(key: K, value: V) = _impl.put(key, value).also { old ->
        if (old == null)
            changed(MappingOp.Added(key, value))
        else
            changed(MappingOp.Updated(key, old, value))
    }

    override fun putAll(from: Map<out K, V>) = from.forEach { put(it.key, it.value) }

    override fun remove(key: K) = _impl.remove(key)?.also {
        changed(MappingOp.Removed(key, it))
    }

    override fun remove(key: K, value: V) = _impl.remove(key, value).also { wasRemoved ->
        if (wasRemoved)
            changed(MappingOp.Removed(key, value))
    }

    override fun subscribe(handler: (MappingOp<K, V>) -> Unit): Token {
        for (entry in _impl)
            handler(MappingOp.Added(entry.key, entry.value))
        return changed.subscribe(handler)
    }
}
