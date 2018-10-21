package com.example.subsinthe.crossline.util

sealed class MappingOp<Key, Value>(val key: Key) {
    class Added<Key, Value>(key: Key, val value: Value) : MappingOp<Key, Value>(key) {
        override fun toString() = "MappingOp.Added($key to $value)"
    }
    class Removed<Key, Value>(key: Key, val value: Value) : MappingOp<Key, Value>(key) {
        override fun toString() = "MappingOp.Removed($key to $value)"
    }

    class Updated<Key, Value>(
        key: Key,
        val old: Value,
        val new: Value
    ) : MappingOp<Key, Value>(key) {
        override fun toString() = "MappingOp.Updated($key to $old -> $new)"
    }

    override fun toString() = "MappingOp($key)"
}
