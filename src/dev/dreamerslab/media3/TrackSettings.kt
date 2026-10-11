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
        set(value) = player.setPlaybackSpeed(value)

    override var loop: Boolean
        get() = player.repeatMode == Player.REPEAT_MODE_ONE
        set(value) {
            player.repeatMode = if (value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.pauseAtEndOfMediaItems = !value
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
