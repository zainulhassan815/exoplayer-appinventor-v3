package dev.dreamerslab.media3

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.HVArrangement
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.OnPauseListener
import com.google.appinventor.components.runtime.OnResumeListener
import dev.dreamerslab.media3.ui.Chapter
import dev.dreamerslab.media3.ui.DEFAULT_ACCENT
import dev.dreamerslab.media3.ui.PlayerUi

@Extension(
    description = "Video and audio player built on AndroidX Media3, with a modern player UI, gestures, " +
        "playlists, subtitles, quality and speed selection.",
    icon = "icon.png"
)
class Media3Player(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()),
    OnPauseListener, OnResumeListener, OnDestroyListener, Player.Listener {

    private val activity: Activity = container.`$context`()
    private val player = ExoPlayer.Builder(activity)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
    private val tracks = TrackSettings(player)
    private val chapters = mutableListOf<Chapter>()
    private var ui: PlayerUi? = null
    private var resumeOnForeground = false
    private val mainThread = Handler(Looper.getMainLooper())

    private var accentColor = DEFAULT_ACCENT
    private var seekIncrement = 10
    private var controlsTimeout = 3000
    private var gesturesEnabled = true
    private var zoomToFill = false

    init {
        player.pauseAtEndOfMediaItems = true
        player.addListener(this)
        form.registerForOnPause(this)
        form.registerForOnResume(this)
        form.registerForOnDestroy(this)
    }

    @SimpleFunction(description = "Shows the player inside the given arrangement.")
    fun Create(arrangement: HVArrangement) {
        ui?.let { previous ->
            previous.release()
            (previous.parent as? ViewGroup)?.removeView(previous)
        }
        val view = PlayerUi(activity, player, tracks).apply {
            onFullscreenChanged = { FullscreenChanged(it) }
        }
        ui = view
        applySettings()
        (arrangement.view as ViewGroup).addView(
            view,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    @SimpleFunction(description = "Plays the media at the given url or asset name, replacing the queue.")
    fun Play(source: String, title: String) {
        player.setMediaItem(MediaItems.create(source, title))
        player.prepare()
        player.play()
    }

    @SimpleFunction(description = "Adds the media at the given url or asset name to the end of the queue.")
    fun AddToQueue(source: String, title: String) {
        player.addMediaItem(MediaItems.create(source, title))
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    @SimpleFunction(
        description = "Adds a subtitle file (WebVTT, SRT, TTML or SSA) to the most recently added media. " +
            "Language is a code like \"en\"; label is the name shown in the subtitles menu."
    )
    fun AddSubtitle(source: String, language: String, label: String) {
        val index = player.mediaItemCount - 1
        if (index < 0) return
        player.replaceMediaItem(index, MediaItems.withSubtitle(player.getMediaItemAt(index), source, language, label))
    }

    @SimpleFunction(description = "Resumes playback.")
    fun Resume() = player.play()

    @SimpleFunction(description = "Pauses playback.")
    fun Pause() = player.pause()

    @SimpleFunction(description = "Seeks to the given position in milliseconds.")
    fun SeekTo(positionMs: Long) = player.seekTo(positionMs)

    @SimpleFunction(description = "Plays the next media in the queue.")
    fun Next() = player.seekToNextMediaItem()

    @SimpleFunction(description = "Plays the previous media in the queue, or restarts the current one.")
    fun Previous() = player.seekToPrevious()

    @SimpleFunction(description = "Removes all media from the queue and stops playback.")
    fun ClearQueue() = player.clearMediaItems()

    @SimpleFunction(description = "Shows the player fullscreen.")
    fun EnterFullscreen() {
        ui?.enterFullscreen()
    }

    @SimpleFunction(description = "Leaves fullscreen.")
    fun ExitFullscreen() {
        ui?.exitFullscreen()
    }

    @SimpleFunction(description = "Adds a chapter starting at the given second, shown on the seek bar while scrubbing.")
    fun AddChapter(startSeconds: Double, title: String) {
        chapters += Chapter((startSeconds * 1000).toLong(), title)
        ui?.chapters = chapters.toList()
    }

    @SimpleFunction(description = "Removes all chapters.")
    fun ClearChapters() {
        chapters.clear()
        ui?.chapters = emptyList()
    }

    @SimpleProperty(description = "Color of the seek bar, buttons and highlights.")
    fun AccentColor(): Int = accentColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF7C5CFF")
    @SimpleProperty
    fun AccentColor(color: Int) {
        accentColor = color
        ui?.accent = color
    }

    @SimpleProperty(description = "Seconds skipped by the rewind and forward buttons and double taps.")
    fun SeekIncrement(): Int = seekIncrement

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "10")
    @SimpleProperty
    fun SeekIncrement(seconds: Int) {
        seekIncrement = seconds.coerceAtLeast(1)
        ui?.seekIncrementMs = seekIncrement * 1000L
    }

    @SimpleProperty(description = "Milliseconds of inactivity after which the controls hide.")
    fun ControlsTimeout(): Int = controlsTimeout

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "3000")
    @SimpleProperty
    fun ControlsTimeout(milliseconds: Int) {
        controlsTimeout = milliseconds
        ui?.controlsTimeoutMs = milliseconds.toLong()
    }

    @SimpleProperty(
        description = "Enables double tap to seek, hold for 2× speed, and swiping for brightness and volume in fullscreen."
    )
    fun GesturesEnabled(): Boolean = gesturesEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun GesturesEnabled(enabled: Boolean) {
        gesturesEnabled = enabled
        ui?.gesturesEnabled = enabled
    }

    @SimpleProperty(description = "Crops the video to fill the player instead of fitting it.")
    fun ZoomToFill(): Boolean = zoomToFill

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun ZoomToFill(zoom: Boolean) {
        zoomToFill = zoom
        ui?.zoomToFill = zoom
    }

    @SimpleProperty(description = "Repeats the current media.")
    fun Loop(): Boolean = tracks.loop

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun Loop(loop: Boolean) {
        tracks.loop = loop
    }

    @SimpleProperty(description = "Playback speed, from 0.25 to 2.")
    fun PlaybackSpeed(): Float = tracks.speed

    @SimpleProperty
    fun PlaybackSpeed(speed: Float) {
        tracks.speed = speed.coerceIn(0.25f, 2f)
    }

    @SimpleProperty(description = "Whether media is playing.")
    fun IsPlaying(): Boolean = player.isPlaying

    @SimpleProperty(description = "Whether the player is fullscreen.")
    fun IsFullscreen(): Boolean = ui?.isFullscreen == true

    @SimpleProperty(description = "Current position in milliseconds.")
    fun Position(): Long = player.currentPosition

    @SimpleProperty(description = "Duration of the current media in milliseconds, or 0 if unknown.")
    fun Duration(): Long = player.duration.takeIf { it != C.TIME_UNSET } ?: 0

    @SimpleProperty(description = "Title of the current media.")
    fun Title(): String = player.mediaMetadata.title?.toString() ?: ""

    @SimpleEvent(description = "The playback state changed to IDLE, BUFFERING, READY or ENDED.")
    fun PlaybackStateChanged(state: String) {
        dispatch("PlaybackStateChanged", state)
    }

    @SimpleEvent(description = "Playback started or stopped.")
    fun IsPlayingChanged(isPlaying: Boolean) {
        dispatch("IsPlayingChanged", isPlaying)
    }

    @SimpleEvent(description = "Playback failed.")
    fun PlayerError(message: String) {
        dispatch("PlayerError", message)
    }

    @SimpleEvent(description = "Another media in the queue started. Index starts at 1.")
    fun MediaChanged(index: Int, title: String) {
        dispatch("MediaChanged", index, title)
    }

    @SimpleEvent(description = "The player entered or left fullscreen.")
    fun FullscreenChanged(isFullscreen: Boolean) {
        dispatch("FullscreenChanged", isFullscreen)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        PlaybackStateChanged(
            when (playbackState) {
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "IDLE"
            }
        )
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = IsPlayingChanged(isPlaying)

    override fun onPlayerError(error: PlaybackException) = PlayerError(error.message ?: error.errorCodeName)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem != null) MediaChanged(player.currentMediaItemIndex + 1, mediaItem.mediaMetadata.title?.toString() ?: "")
    }

    override fun onPause() {
        resumeOnForeground = player.playWhenReady
        player.pause()
    }

    override fun onResume() {
        if (resumeOnForeground) player.play()
    }

    override fun onDestroy() {
        ui?.release()
        player.release()
    }

    private fun dispatch(event: String, vararg args: Any) {
        mainThread.post { EventDispatcher.dispatchEvent(this, event, *args) }
    }

    private fun applySettings() {
        ui?.apply {
            accent = accentColor
            seekIncrementMs = seekIncrement * 1000L
            controlsTimeoutMs = controlsTimeout.toLong()
            gesturesEnabled = this@Media3Player.gesturesEnabled
            zoomToFill = this@Media3Player.zoomToFill
            chapters = this@Media3Player.chapters.toList()
        }
    }
}
