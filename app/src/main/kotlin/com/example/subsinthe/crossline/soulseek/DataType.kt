package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer

sealed class DataType {
    abstract val size: Int

    open abstract fun serialize(bufer: ByteBuffer)

    class Int8Data(val value: Char) : DataType() {
        override val size = Character.BYTES

        override fun serialize(buffer: ByteBuffer) { buffer.putChar(value) }
    }

    class Int32Data(val value: Int) : DataType() {
        override val size = Integer.BYTES

        override fun serialize(buffer: ByteBuffer) { buffer.putInt(value) }
    }

    class StringData(val value: ByteArray) : DataType() {
        override val size = Integer.BYTES + value.size

        override fun serialize(buffer: ByteBuffer) {
            buffer.putInt(value.size)
            buffer.put(value)
        }
    }
}

fun makeData(value: Char) = DataType.Int8Data(value)
fun makeData(value: Int) = DataType.Int32Data(value)
fun makeData(value: ByteArray) = DataType.StringData(value)
fun makeData(value: String) = DataType.StringData(value.toByteArray())
