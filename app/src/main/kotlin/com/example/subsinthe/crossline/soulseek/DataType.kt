package com.example.subsinthe.crossline.soulseek

sealed class DataType {
    abstract val size: Int
}

class Int8Data(val value: Char) : DataType() {
    override val size = Character.BYTES
}

class Int32Data(val value: Int) : DataType() {
    override val size = Integer.BYTES
}

class StringData(val value: ByteArray) : DataType() {
    override val size = Integer.BYTES + value.size
}

fun makeData(value: Char) = Int8Data(value)
fun makeData(value: Int) = Int32Data(value)
fun makeData(value: ByteArray) = StringData(value)
fun makeData(value: String) = StringData(value.toByteArray())
