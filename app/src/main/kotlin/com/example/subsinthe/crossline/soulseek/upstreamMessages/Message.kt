package com.example.subsinthe.crossline.soulseek.upstreamMessages

import com.example.subsinthe.crossline.soulseek.DataType
import com.example.subsinthe.crossline.soulseek.makeData

interface Message {
    val code: Int
    val stream: Iterable<DataType>
}

class Login(
    username: String,
    password: String,
    digest: String,
    version: Int,
    minorVersion: Int
) : Message {
    override val code = 1

    override val stream: Iterable<DataType> = listOf(
        makeData(username),
        makeData(password),
        makeData(version),
        makeData(digest),
        makeData(minorVersion)
    )
}
