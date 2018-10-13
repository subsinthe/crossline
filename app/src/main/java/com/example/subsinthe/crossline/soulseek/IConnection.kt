package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.util.Token
import java.io.Closeable

interface IConnection<in Request_, out Response_>
    : Closeable where Request_ : Request, Response_ : Response {
    suspend fun write(request: Request_)

    suspend fun read(): Response_

    suspend fun subscribe(handler: suspend (Response_) -> Unit): Token
}
