package com.example.subsinthe.crossline.util

class ObservableArrayList<T> : IObservableList<T> {
    private val impl_ = ArrayList<T>()
    private val changed = Multicast<MappingOp<Int, T>>()

    override val size get() = impl_.size

    override fun subscribe(handler: (MappingOp<Int, T>) -> Unit): Token {
        impl_.forEachIndexed { i, value ->
            handler(MappingOp.Added(i, value))
        }
        return changed.subscribe(handler)
    }

    override fun add(value: T) {
        impl_.add(value)
        changed(MappingOp.Added(size - 1, value))
    }

    override fun clear() {
        val copy = ArrayList(impl_)
        impl_.clear()
        copy.forEachIndexed { i, value ->
            changed(MappingOp.Removed(copy.size - i - 1, value))
        }
    }
}
