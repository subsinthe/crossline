package com.example.subsinthe.crossline.streaming

import com.example.subsinthe.crossline.util.AsyncIterator
import com.example.subsinthe.crossline.util.IObservable
import com.example.subsinthe.crossline.util.ObservableValue
import com.example.subsinthe.crossline.util.TimedLruCache
import com.example.subsinthe.crossline.util.createScope
import com.example.subsinthe.crossline.util.loggerFor
import com.example.subsinthe.crossline.util.setTimer
import com.example.subsinthe.crossline.util.try_
import com.example.subsinthe.crossline.util.useOutput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import org.apache.commons.io.FilenameUtils
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.SupportedFileFormat
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class FilesystemStreamingService(
    private val scope: CoroutineScope,
    root: IObservable<String>,
    cacheSize: Int,
    private val cacheLifespan: Long
) : IStreamingService {
    private lateinit var root: String
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val workerScope = worker.createScope()
    private val searcher = Searcher(workerScope, scope, cacheSize, cacheLifespan)
    private var backgroundSearch: Job? = null
    private val rootConnection = root.subscribe { onRootChanged(it) }

    private class Searcher(
        private val worker: CoroutineScope,
        private val scope: CoroutineScope,
        cacheSize: Int,
        cacheLifespan: Long
    ) {
        private val jobs = HashMap<UUID, Job>()
        private val cache = TimedLruCache<String, MusicTrack>(cacheSize, cacheLifespan)

        fun search(
            query: String,
            root: String,
            refreshCache: Boolean = false
        ): AsyncIterator<MusicTrack> {
            LOG.info("search($query in $root)")

            val iterator = Channel<MusicTrack>()
            val jobId = UUID.randomUUID()
            val job = worker.launch { searchJob(jobId, iterator, query, root, refreshCache) }
            jobs.put(jobId, job)
            return AsyncIterator(iterator)
        }

        fun cancel() = jobs.values.forEach { it.cancel() }

        private suspend fun searchJob(
            id: UUID,
            output: SendChannel<MusicTrack>,
            query: String,
            root: String,
            refreshCache: Boolean
        ) {
            output.useOutput {
                val rootEntry = File(root)
                if (!rootEntry.exists())
                    throw IllegalArgumentException("Root entry $root does not exist")

                val knownTracks = HashSet<MusicTrack>()
                for (entry in rootEntry.walkTopDown()) {
                    yield()

                    LOG.try_({ "Entry $entry processing failed" }) {
                        processEntry(entry, refreshCache)
                    }?.let { track ->
                        if (matchQuery(query, track) && knownTracks.add(track))
                            output.send(track)
                    }
                }
            }

            scope.launch {
                assert(jobs.remove(id) != null) {
                    "Internal error: Search job for $id was already removed"
                }
            }
        }

        private fun processEntry(entry: File, refreshCache: Boolean) =
            if (entry.isFile()) {
                val path = entry.getAbsolutePath()
                val retrieveEntry = {
                    entry.asMusicTrack()?.also { cache.set(path, it) }
                }
                if (refreshCache)
                    retrieveEntry()
                else
                    cache.get(path) ?: retrieveEntry()
            } else {
                null
            }

        private fun matchQuery(query: String, track: MusicTrack) =
            track.title.contains(query, ignoreCase = true) ||
            track.artist?.contains(query, ignoreCase = true) ?: false ||
            track.album?.contains(query, ignoreCase = true) ?: false
    }

    class Settings(root: String) {
        val root = ObservableValue(root)
    }

    override val type = ServiceType.Filesystem

    override fun close() {
        rootConnection.close()
        searcher.cancel()
        worker.close()
    }

    override suspend fun search(query: String) = searcher.search(query, root)

    private fun onRootChanged(root_: String) {
        backgroundSearch?.cancel()
        searcher.cancel()

        root = root_
        backgroundSearch = scope.setTimer(cacheLifespan) { backgroundSearchJob(root) }
    }

    private suspend fun backgroundSearchJob(root: String) {
        LOG.info("Starting background search")

        searcher.search("", root, refreshCache = true).use {
            iterator -> for (ignore in iterator) {}
        }

        LOG.info("Background search finished")
    }

    companion object { val LOG = loggerFor<FilesystemStreamingService>() }
}

private fun File.asMusicTrack(): MusicTrack? {
    val filename = getName()

    if (!(FilenameUtils.getExtension(filename) in SUPPORTED_AUDIO_FORMATS))
        return null

    val audioFile = try {
        AudioFileIO.read(this)
    } catch (ex: Throwable) {
        return null
    }

    return audioFile.tag?.let { tag ->
        MusicTrack(
            title = tag.getFirst(FieldKey.TITLE)?.let {
                if (it != "") it else null
            } ?: FilenameUtils.removeExtension(filename),
            artist = tag.getFirst(FieldKey.ARTIST) ?: tag.getFirst(FieldKey.ALBUM_ARTIST),
            album = tag.getFirst(FieldKey.ALBUM)
        )
    }
}

private val SUPPORTED_AUDIO_FORMATS = hashSetOf(
    SupportedFileFormat.OGG.getFilesuffix(),
    SupportedFileFormat.MP3.getFilesuffix(),
    SupportedFileFormat.FLAC.getFilesuffix(),
    SupportedFileFormat.WAV.getFilesuffix()
)
