package dev.dreamerslab.media3

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

internal class MediaSources(private val context: Context) : MediaSource.Factory {

    val http: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    private val drmSessions = DefaultDrmSessionManagerProvider()
    private var drmSessionManagers = DrmSessionManagerProvider { ClearKeys.sessionManager(it) ?: drmSessions.get(it) }
    private var loadErrorHandling: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
    private var cacheSizeMb = 0
    private var liveTargetOffsetMs = C.TIME_UNSET
    private val shared = factory(http)

    fun setCacheSize(megabytes: Int) {
        cacheSizeMb = megabytes
        shared.setDataSourceFactory(dataSources(http))
    }

    fun setLiveTargetOffset(milliseconds: Long) {
        liveTargetOffsetMs = milliseconds
        shared.setLiveTargetOffsetMs(milliseconds)
    }

    override fun createMediaSource(mediaItem: MediaItem): MediaSource = shared.createMediaSource(mediaItem)

    override fun getSupportedTypes(): IntArray = shared.supportedTypes

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider) = apply {
        drmSessionManagers = drmSessionManagerProvider
        shared.setDrmSessionManagerProvider(drmSessionManagerProvider)
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy) = apply {
        loadErrorHandling = loadErrorHandlingPolicy
        shared.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    }

    private fun factory(upstream: DataSource.Factory) = DefaultMediaSourceFactory(dataSources(upstream))
        .setDrmSessionManagerProvider(drmSessionManagers)
        .setLoadErrorHandlingPolicy(loadErrorHandling)
        .setLiveTargetOffsetMs(liveTargetOffsetMs)

    private fun dataSources(upstream: DataSource.Factory): DataSource.Factory {
        val network = DefaultDataSource.Factory(context, upstream)
        val cached = if (cacheSizeMb > 0) {
            CacheDataSource.Factory()
                .setCache(OfflineStorage.streamCache(context, cacheSizeMb * 1024L * 1024L))
                .setUpstreamDataSourceFactory(network)
        } else {
            network
        }
        return CacheDataSource.Factory()
            .setCache(OfflineStorage.downloadCache(context))
            .setUpstreamDataSourceFactory(cached)
            .setCacheWriteDataSinkFactory(null)
    }
}
