package com.example.subsinthe.crossline.soulseek

sealed class Response {
    abstract val code: Int

    data class LoginSuccessful(val greet: String, val ip: Int) : Response() {
        override val code = 1
    }

    data class LoginFailed(val reason: String) : Response() {
        override val code = 1
    }
}
