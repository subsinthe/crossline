package com.example.subsinthe.crossline

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.io.Closeable
import java.net.InetAddress

private class ThreadPoolScope(workers: Int, name: String) : CoroutineScope, Closeable {
    override val coroutineContext = newFixedThreadPoolContext(workers, name)

    override fun close() = coroutineContext.close()
}

class DnsResolver : Closeable {
    private val scope = ThreadPoolScope(5, "dnsResolver")

    suspend fun resolve(host: String) = scope.async { InetAddress.getByName(host) }.await()

    override fun close() = scope.close()
}
