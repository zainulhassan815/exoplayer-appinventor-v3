package dev.dreamerslab.media3

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import java.io.File
import java.util.concurrent.Executors

internal object OfflineStorage {
    private var database: StandaloneDatabaseProvider? = null
    private var streamCache: SimpleCache? = null
    private var downloadCache: SimpleCache? = null
    private var downloadManager: DownloadManager? = null

    fun database(context: Context): StandaloneDatabaseProvider =
        database ?: StandaloneDatabaseProvider(context.applicationContext).also { database = it }

    fun streamCache(context: Context, maxBytes: Long): SimpleCache =
        streamCache ?: SimpleCache(
            File(context.cacheDir, "media3-stream-cache"),
            LeastRecentlyUsedCacheEvictor(maxBytes),
            database(context)
        ).also { streamCache = it }

    fun downloadCache(context: Context): SimpleCache =
        downloadCache ?: SimpleCache(
            File(context.filesDir, "media3-downloads"),
            NoOpCacheEvictor(),
            database(context)
        ).also { downloadCache = it }

    fun downloadManager(context: Context, upstream: DataSource.Factory): DownloadManager =
        downloadManager ?: DownloadManager(
            context.applicationContext,
            database(context),
            downloadCache(context),
            upstream,
            Executors.newFixedThreadPool(DOWNLOAD_THREADS)
        ).also { downloadManager = it }

    val streamCacheBytes: Long get() = streamCache?.cacheSpace ?: 0

    fun clearStreamCache() {
        streamCache?.let { cache -> cache.keys.toList().forEach(cache::removeResource) }
    }

    private const val DOWNLOAD_THREADS = 2
}
