package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.ISocketFactory
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.experimental.CoroutineScope
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.Closeable

class LoginFailedException(reason: String) : Exception("Login failed: $reason")

data class Credentials(val username: String, val password: String) {
    fun makeMd5(): String = String(Hex.encodeHex(DigestUtils.md5("$username$password")))
}

class Client private constructor(private val connection: ServerConnection) : Closeable {
    companion object {
        private val LOG = loggerFor<Client>()

        suspend fun build(
            scope: CoroutineScope,
            socketFactory: ISocketFactory,
            host: String = "server.slsknet.org",
            port: Int = 2242
        ) = Client(ServerConnection(scope, socketFactory.createTcpConnection(host, port)))
    }

    override fun close() = connection.close()

    suspend fun login(credentials: Credentials) {
        LOG.info("login()")
        val response = connection.make_request(
            Request.Login(
                username = credentials.username,
                password = credentials.password,
                digest = credentials.makeMd5(),
                version = 183,
                minorVersion = 1
            )
        )
        when (response) {
            is Response.LoginSuccessful -> {
                LOG.info("Successfully logged in. Server says ${response.greet}")
            }
            is Response.LoginFailed -> {
                LOG.warning("Failed to log in: ${response.reason}")
                throw LoginFailedException(response.reason)
            }
        }
    }
}
