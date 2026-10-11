package dev.dreamerslab.media3

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import dev.dreamerslab.media3.ui.Choice
import dev.dreamerslab.media3.ui.TrackChoices
import java.util.Locale

internal class TrackSettings(private val player: ExoPlayer) : TrackChoices {

    override val qualities: List<Choice>
        get() {
            val auto = player.trackSelectionParameters.overrides.keys.none { it.type == C.TRACK_TYPE_VIDEO }
            val playing = player.videoSize.height
            val automatic = Choice(if (playing > 0) "Auto (${playing}p)" else "Auto", auto) {
                update { clearOverridesOfType(C.TRACK_TYPE_VIDEO) }
            }
            val resolutions = tracksOf(C.TRACK_TYPE_VIDEO)
                .filter { it.format.height > 0 }
                .distinctBy { it.format.height }
                .sortedByDescending { it.format.height }
                .map { track ->
                    Choice("${track.format.height}p", !auto && track.selected) { update { setOverrideForType(track.override) } }
                }
            return listOf(automatic) + resolutions
        }

    override val subtitles: List<Choice>
        get() {
            val enabled = subtitlesEnabled
            val off = Choice("Off", !enabled) { subtitlesEnabled = false }
            return listOf(off) + tracksOf(C.TRACK_TYPE_TEXT).map { track ->
                Choice(track.label, enabled && track.selected) {
                    update {
                        setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        setOverrideForType(track.override)
                    }
                }
            }
        }

    override val audioTracks: List<Choice>
        get() = tracksOf(C.TRACK_TYPE_AUDIO).map { track ->
            Choice(track.label, track.selected) { update { setOverrideForType(track.override) } }
        }

    override val hasSubtitles: Boolean
        get() = tracksOf(C.TRACK_TYPE_TEXT).isNotEmpty()

    override var subtitlesEnabled: Boolean
        get() = C.TRACK_TYPE_TEXT !in player.trackSelectionParameters.disabledTrackTypes &&
            tracksOf(C.TRACK_TYPE_TEXT).any { it.selected }
        set(value) = update {
            setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !value)
            val textTracks = tracksOf(C.TRACK_TYPE_TEXT)
            if (value && textTracks.none { it.selected }) textTracks.firstOrNull()?.let { setOverrideForType(it.override) }
        }

    override var speed: Float
        get() = player.playbackParameters.speed
        set(value) {
            player.playbackParameters = player.playbackParameters.withSpeed(value)
        }

    var repeatMode: Int
        get() = player.repeatMode
        set(value) {
            player.repeatMode = value
            updatePauseAtEnd()
        }

    override var autoAdvance = true
        set(value) {
            field = value
            updatePauseAtEnd()
        }

    override var upNextSeconds = 5
        set(value) {
            field = value
            updatePauseAtEnd()
        }

    override var loop: Boolean
        get() = repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }

    var preferredAudioLanguage: String
        get() = player.trackSelectionParameters.preferredAudioLanguages.firstOrNull() ?: ""
        set(value) = update { setPreferredAudioLanguage(value.ifBlank { null }) }

    var preferredSubtitleLanguage: String
        get() = player.trackSelectionParameters.preferredTextLanguages.firstOrNull() ?: ""
        set(value) = update { setPreferredTextLanguage(value.ifBlank { null }) }

    var maxVideoHeight: Int
        get() = player.trackSelectionParameters.maxVideoHeight.takeIf { it != Int.MAX_VALUE } ?: 0
        set(value) = update { setMaxVideoSize(Int.MAX_VALUE, if (value > 0) value else Int.MAX_VALUE) }

    var maxVideoBitrateKbps: Int
        get() = player.trackSelectionParameters.maxVideoBitrate.takeIf { it != Int.MAX_VALUE }?.div(1000) ?: 0
        set(value) = update { setMaxVideoBitrate(if (value > 0) value * 1000 else Int.MAX_VALUE) }

    private fun updatePauseAtEnd() {
        val playsOnWithoutPause = repeatMode == Player.REPEAT_MODE_ONE || (autoAdvance && upNextSeconds == 0)
        player.pauseAtEndOfMediaItems = !playsOnWithoutPause
    }

    private fun tracksOf(type: Int): List<Track> =
        player.currentTracks.groups
            .filter { it.type == type }
            .flatMap { group -> (0 until group.length).filter(group::isTrackSupported).map { Track(group, it) } }

    private fun update(change: TrackSelectionParameters.Builder.() -> Unit) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply(change).build()
    }

    private class Track(private val group: Tracks.Group, private val index: Int) {
        val format: Format get() = group.getTrackFormat(index)

        val selected: Boolean get() = group.isTrackSelected(index)

        val override: TrackSelectionOverride get() = TrackSelectionOverride(group.mediaTrackGroup, index)

        val label: String
            get() = format.label
                ?: format.language
                    ?.takeUnless { it.isBlank() || it == C.LANGUAGE_UNDETERMINED }
                    ?.let { Locale.forLanguageTag(it).displayName }
                ?: "Track ${index + 1}"
    }
}
