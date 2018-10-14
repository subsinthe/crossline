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

class Client private constructor(
    private val scope: CoroutineScope,
    private val socketFactory: ISocketFactory,
    private val serverConnection: ServerConnection
) : Closeable {
    private var ticketGenerator = 0

    companion object {
        private val LOG = loggerFor<Client>()

        suspend fun build(
            scope: CoroutineScope,
            socketFactory: ISocketFactory,
            host: String,
            port: Int
        ) = Client(scope, socketFactory, Connection.server(scope, socketFactory, host, port))
    }

    override fun close() = serverConnection.close()

    suspend fun login(credentials: Credentials) {
        LOG.info("login()")

        serverConnection.write(
            ServerRequest.Login(
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
        val token = serverConnection.subscribeTo<ServerResponse.ConnectToPeer> { response ->
            onFileSearchConnectToPeer(response, iterator, ticket)
        }
        iterator.invokeOnClose { token.close() }

        try {
            serverConnection.write(ServerRequest.FileSearch(ticket, query))
        } catch (ex: Throwable) {
            iterator.cancel()
            throw ex
        }
        return iterator
    }

    private suspend fun onFileSearchConnectToPeer(
        peerData: ServerResponse.ConnectToPeer,
        output: Channel<String>,
        ticket: Int
    ) {
        LOG.info("onFileSearchConnectToPeer($peerData, ticket=$ticket)")

        Connection.peer(
            scope = scope,
            socketFactory = socketFactory,
            host = peerData.ip,
            port = peerData.port,
            token = peerData.token
        ).use { peerConnection ->
            val iterator = peerConnection.forEach<PeerResponse.SearchReply>()
            output.invokeOnClose { iterator.cancel() }

            for (searchReply in iterator) {
                if (searchReply.ticket != ticket) {
                    LOG.info("Skipping unwanted ticket ${searchReply.ticket}")
                    continue
                }
                for (result in searchReply.results) {
                    LOG.info("Got ${result.filename} from ${searchReply.user}")

                    output.send(result.filename)
                }
            }
        }
    }
}
