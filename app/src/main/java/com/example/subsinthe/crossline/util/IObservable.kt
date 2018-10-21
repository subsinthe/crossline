package com.example.subsinthe.crossline.util

interface IObservable<T> {
    fun subscribe(handler: (T) -> Unit): Token
}
