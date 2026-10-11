package dev.dreamerslab.media3

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

internal object OfflineStorage {
    private var database: StandaloneDatabaseProvider? = null
    private var streamCache: SimpleCache? = null

    fun database(context: Context): StandaloneDatabaseProvider =
        database ?: StandaloneDatabaseProvider(context.applicationContext).also { database = it }

    fun streamCache(context: Context, maxBytes: Long): SimpleCache =
        streamCache ?: SimpleCache(
            File(context.cacheDir, "media3-stream-cache"),
            LeastRecentlyUsedCacheEvictor(maxBytes),
            database(context)
        ).also { streamCache = it }

    val streamCacheBytes: Long get() = streamCache?.cacheSpace ?: 0

    fun clearStreamCache() {
        streamCache?.let { cache -> cache.keys.toList().forEach(cache::removeResource) }
    }
}
