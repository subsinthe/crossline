package com.example.subsinthe.crossline.util

import kotlin.collections.List
import kotlin.collections.MutableList

interface IObservableList<T> : IObservable<MappingOp<Int, T>>, List<T>

interface IMutableObservableList<T> : IObservableList<T>, MutableList<T>
