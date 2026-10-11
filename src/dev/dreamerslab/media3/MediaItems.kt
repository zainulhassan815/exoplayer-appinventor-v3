package dev.dreamerslab.media3

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import java.util.UUID

internal object MediaItems {

    private var nextId = 0

    fun create(source: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId("media-${nextId++}")
            .setUri(resolve(source))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title.ifBlank { null }).build())
            .build()

    fun withSubtitle(item: MediaItem, source: String, language: String, label: String): MediaItem {
        val uri = resolve(source)
        val subtitle = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(subtitleMimeType(uri))
            .setLanguage(language.ifBlank { null })
            .setLabel(label.ifBlank { null })
            .build()
        val existing = item.localConfiguration?.subtitleConfigurations.orEmpty()
        return item.buildUpon().setSubtitleConfigurations(existing + subtitle).build()
    }

    fun clipped(item: MediaItem, startMs: Long, endMs: Long): MediaItem =
        item.buildUpon()
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0))
                    .setEndPositionMs(if (endMs > 0) endMs else C.TIME_END_OF_SOURCE)
                    .build()
            )
            .build()

    fun withDrm(item: MediaItem, scheme: UUID, licenseUrl: String, headers: Map<String, String>): MediaItem =
        item.buildUpon()
            .setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(scheme)
                    .setLicenseUri(licenseUrl.ifBlank { null })
                    .setLicenseRequestHeaders(headers)
                    .build()
            )
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
