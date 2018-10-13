package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.ISocketFactory
import com.example.subsinthe.crossline.util.loggerFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.Closeable

class LoginFailedException(reason: String) : Exception("Login failed: $reason")

data class Credentials(val username: String, val password: String) {
    fun makeMd5(): String = String(Hex.encodeHex(DigestUtils.md5("$username$password")))
}

class Client private constructor(private val serverConnection: ServerConnection) : Closeable {
    private var ticketGenerator = 0

    companion object {
        private val LOG = loggerFor<Client>()

        suspend fun build(
            scope: CoroutineScope,
            socketFactory: ISocketFactory,
            host: String = "server.slsknet.org",
            port: Int = 2242
        ) = Client(Connection.server(scope, socketFactory.createTcpConnection(host, port)))
    }

    override fun close() = serverConnection.close()

    suspend fun login(credentials: Credentials) {
        LOG.info("login()")

        serverConnection.write(
            Request.Login(
                username = credentials.username,
                password = credentials.password,
                digest = credentials.makeMd5(),
                version = 183,
                minorVersion = 1
            )
        )
        val response = serverConnection.read()
        when (response) {
            is ServerResponse.LoginSuccessful -> {
                LOG.info("Successfully logged in. Server says ${response.greet}")
            }
            is ServerResponse.LoginFailed -> {
                LOG.warning("Failed to log in: ${response.reason}")
                throw LoginFailedException(response.reason)
            }
        }
    }

    suspend fun fileSearch(query: String): ReceiveChannel<String> {
        val ticket = ticketGenerator++

        LOG.info("fileSearch($query, ticket=$ticket)")

        val iterator = Channel<String>()
        val token = serverConnection.subscribe { response ->
            when (response) {
                /* is PeerResponse.SearchReply -> { */
                /*     if (response.ticket != ticket) */
                /*         return@subscribe */
                /*     for (result in response.results) { */
                /*         LOG.info("Received ${result.filename} from ${response.user}") */
                /*         iterator.send(result.filename) */
                /*     } */
                /* } */
            }
        }
        iterator.invokeOnClose { token.close() }

        return iterator
    }
}
