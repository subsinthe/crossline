package com.example.subsinthe.crossline.util

interface IObservableValue<T> : IObservable<T> {
    val value: T
}

interface IMutableObservableValue<T> : IObservableValue<T> {
    override var value: T
}
