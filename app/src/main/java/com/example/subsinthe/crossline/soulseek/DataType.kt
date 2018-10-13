package com.example.subsinthe.crossline.soulseek

import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class DataType {
    abstract val size: Int

    abstract fun serialize(buffer: ByteBuffer)

    class I8(private val value: Byte) : DataType() {
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { buffer.put(value) }

        companion object {
            const val SIZE = Byte.SIZE_BYTES

            fun deserialize(buffer: ByteBuffer) = buffer.get()
        }
    }

    class I32(private val value: Int) : DataType() {
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { buffer.putInt(value) }

        companion object {
            const val SIZE = Int.SIZE_BYTES

            fun deserialize(buffer: ByteBuffer) = buffer.int
        }
    }

    class Bool(private val value: Boolean) : DataType() {
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { I8(if (value) 1 else 0).serialize(buffer) }

        companion object {
            const val SIZE = I8.SIZE

            fun deserialize(buffer: ByteBuffer) = DataType.I8.deserialize(buffer).compareTo(1) == 0
        }
    }

    class Str(private val value: ByteArray) : DataType() {
        override val size = I32.SIZE + value.size

        override fun serialize(buffer: ByteBuffer) {
            I32(value.size).serialize(buffer)
            buffer.put(value)
        }

        companion object {
            fun deserialize(buffer: ByteBuffer): String {
                val length = I32.deserialize(buffer)
                if (length == 0)
                    return String()
                val storage = ByteArray(length)
                buffer.get(storage)
                return String(storage)
            }
        }
    }

    companion object {
        val BYTE_ORDER = ByteOrder.LITTLE_ENDIAN

        fun build(value: Byte) = DataType.I8(value)
        fun build(value: Int) = DataType.I32(value)
        fun build(value: ByteArray) = DataType.Str(value)
        fun build(value: String) = DataType.Str(value.toByteArray())
    }
}
