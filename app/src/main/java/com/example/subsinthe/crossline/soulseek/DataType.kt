package com.example.subsinthe.crossline.soulseek

import kotlin.collections.ArrayList
import java.nio.ByteBuffer
import java.nio.ByteOrder

private typealias DeserializerRoutine<T> = (ByteBuffer) -> T

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

    class U8(private val value: Int) : DataType() {
        init {
            if (0 > value || value > 255)
                throw IllegalArgumentException("Attempt to serialize $value as U8")
        }
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { buffer.put(value.toByte()) }

        companion object {
            const val SIZE = Byte.SIZE_BYTES

            fun deserialize(buffer: ByteBuffer) = buffer.get().toInt() and 0xff
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

    class U32(private val value: Long) : DataType() {
        init {
            if (0 > value || value > 4294967296)
                throw IllegalArgumentException("Attempt to serialize $value as U32")
        }
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { buffer.putInt(value.toInt()) }

        companion object {
            const val SIZE = Int.SIZE_BYTES

            fun deserialize(buffer: ByteBuffer) = buffer.get().toLong() and 0xffffffff
        }
    }

    class I64(private val value: Long) : DataType() {
        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) { buffer.putLong(value) }

        companion object {
            const val SIZE = Long.SIZE_BYTES

            fun deserialize(buffer: ByteBuffer) = buffer.long
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

    class Ip(value: String) : DataType() {
        private val split = value.split(".")
        init {
            if (split.size != PARTS_COUNT)
                throw IllegalArgumentException("Split size ${split.size} is not equal to ip parts count")
        }

        override val size = SIZE

        override fun serialize(buffer: ByteBuffer) {
            for (part in split)
                U8(part.toInt()).serialize(buffer)
        }

        companion object {
            const val PARTS_COUNT = 4
            const val SIZE = PARTS_COUNT * I8.SIZE

            fun deserialize(buffer: ByteBuffer): String {
                val storage = arrayOf(
                    U8.deserialize(buffer),
                    U8.deserialize(buffer),
                    U8.deserialize(buffer),
                    U8.deserialize(buffer)
                )
                return "${storage[3]}.${storage[2]}.${storage[1]}.${storage[0]}"
            }
        }
    }

    class List(private val list: ArrayList<DataType>) : DataType() {
        override val size = I32.SIZE + list.size

        override fun serialize(buffer: ByteBuffer) {
            I32(list.size).serialize(buffer)
            list.forEach { it.serialize(buffer) }
        }

        companion object {
            fun <T> deserialize(
                deserializer: DeserializerRoutine<T>,
                buffer: ByteBuffer
            ): ArrayList<T> {
                val length = I32.deserialize(buffer)
                val result = ArrayList<T>()
                for (i in 0..length)
                    result.add(deserializer(buffer))
                return result
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
