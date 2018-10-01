package com.example.subsinthe.crossline.soulseek.downstreamMessages

sealed class Message {
    abstract val code: Int
}

data class LoginSuccessful(
    val greet: String,
    val ip: Int
) : Message() {
    override val code = 1
}

data class LoginFailed(
    val reason: String
) : Message() {
    override val code = 1
}
