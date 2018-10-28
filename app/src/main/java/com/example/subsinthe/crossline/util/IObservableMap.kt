package com.example.subsinthe.crossline.util

interface IObservableMap<K, V> : IObservable<MappingOp<K, V>> {
    val size: Int

    fun put(key: K, value: V): V?
    fun clear()
}
