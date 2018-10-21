package com.example.subsinthe.crossline.soulseek

import com.example.subsinthe.crossline.network.ISocketFactory
import com.example.subsinthe.crossline.util.AsyncIterator
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.pack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
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
        val response = serverConnection.forEach<ServerResponse.LoginSuccessful>().use { successIterator ->
            serverConnection.forEach<ServerResponse.LoginFailed>().use { failureIterator ->
                select<ServerResponse> {
                    successIterator.onReceive { response -> response }
                    failureIterator.onReceive { response -> response }
                }
            }
        }
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

    suspend fun fileSearch(query: String): AsyncIterator<String> {
        val ticket = ticketGenerator++

        LOG.info("fileSearch($query, ticket=$ticket)")

        val channel = Channel<String>()
        val token = serverConnection.subscribeTo<ServerResponse.ConnectToPeer>(scope.pack
            { response -> onFileSearchConnectToPeer(response, channel, ticket) }
        )
        channel.invokeOnClose { token.close() }

        return AsyncIterator(channel).also {
            it.closeOnError { serverConnection.write(ServerRequest.FileSearch(ticket, query)) }
        }
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
            peerConnection.forEach<PeerResponse.SearchReply>().use { iterator ->
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
}
