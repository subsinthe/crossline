package com.example.subsinthe.crossline.soulseek

sealed class Request {
    abstract val code: Int
    abstract val stream: Iterable<DataType>

    class Login(username: String, password: String, digest: String, version: Int, minorVersion: Int)
            : Request() {
        override val code = 1

        override val stream: Iterable<DataType> = listOf(
            makeData(username),
            makeData(password),
            makeData(version),
            makeData(digest),
            makeData(minorVersion)
        )
    }
}
