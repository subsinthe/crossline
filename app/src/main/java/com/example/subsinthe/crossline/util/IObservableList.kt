package com.example.subsinthe.crossline.util

interface IObservableList<T> : IObservable<MappingOp<Int, T>> {
    val size: Int

    fun add(value: T)
    fun clear()
}
