package com.example.subsinthe.crossline.util

interface IObservableValue<T> : IObservable<T> {
    val value: T
    suspend fun set(value: T)
}
