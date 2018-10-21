package com.example.subsinthe.crossline.util

interface IObservableValue<T> : IObservable<T> {
    var value: T
}
