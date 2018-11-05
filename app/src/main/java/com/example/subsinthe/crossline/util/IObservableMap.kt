package com.example.subsinthe.crossline.util

import kotlin.collections.Map
import kotlin.collections.MutableMap

interface IObservableMap<K, V> : IObservable<MappingOp<K, V>>, Map<K, V>

interface IMutableObservableMap<K, V> : IObservableMap<K, V>, MutableMap<K, V>
