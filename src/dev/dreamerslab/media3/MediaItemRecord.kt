package dev.dreamerslab.media3

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.appinventor.components.common.OptionList
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.media3.MediaItems.DRM_SCHEMES
import dev.dreamerslab.media3.MediaItems.MIME_TYPES
import dev.dreamerslab.media3.helpers.ClippingConfigurationField
import dev.dreamerslab.media3.helpers.DrmConfigurationField
import dev.dreamerslab.media3.helpers.DrmScheme
import dev.dreamerslab.media3.helpers.LiveConfigurationField
import dev.dreamerslab.media3.helpers.MediaItemField
import dev.dreamerslab.media3.helpers.MediaType
import dev.dreamerslab.media3.helpers.SubtitleConfigurationField
import dev.dreamerslab.media3.helpers.ThumbnailsField
import dev.dreamerslab.media3.ui.ThumbnailSource

internal class InvalidRecordException(message: String) : Exception(message)

internal object MediaItemRecord {

    fun fromYail(value: Any?, path: String, mediaId: String = MediaItems.newMediaId()): MediaItem {
        val fields = Fields(value, path, MediaItemField.entries)
        val drm = fields.record(MediaItemField.DrmConfiguration, DrmConfigurationField.entries)
        val clearKeysLicense = drm?.let(::clearKeysLicense)
        val thumbnails = fields.record(MediaItemField.Thumbnails, ThumbnailsField.entries)?.let(::thumbnails)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(MediaItems.resolve(fields.requiredText(MediaItemField.Uri)))
            .setMimeType(fields.option(MediaItemField.MimeType, MediaType.entries)?.let { MIME_TYPES[it] })
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(fields.text(MediaItemField.Title))
                    .setArtist(fields.text(MediaItemField.Artist))
                    .setDescription(fields.text(MediaItemField.Description))
                    .setArtworkUri(fields.text(MediaItemField.ArtworkUri)?.let(MediaItems::resolve))
                    .setExtras(extras(thumbnails, clearKeysLicense, fields.textDictionary(MediaItemField.RequestHeaders)))
                    .build()
            )
            .setDrmConfiguration(drm?.takeIf { clearKeysLicense == null }?.let(::drm))
            .setSubtitleConfigurations(
                fields.records(MediaItemField.SubtitleConfigurations, SubtitleConfigurationField.entries).map(::subtitle)
            )
            .setClippingConfiguration(
                fields.record(MediaItemField.ClippingConfiguration, ClippingConfigurationField.entries)?.let(::clipping)
                    ?: MediaItem.ClippingConfiguration.UNSET
            )
            .setLiveConfiguration(
                fields.record(MediaItemField.LiveConfiguration, LiveConfigurationField.entries)?.let(::live)
                    ?: MediaItem.LiveConfiguration.UNSET
            )
            .build()
    }

    fun toYail(item: MediaItem): YailDictionary = YailDictionary().apply {
        val local = item.localConfiguration
        setField(MediaItemField.Uri, local?.uri?.toString().orEmpty())
        item.mediaMetadata.title?.let { setField(MediaItemField.Title, it.toString()) }
        item.mediaMetadata.artist?.let { setField(MediaItemField.Artist, it.toString()) }
        item.mediaMetadata.description?.let { setField(MediaItemField.Description, it.toString()) }
        item.mediaMetadata.artworkUri?.let { setField(MediaItemField.ArtworkUri, it.toString()) }
        MIME_TYPES.entries.firstOrNull { it.value != null && it.value == local?.mimeType }?.let {
            setField(MediaItemField.MimeType, it.key)
        }
        (ClearKeys.keys(item)?.let(::clearKeysToYail) ?: local?.drmConfiguration?.let(::drmToYail))?.let {
            setField(MediaItemField.DrmConfiguration, it)
        }
        local?.subtitleConfigurations?.takeIf { it.isNotEmpty() }?.let { subtitles ->
            setField(MediaItemField.SubtitleConfigurations, YailList.makeList(subtitles.map(::subtitleToYail)))
        }
        item.clippingConfiguration.takeIf { it != MediaItem.ClippingConfiguration.UNSET }?.let {
            setField(MediaItemField.ClippingConfiguration, clippingToYail(it))
        }
        ThumbnailSource.from(item.mediaMetadata.extras)?.let { setField(MediaItemField.Thumbnails, thumbnailsToYail(it)) }
        item.liveConfiguration.takeIf { it != MediaItem.LiveConfiguration.UNSET }?.let {
            setField(MediaItemField.LiveConfiguration, liveToYail(it))
        }
        MediaItems.requestHeaders(item).takeIf { it.isNotEmpty() }?.let {
            setField(MediaItemField.RequestHeaders, YailDictionary().apply { putAll(it) })
        }
    }

    private fun YailDictionary.setField(field: OptionList<String>, value: Any) {
        put(field.toUnderlyingValue(), if (value is OptionList<*>) value.toUnderlyingValue() else value)
    }

    private fun YailDictionary.getField(field: OptionList<String>): Any? = get(field.toUnderlyingValue())

    fun YailDictionary.child(field: MediaItemField): YailDictionary =
        getField(field) as? YailDictionary ?: YailDictionary().also { setField(field, it) }

    fun YailDictionary.subtitle(index: Int): YailDictionary {
        val subtitles = (getField(MediaItemField.SubtitleConfigurations) as? YailList)?.toArray().orEmpty()
        val path = "mediaItem.${MediaItemField.SubtitleConfigurations.toUnderlyingValue()}"
        if (subtitles.isEmpty()) throw InvalidRecordException("$path is empty.")
        return subtitles.getOrNull(index - 1) as? YailDictionary
            ?: throw InvalidRecordException("$path has no subtitle $index (1 to ${subtitles.size}).")
    }

    fun update(item: MediaItem, change: MediaItemRecord.(YailDictionary) -> Unit): MediaItem =
        fromYail(toYail(item).also { change(it) }, "mediaItem", item.mediaId)

    private fun drm(fields: Fields<DrmConfigurationField>): MediaItem.DrmConfiguration {
        val scheme = fields.option(DrmConfigurationField.Scheme, DrmScheme.entries) ?: DrmScheme.Widevine
        return MediaItems.drm(
            DRM_SCHEMES.getValue(scheme),
            fields.text(DrmConfigurationField.LicenseUri).orEmpty(),
            fields.textDictionary(DrmConfigurationField.LicenseRequestHeaders).orEmpty()
        )
    }

    private fun clearKeysLicense(fields: Fields<DrmConfigurationField>): String? {
        val keys = fields.textDictionary(DrmConfigurationField.ClearKeys) ?: return null
        fields.rejectBoth(DrmConfigurationField.ClearKeys, DrmConfigurationField.LicenseUri)
        fields.rejectBoth(DrmConfigurationField.ClearKeys, DrmConfigurationField.LicenseRequestHeaders)
        val scheme = fields.option(DrmConfigurationField.Scheme, DrmScheme.entries)
        if (scheme != null && scheme != DrmScheme.ClearKey) {
            throw fields.error(DrmConfigurationField.ClearKeys, "only works with the ClearKey scheme.")
        }
        return try {
            ClearKeys.license(keys)
        } catch (e: IllegalArgumentException) {
            throw fields.error(DrmConfigurationField.ClearKeys, e.message.orEmpty())
        }
    }

    private fun extras(thumbnails: ThumbnailSource?, clearKeysLicense: String?, requestHeaders: Map<String, String>?): Bundle? =
        listOfNotNull<(Bundle?) -> Bundle>(
            thumbnails?.let { source -> source::toExtras },
            clearKeysLicense?.let { license -> { extras -> ClearKeys.toExtras(license, extras) } },
            requestHeaders?.let { headers -> { extras -> MediaItems.requestHeadersExtras(headers, extras) } },
        ).fold(null as Bundle?) { extras, add -> add(extras) }

    private fun thumbnails(fields: Fields<ThumbnailsField>): ThumbnailSource {
        val uri = MediaItems.resolve(fields.requiredText(ThumbnailsField.Uri))
        val grid = listOf(ThumbnailsField.Columns, ThumbnailsField.Rows, ThumbnailsField.IntervalMs).map(fields::number)
        if (grid.all { it == null }) return ThumbnailSource.Track(uri)
        val (columns, rows, intervalMs) = grid
        if (columns == null || rows == null || intervalMs == null || columns < 1 || rows < 1 || intervalMs <= 0) {
            throw fields.error("a sprite sheet needs columns and rows of at least 1, and intervalMs more than 0.")
        }
        return ThumbnailSource.Grid(uri, columns.toInt(), rows.toInt(), intervalMs)
    }

    private fun subtitle(fields: Fields<SubtitleConfigurationField>) = MediaItems.subtitle(
        fields.requiredText(SubtitleConfigurationField.Uri),
        fields.text(SubtitleConfigurationField.Language).orEmpty(),
        fields.text(SubtitleConfigurationField.Label).orEmpty()
    )

    private fun clipping(fields: Fields<ClippingConfigurationField>) = MediaItems.clipping(
        fields.number(ClippingConfigurationField.StartPositionMs) ?: 0,
        fields.number(ClippingConfigurationField.EndPositionMs) ?: 0
    )

    private fun live(fields: Fields<LiveConfigurationField>): MediaItem.LiveConfiguration =
        MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(fields.number(LiveConfigurationField.TargetOffsetMs) ?: C.TIME_UNSET)
            .setMinOffsetMs(fields.number(LiveConfigurationField.MinOffsetMs) ?: C.TIME_UNSET)
            .setMaxOffsetMs(fields.number(LiveConfigurationField.MaxOffsetMs) ?: C.TIME_UNSET)
            .setMinPlaybackSpeed(fields.decimal(LiveConfigurationField.MinPlaybackSpeed) ?: C.RATE_UNSET)
            .setMaxPlaybackSpeed(fields.decimal(LiveConfigurationField.MaxPlaybackSpeed) ?: C.RATE_UNSET)
            .build()

    private fun liveToYail(live: MediaItem.LiveConfiguration) = YailDictionary().apply {
        live.targetOffsetMs.takeIf { it != C.TIME_UNSET }?.let { setField(LiveConfigurationField.TargetOffsetMs, it) }
        live.minOffsetMs.takeIf { it != C.TIME_UNSET }?.let { setField(LiveConfigurationField.MinOffsetMs, it) }
        live.maxOffsetMs.takeIf { it != C.TIME_UNSET }?.let { setField(LiveConfigurationField.MaxOffsetMs, it) }
        live.minPlaybackSpeed.takeIf { it != C.RATE_UNSET }?.let { setField(LiveConfigurationField.MinPlaybackSpeed, it) }
        live.maxPlaybackSpeed.takeIf { it != C.RATE_UNSET }?.let { setField(LiveConfigurationField.MaxPlaybackSpeed, it) }
    }

    private fun drmToYail(drm: MediaItem.DrmConfiguration) = YailDictionary().apply {
        DRM_SCHEMES.entries.firstOrNull { it.value == drm.scheme }?.let { setField(DrmConfigurationField.Scheme, it.key) }
        drm.licenseUri?.let { setField(DrmConfigurationField.LicenseUri, it.toString()) }
        if (drm.licenseRequestHeaders.isNotEmpty()) {
            setField(DrmConfigurationField.LicenseRequestHeaders, YailDictionary().apply { putAll(drm.licenseRequestHeaders) })
        }
    }

    private fun clearKeysToYail(keys: Map<String, String>) = YailDictionary().apply {
        setField(DrmConfigurationField.Scheme, DrmScheme.ClearKey)
        setField(DrmConfigurationField.ClearKeys, YailDictionary().apply { putAll(keys) })
    }

    private fun thumbnailsToYail(source: ThumbnailSource) = YailDictionary().apply {
        when (source) {
            is ThumbnailSource.Track -> setField(ThumbnailsField.Uri, source.uri.toString())
            is ThumbnailSource.Grid -> {
                setField(ThumbnailsField.Uri, source.uri.toString())
                setField(ThumbnailsField.Columns, source.columns)
                setField(ThumbnailsField.Rows, source.rows)
                setField(ThumbnailsField.IntervalMs, source.intervalMs)
            }
        }
    }

    private fun subtitleToYail(subtitle: MediaItem.SubtitleConfiguration) = YailDictionary().apply {
        setField(SubtitleConfigurationField.Uri, subtitle.uri.toString())
        subtitle.language?.let { setField(SubtitleConfigurationField.Language, it) }
        subtitle.label?.let { setField(SubtitleConfigurationField.Label, it) }
    }

    private fun clippingToYail(clipping: MediaItem.ClippingConfiguration) = YailDictionary().apply {
        setField(ClippingConfigurationField.StartPositionMs, clipping.startPositionMs)
        setField(ClippingConfigurationField.EndPositionMs, clipping.endPositionMs.takeIf { it != C.TIME_END_OF_SOURCE } ?: 0L)
    }

    private class Fields<F : OptionList<String>>(value: Any?, private val path: String, private val choices: List<F>) {

        private val group = choices.first().javaClass.simpleName

        private val values: Map<F, Any?> = run {
            val dictionary = value as? YailDictionary ?: throw InvalidRecordException("$path must be a dictionary.")
            dictionary.entries.associate { (key, fieldValue) -> fieldFor(key) to fieldValue }
        }

        private fun fieldFor(key: Any?): F {
            if (key is Enum<*> && key is OptionList<*> && choices.none { it === key }) {
                throw InvalidRecordException("$path: ${key.javaClass.simpleName} ${key.name} doesn't belong here. Use $group keys.")
            }
            val name = nameOf(key)
            return choices.firstOrNull { it.toUnderlyingValue() == name } ?: throw InvalidRecordException(
                "$path: \"$name\" isn't a $group. Use ${choices.joinToString { it.toUnderlyingValue() }}."
            )
        }

        fun requiredText(field: F): String =
            text(field) ?: throw InvalidRecordException("$path.${field.toUnderlyingValue()} is required.")

        fun text(field: F): String? = when (val value = values[field]) {
            null -> null
            is CharSequence, is Number -> value.toString()
            else -> throw invalid(field, "text")
        }

        fun number(field: F): Long? = when (val value = values[field]) {
            null -> null
            is Number -> value.toLong()
            is CharSequence -> value.toString().toLongOrNull() ?: throw invalid(field, "a number")
            else -> throw invalid(field, "a number")
        }

        fun decimal(field: F): Float? = when (val value = values[field]) {
            null -> null
            is Number -> value.toFloat()
            is CharSequence -> value.toString().toFloatOrNull() ?: throw invalid(field, "a number")
            else -> throw invalid(field, "a number")
        }?.also { if (it <= 0f) throw invalid(field, "more than 0") }

        fun <T : OptionList<String>> option(field: F, options: List<T>): T? {
            val value = values[field] ?: return null
            val name = nameOf(value)
            return options.firstOrNull { it.toUnderlyingValue() == name } ?: throw InvalidRecordException(
                "$path.${field.toUnderlyingValue()}: \"$name\" isn't one of ${options.joinToString { it.toUnderlyingValue() }}."
            )
        }

        fun textDictionary(field: F): Map<String, String>? = when (val value = values[field]) {
            null -> null
            is YailDictionary -> value.entries.associate { (name, text) -> name.toString() to text.toString() }
            else -> throw invalid(field, "a dictionary")
        }

        fun rejectBoth(field: F, other: F) {
            if (values[field] != null && values[other] != null) {
                throw InvalidRecordException("$path: use ${field.toUnderlyingValue()} or ${other.toUnderlyingValue()}, not both.")
            }
        }

        fun <G : OptionList<String>> record(field: F, choices: List<G>): Fields<G>? =
            values[field]?.let { Fields(it, "$path.${field.toUnderlyingValue()}", choices) }

        fun <G : OptionList<String>> records(field: F, choices: List<G>): List<Fields<G>> = when (val value = values[field]) {
            null -> emptyList()
            is YailList -> value.toArray().mapIndexed { index, entry ->
                Fields(entry, "$path.${field.toUnderlyingValue()}[${index + 1}]", choices)
            }
            else -> throw invalid(field, "a list")
        }

        fun error(message: String) = InvalidRecordException("$path: $message")

        fun error(field: F, message: String) = InvalidRecordException("$path.${field.toUnderlyingValue()}: $message")

        private fun invalid(field: F, expected: String) =
            InvalidRecordException("$path.${field.toUnderlyingValue()} must be $expected.")

        private fun nameOf(value: Any?): String = (value as? OptionList<*>)?.toUnderlyingValue()?.toString() ?: value.toString()
    }
}
