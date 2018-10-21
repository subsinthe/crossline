package com.example.subsinthe.crossline.util

interface IObservable<T> {
    suspend fun subscribe(handler: suspend (T) -> Unit): Token
}
