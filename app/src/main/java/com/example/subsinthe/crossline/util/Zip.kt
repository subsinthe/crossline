package com.example.subsinthe.crossline.util

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.Inflater

class Zip {
    companion object {
        fun decompress(buffer: ByteBuffer): ByteBuffer {
            val input = buffer.array()
            val inflater = Inflater()
            inflater.setInput(input)

            ByteArrayOutputStream(INFLATE_BUFFER_SIZE).use { outputStream ->
                val intermediateBuffer = ByteArray(INFLATE_BUFFER_SIZE)
                while (!inflater.finished()) {
                    val count = inflater.inflate(intermediateBuffer)
                    buffer.position(buffer.position() + inflater.getBytesRead().toInt())
                    outputStream.write(intermediateBuffer, 0, count)
                }
                return ByteBuffer.wrap(outputStream.toByteArray())
            }
        }

        private const val INFLATE_BUFFER_SIZE = 512 * 1024
    }
}
