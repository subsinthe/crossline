package com.example.subsinthe.crossline.util

import kotlin.collections.MutableSet
import kotlin.collections.Set

interface IObservableSet<T> : IObservable<SetOp<T>>, Set<T>

interface IMutableObservableSet<T> : IObservableSet<T>, MutableSet<T>
