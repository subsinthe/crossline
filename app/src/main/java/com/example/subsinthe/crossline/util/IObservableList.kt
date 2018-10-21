package com.example.subsinthe.crossline.util

interface IObservableList<T> : IObservable<MappingOp<Int, T>> {
    val size: Int

    suspend fun add(value: T)
    suspend fun clear()
}
