package dev.dreamerslab.media3

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
import dev.dreamerslab.media3.helpers.MediaItemField
import dev.dreamerslab.media3.helpers.MediaType
import dev.dreamerslab.media3.helpers.SubtitleConfigurationField

internal class InvalidRecordException(message: String) : Exception(message)

internal object MediaItemRecord {

    fun fromYail(value: Any?, path: String, base: MediaItem? = null): MediaItem {
        val fields = Fields(value, path, MediaItemField.values())
        val uri = fields.requiredText(MediaItemField.Uri)
        return (base ?: MediaItems.create(uri, "")).buildUpon()
            .setUri(MediaItems.resolve(uri))
            .setMimeType(fields.option(MediaItemField.MimeType, MediaType.values())?.let { MIME_TYPES[it] })
            .setMediaMetadata(
                (base?.mediaMetadata ?: MediaMetadata.EMPTY).buildUpon()
                    .setTitle(fields.text(MediaItemField.Title))
                    .setArtist(fields.text(MediaItemField.Artist))
                    .build()
            )
            .setDrmConfiguration(fields.record(MediaItemField.DrmConfiguration, DrmConfigurationField.values())?.let(::drm))
            .setSubtitleConfigurations(
                fields.records(MediaItemField.SubtitleConfigurations, SubtitleConfigurationField.values()).map(::subtitle)
            )
            .setClippingConfiguration(
                fields.record(MediaItemField.ClippingConfiguration, ClippingConfigurationField.values())?.let(::clipping)
                    ?: MediaItem.ClippingConfiguration.UNSET
            )
            .build()
    }

    fun toYail(item: MediaItem): YailDictionary = YailDictionary().apply {
        val local = item.localConfiguration
        setField(MediaItemField.Uri, local?.uri?.toString().orEmpty())
        item.mediaMetadata.title?.let { setField(MediaItemField.Title, it.toString()) }
        item.mediaMetadata.artist?.let { setField(MediaItemField.Artist, it.toString()) }
        MIME_TYPES.entries.firstOrNull { it.value != null && it.value == local?.mimeType }?.let {
            setField(MediaItemField.MimeType, it.key)
        }
        local?.drmConfiguration?.let { setField(MediaItemField.DrmConfiguration, drmToYail(it)) }
        local?.subtitleConfigurations?.takeIf { it.isNotEmpty() }?.let { subtitles ->
            setField(MediaItemField.SubtitleConfigurations, YailList.makeList(subtitles.map(::subtitleToYail)))
        }
        item.clippingConfiguration.takeIf { it != MediaItem.ClippingConfiguration.UNSET }?.let {
            setField(MediaItemField.ClippingConfiguration, clippingToYail(it))
        }
    }

    fun YailDictionary.setField(field: OptionList<String>, value: Any) {
        put(field.toUnderlyingValue(), if (value is OptionList<*>) value.toUnderlyingValue() else value)
    }

    fun YailDictionary.getField(field: OptionList<String>): Any? = get(field.toUnderlyingValue())

    fun YailDictionary.child(field: MediaItemField): YailDictionary =
        getField(field) as? YailDictionary ?: YailDictionary().also { setField(field, it) }

    fun YailDictionary.subtitle(index: Int): YailDictionary {
        val subtitles = (getField(MediaItemField.SubtitleConfigurations) as? YailList)?.toArray().orEmpty()
        return subtitles.getOrNull(index - 1) as? YailDictionary ?: throw InvalidRecordException(
            "mediaItem.subtitleConfigurations has no subtitle $index (1 to ${subtitles.size})."
        )
    }

    fun update(item: MediaItem, change: MediaItemRecord.(YailDictionary) -> Unit): MediaItem =
        fromYail(toYail(item).also { change(it) }, "mediaItem", item)

    private fun drm(fields: Fields<DrmConfigurationField>): MediaItem.DrmConfiguration {
        val scheme = fields.option(DrmConfigurationField.Scheme, DrmScheme.values()) ?: DrmScheme.Widevine
        return MediaItems.drm(
            DRM_SCHEMES.getValue(scheme),
            fields.text(DrmConfigurationField.LicenseUri).orEmpty(),
            fields.headers(DrmConfigurationField.LicenseRequestHeaders)
        )
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

    private fun drmToYail(drm: MediaItem.DrmConfiguration) = YailDictionary().apply {
        DRM_SCHEMES.entries.firstOrNull { it.value == drm.scheme }?.let { setField(DrmConfigurationField.Scheme, it.key) }
        drm.licenseUri?.let { setField(DrmConfigurationField.LicenseUri, it.toString()) }
        if (drm.licenseRequestHeaders.isNotEmpty()) {
            setField(DrmConfigurationField.LicenseRequestHeaders, YailDictionary().apply { putAll(drm.licenseRequestHeaders) })
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

    private class Fields<F : OptionList<String>>(value: Any?, private val path: String, private val choices: Array<F>) {

        private val group = choices.first().javaClass.simpleName

        private val values: Map<F, Any?> = run {
            val dictionary = value as? YailDictionary ?: throw InvalidRecordException("$path must be a dictionary.")
            dictionary.entries.associate { (key, fieldValue) -> fieldFor(key) to fieldValue }
        }

        private fun fieldFor(key: Any?): F {
            if (key is Enum<*> && key is OptionList<*> && choices.none { it === key }) {
                throw InvalidRecordException("$path: ${key.javaClass.simpleName} ${key.name} doesn't belong here. Use $group keys.")
            }
            val name = (key as? OptionList<*>)?.toUnderlyingValue()?.toString() ?: key.toString()
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

        fun <T : OptionList<String>> option(field: F, options: Array<T>): T? {
            val value = values[field] ?: return null
            val name = (value as? OptionList<*>)?.toUnderlyingValue()?.toString() ?: value.toString()
            return options.firstOrNull { it.toUnderlyingValue() == name } ?: throw InvalidRecordException(
                "$path.${field.toUnderlyingValue()}: \"$name\" isn't one of ${options.joinToString { it.toUnderlyingValue() }}."
            )
        }

        fun headers(field: F): Map<String, String> = when (val value = values[field]) {
            null -> emptyMap()
            is YailDictionary -> value.entries.associate { (name, header) -> name.toString() to header.toString() }
            else -> throw invalid(field, "a dictionary")
        }

        fun <G : OptionList<String>> record(field: F, choices: Array<G>): Fields<G>? =
            values[field]?.let { Fields(it, "$path.${field.toUnderlyingValue()}", choices) }

        fun <G : OptionList<String>> records(field: F, choices: Array<G>): List<Fields<G>> = when (val value = values[field]) {
            null -> emptyList()
            is YailList -> value.toArray().mapIndexed { index, entry ->
                Fields(entry, "$path.${field.toUnderlyingValue()}[${index + 1}]", choices)
            }
            else -> throw invalid(field, "a list")
        }

        private fun invalid(field: F, expected: String) =
            InvalidRecordException("$path.${field.toUnderlyingValue()} must be $expected.")
    }
}
