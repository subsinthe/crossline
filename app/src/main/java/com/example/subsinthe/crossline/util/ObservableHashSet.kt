package com.example.subsinthe.crossline.util

import kotlin.collections.HashSet

class ObservableHashSet<T> : IMutableObservableSet<T> {
    private var _impl = HashSet<T>()
    private val changed = Multicast<SetOp<T>>()

    override val size get() = _impl.size

    override operator fun iterator() = _impl.iterator()

    override operator fun contains(element: T) = _impl.contains(element)
    override fun containsAll(elements: Collection<T>) = _impl.containsAll(elements)

    override fun isEmpty() = _impl.isEmpty()

    override fun add(element: T) = _impl.add(element).also { wasAdded ->
        if (wasAdded)
            changed(SetOp.Added(element))
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val oldSize = size
        for (element in elements)
            add(element)
        return oldSize != size
    }

    override fun clear() {
        val oldImpl = _impl
        _impl = HashSet<T>()
        for (element in oldImpl)
            changed(SetOp.Removed(element))
    }

    override fun remove(element: T) = _impl.remove(element).also { wasRemoved ->
        if (wasRemoved)
            changed(SetOp.Removed(element))
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val oldSize = size
        for (element in elements)
            remove(element)
        return oldSize != size
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val oldImpl = _impl
        val newImpl = HashSet<T>()
        for (element in elements)
            if (element in oldImpl)
                newImpl.add(element)
        _impl = newImpl
        for (element in oldImpl)
            if (!(element in _impl))
                changed(SetOp.Removed(element))
        return oldImpl.size != _impl.size
    }

    override fun subscribe(handler: (SetOp<T>) -> Unit): Token {
        for (element in _impl)
            handler(SetOp.Added(element))
        return changed.subscribe(handler)
    }
}
