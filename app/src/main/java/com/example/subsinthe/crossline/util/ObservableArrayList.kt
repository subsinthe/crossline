package com.example.subsinthe.crossline.util

import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.RandomAccess

class ObservableArrayList<T> : IMutableObservableList<T>, RandomAccess {
    private var _impl = ArrayList<T>()
    private val changed = Multicast<MappingOp<Int, T>>()

    override val size get() = _impl.size

    override operator fun iterator() = _impl.iterator()

    override operator fun contains(element: T) = _impl.contains(element)
    override fun containsAll(elements: Collection<T>) = _impl.containsAll(elements)

    override fun isEmpty() = _impl.isEmpty()

    override operator fun get(index: Int) = _impl.get(index)

    override fun indexOf(element: T) = _impl.indexOf(element)
    override fun lastIndexOf(element: T) = _impl.lastIndexOf(element)

    override fun listIterator() = _impl.listIterator()
    override fun listIterator(index: Int) = _impl.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = _impl.subList(fromIndex, toIndex)

    override fun add(element: T) = _impl.add(element).also {
        changed(MappingOp.Added(size - 1, element))
    }

    override fun add(index: Int, element: T) = _impl.add(index, element).also {
        changed(MappingOp.Added(index, element))
    }

    override fun addAll(elements: Collection<T>) = _impl.addAll(elements).also {
        elements.forEachIndexed { i, element ->
            changed(MappingOp.Added(size - elements.size + i, element))
        }
    }

    override fun addAll(index: Int, elements: Collection<T>) = _impl.addAll(index, elements).also {
        elements.forEachIndexed { i, element ->
            changed(MappingOp.Added(index + i, element))
        }
    }

    override fun clear() {
        val oldImpl = _impl
        _impl = ArrayList<T>()
        for (i in 0 until oldImpl.size) {
            val index = oldImpl.size - 1 - i
            changed(MappingOp.Removed(index, oldImpl[index]))
        }
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        if (index == -1)
            return false
        _impl.removeAt(index).let { changed(MappingOp.Removed(index, it)) }
        return true
    }

    override fun removeAll(elements: Collection<T>) = removeAll(elements) { e, o -> e in o }

    override fun removeAt(index: Int) = _impl.removeAt(index).also {
        changed(MappingOp.Removed(index, it))
    }

    override fun retainAll(elements: Collection<T>) = removeAll(elements) { e, o -> !(e in o) }

    override operator fun set(index: Int, element: T) = _impl.set(index, element).also {
        changed(MappingOp.Updated(index, it, element))
    }

    override fun subscribe(handler: (MappingOp<Int, T>) -> Unit): Token {
        _impl.forEachIndexed { i, value ->
            handler(MappingOp.Added(i, value))
        }
        return changed.subscribe(handler)
    }

    private fun removeAll(elements: Collection<T>, pred: (T, Collection<T>) -> Boolean): Boolean {
        val oldSize = size
        var i = 0
        while (i < size) {
            if (pred(get(i), elements))
                removeAt(i)
            else
                ++i
        }
        return size != oldSize
    }
}
