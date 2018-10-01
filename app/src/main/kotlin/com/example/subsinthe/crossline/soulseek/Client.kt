package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.soulseek.downstreamMessages.LoginFailed
import com.example.subsinthe.crossline.soulseek.downstreamMessages.LoginSuccessful
import com.example.subsinthe.crossline.soulseek.upstreamMessages.Login
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.experimental.CoroutineScope
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.Closeable
import java.util.logging.Logger

class LoginFailedException(reason: String) : Exception("Login failed: $reason")

data class Credentials(val username: String, val password: String) {
    fun makeMd5(): String = String(Hex.encodeHex(DigestUtils.md5("$username$password")))
}

class Client private constructor(private val serverSocket: ServerSocket) : Closeable {

    companion object {
        private val LOG: Logger = loggerFor<Client>()

        suspend fun build(
            ioScope: CoroutineScope,
            host: String = "server.slsknet.org",
            port: Int = 2242
        ) = Client(ServerSocket.build(ioScope, host, port, 1 * 1024 * 1024))
    }

    override fun close() = serverSocket.close()

    suspend fun login(credentials: Credentials) {
        serverSocket.write(
            Login(
                username = credentials.username,
                password = credentials.password,
                digest = credentials.makeMd5(),
                version = 183,
                minorVersion = 1
            )
        )

        val response = serverSocket.read()
        when (response) {
            is LoginSuccessful -> LOG.info("Successfully logged in. Server says ${response.greet}")
            is LoginFailed -> throw LoginFailedException(response.reason)
        }
    }
}
