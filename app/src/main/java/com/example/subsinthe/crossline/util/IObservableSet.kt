package com.example.subsinthe.crossline.util

interface IObservableSet<T> : IObservable<SetOp<T>> {
    val size: Int

    fun add(value: T): Boolean
    fun clear()
}
