package dev.dreamerslab.media3

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import dev.dreamerslab.media3.helpers.DrmScheme
import dev.dreamerslab.media3.helpers.MediaType
import dev.dreamerslab.media3.ui.ThumbnailSource
import java.util.UUID

internal object MediaItems {

    val DRM_SCHEMES = mapOf(
        DrmScheme.Widevine to C.WIDEVINE_UUID,
        DrmScheme.ClearKey to C.CLEARKEY_UUID,
        DrmScheme.PlayReady to C.PLAYREADY_UUID,
    )
    val MIME_TYPES = mapOf(
        MediaType.Auto to null,
        MediaType.Hls to MimeTypes.APPLICATION_M3U8,
        MediaType.Dash to MimeTypes.APPLICATION_MPD,
        MediaType.SmoothStreaming to MimeTypes.APPLICATION_SS,
        MediaType.Rtsp to MimeTypes.APPLICATION_RTSP,
        MediaType.Progressive to MimeTypes.VIDEO_UNKNOWN,
    )

    private const val KEY_REQUEST_HEADERS = "media3player.requestHeaders"
    private var nextId = 0

    fun newMediaId() = "media-${nextId++}"

    fun requestHeaders(item: MediaItem): Map<String, String> {
        val headers = item.mediaMetadata.extras?.getBundle(KEY_REQUEST_HEADERS) ?: return emptyMap()
        return headers.keySet().associateWith { headers.getString(it).orEmpty() }
    }

    fun requestHeadersExtras(headers: Map<String, String>, extras: Bundle?): Bundle = Bundle(extras ?: Bundle()).apply {
        putBundle(KEY_REQUEST_HEADERS, Bundle().apply { headers.forEach(::putString) })
    }

    fun create(source: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(newMediaId())
            .setUri(resolve(source))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title.ifBlank { null }).build())
            .build()

    fun withSubtitle(item: MediaItem, source: String, language: String, label: String): MediaItem {
        val existing = item.localConfiguration?.subtitleConfigurations.orEmpty()
        return item.buildUpon().setSubtitleConfigurations(existing + subtitle(source, language, label)).build()
    }

    fun subtitle(source: String, language: String, label: String): MediaItem.SubtitleConfiguration {
        val uri = resolve(source)
        return MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(subtitleMimeType(uri))
            .setLanguage(language.ifBlank { null })
            .setLabel(label.ifBlank { null })
            .build()
    }

    fun clipped(item: MediaItem, startMs: Long, endMs: Long): MediaItem =
        item.buildUpon().setClippingConfiguration(clipping(startMs, endMs)).build()

    fun clipping(startMs: Long, endMs: Long): MediaItem.ClippingConfiguration =
        MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0))
            .setEndPositionMs(if (endMs > 0) endMs else C.TIME_END_OF_SOURCE)
            .build()

    fun withDrm(item: MediaItem, scheme: UUID, licenseUrl: String, headers: Map<String, String>): MediaItem =
        item.buildUpon().setDrmConfiguration(drm(scheme, licenseUrl, headers)).build()

    fun drm(scheme: UUID, licenseUrl: String, headers: Map<String, String>): MediaItem.DrmConfiguration =
        MediaItem.DrmConfiguration.Builder(scheme)
            .setLicenseUri(licenseUrl.ifBlank { null })
            .setLicenseRequestHeaders(headers)
            .build()

    fun withMimeType(item: MediaItem, mimeType: String?): MediaItem = item.buildUpon().setMimeType(mimeType).build()

    fun withMetadata(item: MediaItem, artist: String, description: String, artworkUrl: String): MediaItem =
        item.buildUpon()
            .setMediaMetadata(
                item.mediaMetadata.buildUpon()
                    .setArtist(artist.ifBlank { null })
                    .setDescription(description.ifBlank { null })
                    .setArtworkUri(artworkUrl.ifBlank { null }?.let(::resolve))
                    .build()
            )
            .build()

    fun withThumbnails(item: MediaItem, source: ThumbnailSource): MediaItem =
        item.buildUpon()
            .setMediaMetadata(item.mediaMetadata.buildUpon().setExtras(source.toExtras(item.mediaMetadata.extras)).build())
            .build()

    fun resolve(source: String): Uri = when {
        "://" in source -> Uri.parse(source)
        source.startsWith("/") -> Uri.parse("file://$source")
        else -> Uri.parse("asset:///$source")
    }

    private fun subtitleMimeType(uri: Uri): String =
        when (uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()) {
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "ttml", "dfxp", "xml" -> MimeTypes.APPLICATION_TTML
            "ssa", "ass" -> MimeTypes.TEXT_SSA
            else -> MimeTypes.TEXT_VTT
        }
}
