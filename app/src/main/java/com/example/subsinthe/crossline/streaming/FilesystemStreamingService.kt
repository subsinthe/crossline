package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import com.example.subsinthe.crossline.util.IObservable
import com.example.subsinthe.crossline.util.createScope
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.useOutput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.collections.HashMap
import org.apache.commons.io.FilenameUtils
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private typealias MusicTrack = IStreamingService.MusicTrack

class FilesystemStreamingService(
    private val scope: CoroutineScope,
    settings_: IObservable<Settings>
) : IStreamingService {
    private lateinit var settings: Settings
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val workerScope = worker.createScope()
    private val searcher = Searcher(workerScope, scope, entryFilter = ::containsFileFilter)
    private val connection = settings_.subscribe { onSettingsChanged(it) }

    private class Searcher(
        private val worker: CoroutineScope,
        private val scope: CoroutineScope,
        private val entryFilter: (File, String) -> Boolean
    ) {
        private val jobs = HashMap<UUID, Job>()

        fun search(query: String, root: String): AsyncIterator<MusicTrack> {
            val iterator = Channel<MusicTrack>()
            val jobId = UUID.randomUUID()
            val job = worker.launch { searchJob(jobId, iterator, query, root) }
            jobs.put(jobId, job)
            return AsyncIterator(iterator)
        }

        fun cancel() = jobs.values.forEach { it.cancel() }

        private suspend fun searchJob(
            id: UUID,
            output: SendChannel<MusicTrack>,
            query: String,
            root: String
        ) {
            output.useOutput {
                val rootEntry = File(root)
                if (!rootEntry.exists())
                    throw IllegalArgumentException("Root entry $root does not exist")

                rootEntry.walkTopDown().forEach {
                    if (it.isFile() && entryFilter(it, query))
                        it.asMusicTrack()?.let { output.send(it) }
                    yield()
                }
            }
            scope.launch {
                if (jobs.remove(id) == null)
                    LOG.severe("Internal error: Search job for $id was already removed")
            }
        }
    }

    data class Settings(val root: String)

    override fun close() {
        connection.close()
        searcher.cancel()
        worker.close()
    }

    override suspend fun search(query: String): AsyncIterator<MusicTrack> {
        LOG.info("search($query)")

        return searcher.search(query, settings.root)
    }

    private fun onSettingsChanged(settings_: Settings) {
        searcher.cancel()
        settings = settings_
    }

    companion object { val LOG = loggerFor<FilesystemStreamingService>() }
}

private fun File.asMusicTrack(): MusicTrack? {
    val audioFile = try { AudioFileIO.read(this) } catch (ex: Throwable) { null }
    audioFile?.tag?.apply {
        return MusicTrack(
            title = getFirst(FieldKey.TITLE) ?: FilenameUtils.removeExtension(getName()),
            artist = getFirst(FieldKey.ARTIST) ?: getFirst(FieldKey.ALBUM_ARTIST),
            album = getFirst(FieldKey.ALBUM)
        )
    }
    return null
}

private fun containsFileFilter(file: File, query: String) = file.getAbsolutePath().contains(query)
