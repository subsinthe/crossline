package com.example.subsinthe.crossline.util

sealed class SetOp<T>(val value: T) {
    class Added<T>(value: T) : SetOp<T>(value)
    class Removed<T>(value: T) : SetOp<T>(value)

    override fun toString() = "SetOp($value)"
}
