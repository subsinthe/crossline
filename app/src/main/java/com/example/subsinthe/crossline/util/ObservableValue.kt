package com.example.subsinthe.crossline.util

class ObservableValue<T>(private var impl_: T) : IMutableObservableValue<T> {
    private val changed = Multicast<T>()

    override var value
        get() = impl_
        set(value: T) {
            if (impl_ == value)
                return
            impl_ = value
            changed(impl_)
        }

    override fun subscribe(handler: (T) -> Unit): Token {
        handler(value)
        return changed.subscribe(handler)
    }
}
