package dev.dreamerslab.media3

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes

internal object MediaItems {

    fun create(source: String, title: String): MediaItem =
        MediaItem.Builder()
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

    private fun resolve(source: String): Uri = when {
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
