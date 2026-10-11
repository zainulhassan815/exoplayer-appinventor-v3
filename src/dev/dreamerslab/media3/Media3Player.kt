package dev.dreamerslab.media3

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.PropertyCategory
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
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.media3.helpers.DrmScheme
import dev.dreamerslab.media3.helpers.FullscreenOrientation
import dev.dreamerslab.media3.helpers.MediaType
import dev.dreamerslab.media3.helpers.RepeatMode
import dev.dreamerslab.media3.helpers.ResizeMode
import dev.dreamerslab.media3.helpers.SeekMode
import dev.dreamerslab.media3.helpers.SubtitleEdge
import dev.dreamerslab.media3.ui.Chapter
import dev.dreamerslab.media3.ui.Choice
import dev.dreamerslab.media3.ui.Control
import dev.dreamerslab.media3.ui.DEFAULT_ACCENT
import dev.dreamerslab.media3.ui.Gesture
import dev.dreamerslab.media3.ui.PlayerUi

@Extension(
    description = "Video and audio player built on AndroidX Media3, with a modern player UI, gestures, " +
        "playlists, subtitles, quality and speed selection.",
    icon = "icon.png"
)
class Media3Player(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()),
    OnPauseListener, OnResumeListener, OnDestroyListener, Player.Listener {

    private val activity: Activity = container.`$context`()
    private val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    private val mediaSources = DefaultMediaSourceFactory(DefaultDataSource.Factory(activity, http))
    private val player = ExoPlayer.Builder(activity)
        .setMediaSourceFactory(mediaSources)
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
    private var enabledGestures = Gesture.entries.toSet()
    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var fullscreenOrientation = FullscreenOrientation.Auto
    private var autoplay = true
    private var volume = 100
    private var muted = false
    private var userAgent = ""
    private var targetLiveOffset = 0L
    private val headers = mutableMapOf<String, String>()
    private var updatingQueue = false
    private var lastAddedId = ""
    private var showSubtitles = false
    private var showControls = true
    private var keepScreenOn = true
    private var hiddenControls = emptySet<Control>()
    private var playerBackground = Color.BLACK
    private var controlColor = Color.WHITE
    private var scrimColor = 0xB3000000.toInt()
    private var seekBarTrackColor = 0x40FFFFFF
    private var seekBarBufferedColor = 0x73FFFFFF
    private var cornerRadius = 0
    private var showRemainingTime = false
    private var subtitlesShownFor: String? = null
    private var subtitleSize = 100
    private var subtitleTextColor = Color.WHITE
    private var subtitleBackgroundColor = 0xB8000000.toInt()
    private var subtitleEdge = SubtitleEdge.None
    private var subtitleBottomMargin = 8

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
    fun Play(source: String, title: String) = PlayFrom(source, title, 0)

    @SimpleFunction(
        description = "Plays the media at the given url or asset name from the given position in milliseconds, " +
            "replacing the queue."
    )
    fun PlayFrom(source: String, title: String, positionMs: Long) {
        player.setMediaItem(newItem(source, title), positionMs)
        player.prepare()
        player.playWhenReady = autoplay
    }

    @SimpleFunction(description = "Adds the media at the given url or asset name to the end of the queue.")
    fun AddToQueue(source: String, title: String) = insert(player.mediaItemCount, source, title)

    @SimpleFunction(
        description = "Inserts the media at the given url or asset name at the given position in the queue. " +
            "Index starts at 1; one more than the queue length adds it to the end."
    )
    fun InsertAt(index: Int, source: String, title: String) {
        if (index !in 1..player.mediaItemCount + 1) {
            reportNotInQueue("InsertAt", index)
            return
        }
        insert(index - 1, source, title)
    }

    @SimpleFunction(description = "Removes the media at the given position from the queue. Index starts at 1.")
    fun RemoveAt(index: Int) {
        if (index !in 1..player.mediaItemCount) {
            reportNotInQueue("RemoveAt", index)
            return
        }
        player.removeMediaItem(index - 1)
    }

    @SimpleFunction(description = "Moves the media at one position in the queue to another. Indexes start at 1.")
    fun MoveItem(from: Int, to: Int) {
        val invalid = listOf(from, to).firstOrNull { it !in 1..player.mediaItemCount }
        if (invalid != null) {
            reportNotInQueue("MoveItem", invalid)
            return
        }
        player.moveMediaItem(from - 1, to - 1)
    }

    @SimpleFunction(
        description = "Adds a subtitle file (WebVTT, SRT, TTML or SSA) to the most recently added media. " +
            "Language is a code like \"en\"; label is the name shown in the subtitles menu."
    )
    fun AddSubtitle(source: String, language: String, label: String) =
        updateLastAdded { MediaItems.withSubtitle(it, source, language, label) }

    @SimpleFunction(
        description = "Plays only part of the most recently added media, from the start to the end position in " +
            "milliseconds. An end of 0 plays until the end of the media."
    )
    fun ClipMedia(startMs: Long, endMs: Long) = updateLastAdded { MediaItems.clipped(it, startMs, endMs) }

    @SimpleFunction(
        description = "Plays the most recently added media with DRM. Headers is a dictionary of HTTP headers sent " +
            "with license requests; pass an empty dictionary if the license server needs none."
    )
    fun SetDrm(@Options(DrmScheme::class) scheme: String, licenseUrl: String, headers: YailDictionary) {
        val uuid = DRM_SCHEMES.getValue(DrmScheme.fromUnderlyingValue(scheme) ?: return)
        val requestHeaders = headers.entries.associate { (name, value) -> name.toString() to value.toString() }
        updateLastAdded { MediaItems.withDrm(it, uuid, licenseUrl, requestHeaders) }
    }

    @SimpleFunction(
        description = "Sets the format of the most recently added media, for URLs that don't end in a known " +
            "extension."
    )
    fun SetMediaType(@Options(MediaType::class) type: String) {
        val mediaType = MediaType.fromUnderlyingValue(type) ?: return
        updateLastAdded { MediaItems.withMimeType(it, MIME_TYPES[mediaType]) }
    }

    @SimpleFunction(
        description = "Sets the artist, description and artwork of the most recently added media. Artwork is a " +
            "URL or asset name."
    )
    fun SetMetadata(artist: String, description: String, artworkUrl: String) =
        updateLastAdded { MediaItems.withMetadata(it, artist, description, artworkUrl) }

    @SimpleFunction(description = "Sends the given HTTP header with every media request.")
    fun SetHeader(name: String, value: String) {
        headers[name] = value
        http.setDefaultRequestProperties(headers)
    }

    @SimpleFunction(description = "Stops sending the headers set with SetHeader.")
    fun ClearHeaders() {
        headers.clear()
        http.setDefaultRequestProperties(headers)
    }

    @SimpleFunction(
        description = "Plays the video quality at the given position in VideoQualities. Index starts at 1; " +
            "1 is Auto."
    )
    fun SelectQuality(index: Int) = select("SelectQuality", tracks.qualities, index)

    @SimpleFunction(description = "Plays the audio track at the given position in AudioTracks. Index starts at 1.")
    fun SelectAudioTrack(index: Int) = select("SelectAudioTrack", tracks.audioTracks, index)

    @SimpleFunction(
        description = "Shows the subtitles at the given position in SubtitleTracks. Index starts at 1; 1 turns " +
            "subtitles off."
    )
    fun SelectSubtitle(index: Int) = select("SelectSubtitle", tracks.subtitles, index)

    @SimpleFunction(description = "Jumps to the live edge of a live stream.")
    fun SeekToLiveEdge() = player.seekToDefaultPosition()

    @SimpleFunction(description = "Resumes playback.")
    fun Resume() {
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    @SimpleFunction(description = "Stops playback and releases the media, keeping the queue. Resume plays it again.")
    fun Stop() = player.stop()

    @SimpleFunction(description = "Plays the current media again from the start.")
    fun Replay() {
        player.seekTo(player.currentMediaItemIndex, 0)
        Resume()
    }

    @SimpleFunction(description = "Plays the media at the given position in the queue. Index starts at 1.")
    fun PlayAt(index: Int) = seekToItem("PlayAt", index, 0)

    @SimpleFunction(
        description = "Plays the media at the given position in the queue from the given position in milliseconds. " +
            "Index starts at 1."
    )
    fun SeekToItem(index: Int, positionMs: Long) = seekToItem("SeekToItem", index, positionMs)

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

    @SimpleProperty(
        description = "Shows the built-in controls, gestures and overlays. Turn off to build your own controls " +
            "with blocks.",
        category = PropertyCategory.APPEARANCE
    )
    fun ShowControls(): Boolean = showControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowControls(show: Boolean) {
        showControls = show
        ui?.controlsEnabled = show
    }

    @SimpleProperty(description = "Shows the previous and next buttons when the queue has more than one media.", category = PropertyCategory.APPEARANCE)
    fun ShowPreviousNext(): Boolean = Control.PREVIOUS_NEXT !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowPreviousNext(show: Boolean) = showControl(Control.PREVIOUS_NEXT, show)

    @SimpleProperty(description = "Shows the rewind and forward buttons.", category = PropertyCategory.APPEARANCE)
    fun ShowSeekButtons(): Boolean = Control.SEEK_BUTTONS !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowSeekButtons(show: Boolean) = showControl(Control.SEEK_BUTTONS, show)

    @SimpleProperty(description = "Shows the playback speed button.", category = PropertyCategory.APPEARANCE)
    fun ShowSpeedButton(): Boolean = Control.SPEED !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowSpeedButton(show: Boolean) = showControl(Control.SPEED, show)

    @SimpleProperty(description = "Shows the subtitles button.", category = PropertyCategory.APPEARANCE)
    fun ShowSubtitlesButton(): Boolean = Control.SUBTITLES !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowSubtitlesButton(show: Boolean) = showControl(Control.SUBTITLES, show)

    @SimpleProperty(description = "Shows the settings button.", category = PropertyCategory.APPEARANCE)
    fun ShowSettingsButton(): Boolean = Control.SETTINGS !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowSettingsButton(show: Boolean) = showControl(Control.SETTINGS, show)

    @SimpleProperty(description = "Shows the fullscreen button.", category = PropertyCategory.APPEARANCE)
    fun ShowFullscreenButton(): Boolean = Control.FULLSCREEN !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowFullscreenButton(show: Boolean) = showControl(Control.FULLSCREEN, show)

    @SimpleProperty(description = "Shows the button that locks the controls.", category = PropertyCategory.APPEARANCE)
    fun ShowLockButton(): Boolean = Control.LOCK !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowLockButton(show: Boolean) = showControl(Control.LOCK, show)

    @SimpleProperty(description = "Shows the title of the media in fullscreen.", category = PropertyCategory.APPEARANCE)
    fun ShowTitle(): Boolean = Control.TITLE !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowTitle(show: Boolean) = showControl(Control.TITLE, show)

    @SimpleProperty(description = "Shows the seek bar.", category = PropertyCategory.APPEARANCE)
    fun ShowTimeBar(): Boolean = Control.TIME_BAR !in hiddenControls

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun ShowTimeBar(show: Boolean) = showControl(Control.TIME_BAR, show)

    @SimpleProperty(description = "Keeps the screen on while media plays.", category = PropertyCategory.BEHAVIOR)
    fun KeepScreenOn(): Boolean = keepScreenOn

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun KeepScreenOn(keep: Boolean) {
        keepScreenOn = keep
        ui?.keepScreenOnWhilePlaying = keep
    }

    @SimpleProperty(description = "Color shown around and behind the video.", category = PropertyCategory.APPEARANCE)
    fun BackgroundColor(): Int = playerBackground

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty
    fun BackgroundColor(color: Int) {
        playerBackground = color
        ui?.playerBackground = color
    }

    @SimpleProperty(description = "Color of the control icons and text.", category = PropertyCategory.APPEARANCE)
    fun ControlsColor(): Int = controlColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty
    fun ControlsColor(color: Int) {
        controlColor = color
        ui?.controlColor = color
    }

    @SimpleProperty(description = "Color of the shading behind the controls.", category = PropertyCategory.APPEARANCE)
    fun ScrimColor(): Int = scrimColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HB3000000")
    @SimpleProperty
    fun ScrimColor(color: Int) {
        scrimColor = color
        ui?.scrimColor = color
    }

    @SimpleProperty(description = "Color of the unplayed part of the seek bar.", category = PropertyCategory.APPEARANCE)
    fun SeekBarTrackColor(): Int = seekBarTrackColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H40FFFFFF")
    @SimpleProperty
    fun SeekBarTrackColor(color: Int) {
        seekBarTrackColor = color
        ui?.seekBarTrackColor = color
    }

    @SimpleProperty(description = "Color of the buffered part of the seek bar.", category = PropertyCategory.APPEARANCE)
    fun SeekBarBufferedColor(): Int = seekBarBufferedColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H73FFFFFF")
    @SimpleProperty
    fun SeekBarBufferedColor(color: Int) {
        seekBarBufferedColor = color
        ui?.seekBarBufferedColor = color
    }

    @SimpleProperty(description = "Roundness of the player's corners, in pixels.", category = PropertyCategory.APPEARANCE)
    fun CornerRadius(): Int = cornerRadius

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun CornerRadius(radius: Int) {
        cornerRadius = radius
        ui?.cornerRadius = cornerRadiusPx()
    }

    @SimpleProperty(
        description = "Shows the time left instead of the duration next to the position.",
        category = PropertyCategory.APPEARANCE
    )
    fun ShowRemainingTime(): Boolean = showRemainingTime

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun ShowRemainingTime(show: Boolean) {
        showRemainingTime = show
        ui?.showRemainingTime = show
    }

    @SimpleProperty(description = "Color of the seek bar, buttons and highlights.", category = PropertyCategory.APPEARANCE)
    fun AccentColor(): Int = accentColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF7C5CFF")
    @SimpleProperty
    fun AccentColor(color: Int) {
        accentColor = color
        ui?.accent = color
    }

    @SimpleProperty(description = "How the video fits the player.", category = PropertyCategory.APPEARANCE)
    @Options(ResizeMode::class)
    fun ResizeMode(): String =
        RESIZE_MODES.entries.first { it.value == (ui?.resizeMode ?: resizeMode) }.key.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["Fit", "Fill", "Zoom", "Fixed width", "Fixed height"],
        defaultValue = "Fit"
    )
    @SimpleProperty
    fun ResizeMode(@Options(ResizeMode::class) mode: String) {
        resizeMode = RESIZE_MODES.getValue(ResizeMode.fromUnderlyingValue(mode) ?: return)
        ui?.resizeMode = resizeMode
    }

    @SimpleProperty(description = "Orientation of the screen in fullscreen.", category = PropertyCategory.APPEARANCE)
    @Options(FullscreenOrientation::class)
    fun FullscreenOrientation(): String = fullscreenOrientation.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["Auto", "Landscape", "Portrait", "Sensor"],
        defaultValue = "Auto"
    )
    @SimpleProperty
    fun FullscreenOrientation(@Options(FullscreenOrientation::class) orientation: String) {
        fullscreenOrientation = FullscreenOrientation.fromUnderlyingValue(orientation) ?: return
        ui?.fullscreenOrientation = ORIENTATIONS[fullscreenOrientation]
    }

    @SimpleProperty(
        description = "Seconds skipped by the rewind and forward buttons and double taps.",
        category = PropertyCategory.BEHAVIOR
    )
    fun SeekIncrement(): Int = seekIncrement

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "10")
    @SimpleProperty
    fun SeekIncrement(seconds: Int) {
        seekIncrement = seconds.coerceAtLeast(1)
        ui?.seekIncrementMs = seekIncrement * 1000L
    }

    @SimpleProperty(
        description = "Milliseconds of inactivity after which the controls hide.",
        category = PropertyCategory.BEHAVIOR
    )
    fun ControlsTimeout(): Int = controlsTimeout

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "3000")
    @SimpleProperty
    fun ControlsTimeout(milliseconds: Int) {
        controlsTimeout = milliseconds
        ui?.controlsTimeoutMs = milliseconds.toLong()
    }

    @SimpleProperty(description = "Double tap the sides to rewind or forward, and the middle to play or pause.", category = PropertyCategory.BEHAVIOR)
    fun DoubleTapToSeek(): Boolean = Gesture.DOUBLE_TAP_SEEK in enabledGestures

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun DoubleTapToSeek(enabled: Boolean) = enableGesture(Gesture.DOUBLE_TAP_SEEK, enabled)

    @SimpleProperty(description = "Touch and hold to play at 2× speed.", category = PropertyCategory.BEHAVIOR)
    fun HoldForSpeed(): Boolean = Gesture.HOLD_FOR_SPEED in enabledGestures

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun HoldForSpeed(enabled: Boolean) = enableGesture(Gesture.HOLD_FOR_SPEED, enabled)

    @SimpleProperty(description = "Swipe up or down on the left in fullscreen to change the brightness.", category = PropertyCategory.BEHAVIOR)
    fun SwipeForBrightness(): Boolean = Gesture.SWIPE_BRIGHTNESS in enabledGestures

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun SwipeForBrightness(enabled: Boolean) = enableGesture(Gesture.SWIPE_BRIGHTNESS, enabled)

    @SimpleProperty(description = "Swipe up or down on the right in fullscreen to change the volume.", category = PropertyCategory.BEHAVIOR)
    fun SwipeForVolume(): Boolean = Gesture.SWIPE_VOLUME in enabledGestures

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun SwipeForVolume(enabled: Boolean) = enableGesture(Gesture.SWIPE_VOLUME, enabled)

    @SimpleProperty(description = "Pinch out to zoom the video to fill the player, and in to fit it.", category = PropertyCategory.BEHAVIOR)
    fun PinchToZoom(): Boolean = Gesture.PINCH_TO_ZOOM in enabledGestures

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun PinchToZoom(enabled: Boolean) = enableGesture(Gesture.PINCH_TO_ZOOM, enabled)

    @SimpleProperty(description = "Whether the current media or the whole queue repeats.", category = PropertyCategory.BEHAVIOR)
    @Options(RepeatMode::class)
    fun RepeatMode(): String = REPEAT_MODES.entries.first { it.value == tracks.repeatMode }.key.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["Off", "One", "All"],
        defaultValue = "Off"
    )
    @SimpleProperty
    fun RepeatMode(@Options(RepeatMode::class) mode: String) {
        tracks.repeatMode = REPEAT_MODES.getValue(RepeatMode.fromUnderlyingValue(mode) ?: return)
    }

    @SimpleProperty(description = "Shuffles the order the queue plays in.", category = PropertyCategory.BEHAVIOR)
    fun Shuffle(): Boolean = player.shuffleModeEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun Shuffle(shuffle: Boolean) {
        player.shuffleModeEnabled = shuffle
    }

    @SimpleProperty(description = "Starts playing as soon as media is played or added.", category = PropertyCategory.BEHAVIOR)
    fun Autoplay(): Boolean = autoplay

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun Autoplay(autoplay: Boolean) {
        this.autoplay = autoplay
    }

    @SimpleProperty(description = "Volume of the player, from 0 to 100.", category = PropertyCategory.BEHAVIOR)
    fun Volume(): Int = volume

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "100")
    @SimpleProperty
    fun Volume(volume: Int) {
        this.volume = volume.coerceIn(0, 100)
        applyVolume()
    }

    @SimpleProperty(description = "Silences the player without changing its volume.", category = PropertyCategory.BEHAVIOR)
    fun Muted(): Boolean = muted

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun Muted(muted: Boolean) {
        this.muted = muted
        applyVolume()
    }

    @SimpleProperty(
        description = "Pitch of the audio, from 0.5 to 2. 1 is the original pitch.",
        category = PropertyCategory.BEHAVIOR
    )
    fun Pitch(): Float = player.playbackParameters.pitch

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT, defaultValue = "1.0")
    @SimpleProperty
    fun Pitch(pitch: Float) {
        player.playbackParameters = player.playbackParameters.withPitch(pitch.coerceIn(0.5f, 2f))
    }

    @SimpleProperty(description = "Skips silent parts of the audio.", category = PropertyCategory.BEHAVIOR)
    fun SkipSilence(): Boolean = player.skipSilenceEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun SkipSilence(skip: Boolean) {
        player.skipSilenceEnabled = skip
    }

    @SimpleProperty(description = "How precisely SeekTo and the seek bar seek.", category = PropertyCategory.BEHAVIOR)
    @Options(SeekMode::class)
    fun SeekMode(): String = SEEK_MODES.entries.first { it.value == player.seekParameters }.key.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["Exact", "Closest keyframe"],
        defaultValue = "Exact"
    )
    @SimpleProperty
    fun SeekMode(@Options(SeekMode::class) mode: String) {
        player.setSeekParameters(SEEK_MODES.getValue(SeekMode.fromUnderlyingValue(mode) ?: return))
    }

    @SimpleProperty(description = "Playback speed, from 0.25 to 2.", category = PropertyCategory.BEHAVIOR)
    fun PlaybackSpeed(): Float = tracks.speed

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_FLOAT, defaultValue = "1.0")
    @SimpleProperty
    fun PlaybackSpeed(speed: Float) {
        tracks.speed = speed.coerceIn(0.25f, 2f)
    }

    @SimpleProperty(
        description = "User agent sent with media requests. Leave empty for the default.",
        category = PropertyCategory.ADVANCED
    )
    fun UserAgent(): String = userAgent

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    fun UserAgent(userAgent: String) {
        this.userAgent = userAgent
        http.setUserAgent(userAgent.ifBlank { null })
    }

    @SimpleProperty(
        description = "How far behind the live edge live streams play, in milliseconds. 0 uses the stream's own " +
            "setting. Applies to media added afterwards.",
        category = PropertyCategory.ADVANCED
    )
    fun TargetLiveOffset(): Long = targetLiveOffset

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun TargetLiveOffset(milliseconds: Long) {
        targetLiveOffset = milliseconds.coerceAtLeast(0)
        mediaSources.setLiveTargetOffsetMs(if (targetLiveOffset > 0) targetLiveOffset else C.TIME_UNSET)
    }

    @SimpleProperty(description = "Whether the current media is a live stream.")
    fun IsLive(): Boolean = player.isCurrentMediaItemLive

    @SimpleProperty(description = "How far behind the live edge the live stream plays, in milliseconds, or 0.")
    fun LiveOffset(): Long = player.currentLiveOffset.takeIf { it != C.TIME_UNSET } ?: 0

    @SimpleProperty(description = "Video qualities of the current media, starting with Auto.")
    fun VideoQualities(): YailList = labels(tracks.qualities)

    @SimpleProperty(description = "Audio tracks of the current media.")
    fun AudioTracks(): YailList = labels(tracks.audioTracks)

    @SimpleProperty(description = "Subtitles of the current media, starting with Off.")
    fun SubtitleTracks(): YailList = labels(tracks.subtitles)

    @SimpleProperty(
        description = "Language of the audio track to play when media has several, as a code like \"en\". " +
            "Empty uses the device language.",
        category = PropertyCategory.BEHAVIOR
    )
    fun PreferredAudioLanguage(): String = tracks.preferredAudioLanguage

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    fun PreferredAudioLanguage(language: String) {
        tracks.preferredAudioLanguage = language
    }

    @SimpleProperty(
        description = "Language of the subtitles to show, as a code like \"en\". Subtitles in this language turn " +
            "on automatically.",
        category = PropertyCategory.BEHAVIOR
    )
    fun PreferredSubtitleLanguage(): String = tracks.preferredSubtitleLanguage

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    fun PreferredSubtitleLanguage(language: String) {
        tracks.preferredSubtitleLanguage = language
    }

    @SimpleProperty(description = "Turns on subtitles for every new media that has them.", category = PropertyCategory.BEHAVIOR)
    fun ShowSubtitles(): Boolean = showSubtitles

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun ShowSubtitles(show: Boolean) {
        showSubtitles = show
    }

    @SimpleProperty(
        description = "Highest video height to play, like 720 for 720p, to save data. 0 has no limit.",
        category = PropertyCategory.BEHAVIOR
    )
    fun MaxVideoHeight(): Int = tracks.maxVideoHeight

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun MaxVideoHeight(height: Int) {
        tracks.maxVideoHeight = height
    }

    @SimpleProperty(
        description = "Highest video bitrate to play, in kilobits per second, to save data. 0 has no limit.",
        category = PropertyCategory.BEHAVIOR
    )
    fun MaxVideoBitrate(): Int = tracks.maxVideoBitrateKbps

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun MaxVideoBitrate(kbps: Int) {
        tracks.maxVideoBitrateKbps = kbps
    }

    @SimpleProperty(description = "Size of subtitles in percent of the default size.", category = PropertyCategory.APPEARANCE)
    fun SubtitleSize(): Int = subtitleSize

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "100")
    @SimpleProperty
    fun SubtitleSize(percent: Int) {
        subtitleSize = percent.coerceIn(25, 400)
        ui?.subtitleScale = subtitleSize / 100f
    }

    @SimpleProperty(description = "Color of the subtitle text.", category = PropertyCategory.APPEARANCE)
    fun SubtitleTextColor(): Int = subtitleTextColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty
    fun SubtitleTextColor(color: Int) {
        subtitleTextColor = color
        ui?.captionStyle = captionStyle()
    }

    @SimpleProperty(description = "Color behind the subtitle text.", category = PropertyCategory.APPEARANCE)
    fun SubtitleBackgroundColor(): Int = subtitleBackgroundColor

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HB8000000")
    @SimpleProperty
    fun SubtitleBackgroundColor(color: Int) {
        subtitleBackgroundColor = color
        ui?.captionStyle = captionStyle()
    }

    @SimpleProperty(description = "Edge drawn around the subtitle text, in black.", category = PropertyCategory.APPEARANCE)
    @Options(SubtitleEdge::class)
    fun SubtitleEdge(): String = subtitleEdge.toUnderlyingValue()

    @DesignerProperty(
        editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
        editorArgs = ["None", "Outline", "Drop shadow", "Raised", "Depressed"],
        defaultValue = "None"
    )
    @SimpleProperty
    fun SubtitleEdge(@Options(SubtitleEdge::class) edge: String) {
        subtitleEdge = SubtitleEdge.fromUnderlyingValue(edge) ?: return
        ui?.captionStyle = captionStyle()
    }

    @SimpleProperty(
        description = "Space below the subtitles, in percent of the player's height.",
        category = PropertyCategory.APPEARANCE
    )
    fun SubtitleBottomMargin(): Int = subtitleBottomMargin

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "8")
    @SimpleProperty
    fun SubtitleBottomMargin(percent: Int) {
        subtitleBottomMargin = percent.coerceIn(0, 90)
        ui?.subtitleBottomFraction = subtitleBottomMargin / 100f
    }

    @SimpleProperty(description = "Number of media in the queue.")
    fun QueueLength(): Int = player.mediaItemCount

    @SimpleProperty(description = "Titles of the media in the queue, in order.")
    fun Queue(): YailList =
        YailList.makeList((0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaMetadata.title?.toString() ?: "" })

    @SimpleProperty(description = "Position of the current media in the queue, starting at 1, or 0 if the queue is empty.")
    fun CurrentIndex(): Int = if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1

    @SimpleProperty(description = "Whether there is media after the current one.")
    fun HasNext(): Boolean = player.hasNextMediaItem()

    @SimpleProperty(description = "Whether there is media before the current one.")
    fun HasPrevious(): Boolean = player.hasPreviousMediaItem()

    @SimpleProperty(
        description = "Plays the next media when one ends. When off, the player stops after each media and offers " +
            "to replay it or play the next one.",
        category = PropertyCategory.BEHAVIOR
    )
    fun AutoAdvance(): Boolean = tracks.autoAdvance

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun AutoAdvance(autoAdvance: Boolean) {
        tracks.autoAdvance = autoAdvance
    }

    @SimpleProperty(
        description = "Seconds the up next countdown runs before playing the next media. 0 plays it right away.",
        category = PropertyCategory.BEHAVIOR
    )
    fun UpNextCountdown(): Int = tracks.upNextSeconds

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "5")
    @SimpleProperty
    fun UpNextCountdown(seconds: Int) {
        tracks.upNextSeconds = seconds.coerceAtLeast(0)
    }

    @SimpleProperty(description = "Artist of the current media.")
    fun Artist(): String = player.mediaMetadata.artist?.toString() ?: ""

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

    @SimpleEvent(
        description = "The video qualities, audio tracks or subtitles of the current media changed, for example " +
            "once it loaded."
    )
    fun TracksChanged() {
        dispatch("TracksChanged")
    }

    @SimpleEvent(description = "Media was added to, removed from or moved in the queue.")
    fun QueueChanged(length: Int) {
        dispatch("QueueChanged", length)
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

    override fun onTracksChanged(changed: Tracks) {
        val mediaId = player.currentMediaItem?.mediaId
        if (showSubtitles && mediaId != subtitlesShownFor && tracks.hasSubtitles) {
            subtitlesShownFor = mediaId
            tracks.subtitlesEnabled = true
        }
        TracksChanged()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED && !updatingQueue) QueueChanged(player.mediaItemCount)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem != null && !updatingQueue) MediaChanged(player.currentMediaItemIndex + 1, mediaItem.mediaMetadata.title?.toString() ?: "")
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

    private fun cornerRadiusPx() = cornerRadius * activity.resources.displayMetrics.density

    private fun enableGesture(gesture: Gesture, enabled: Boolean) {
        enabledGestures = if (enabled) enabledGestures + gesture else enabledGestures - gesture
        ui?.enabledGestures = enabledGestures
    }

    private fun showControl(control: Control, show: Boolean) {
        hiddenControls = if (show) hiddenControls - control else hiddenControls + control
        ui?.hiddenControls = hiddenControls
    }

    private fun labels(choices: List<Choice>): YailList = YailList.makeList(choices.map { it.label })

    private fun select(function: String, choices: List<Choice>, index: Int) {
        val choice = choices.getOrNull(index - 1)
        if (choice == null) {
            reportError(function, ERROR_NOT_IN_LIST, "Index $index is not in the list (1 to ${choices.size}).")
            return
        }
        choice.select()
    }

    private fun captionStyle() = CaptionStyleCompat(
        subtitleTextColor,
        subtitleBackgroundColor,
        Color.TRANSPARENT,
        CAPTION_EDGES.getValue(subtitleEdge),
        Color.BLACK,
        null
    )

    private fun newItem(source: String, title: String): MediaItem =
        MediaItems.create(source, title).also { lastAddedId = it.mediaId }

    private fun insert(index: Int, source: String, title: String) {
        player.addMediaItem(index, newItem(source, title))
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    private fun reportNotInQueue(function: String, index: Int) =
        reportError(function, ERROR_NOT_IN_QUEUE, "Index $index is not in the queue (1 to ${player.mediaItemCount}).")

    private fun updateLastAdded(update: (MediaItem) -> MediaItem) {
        val index = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == lastAddedId } ?: return
        updatingQueue = true
        player.addMediaItem(index + 1, update(player.getMediaItemAt(index)))
        if (index == player.currentMediaItemIndex) player.seekTo(index + 1, player.currentPosition)
        player.removeMediaItem(index)
        updatingQueue = false
    }

    private fun seekToItem(function: String, index: Int, positionMs: Long) {
        if (index !in 1..player.mediaItemCount) {
            reportNotInQueue(function, index)
            return
        }
        player.seekTo(index - 1, positionMs)
        Resume()
    }

    private fun reportError(function: String, errorNumber: Int, message: String) {
        mainThread.post { form.ErrorOccurred(this, function, errorNumber, message) }
    }

    private fun applyVolume() {
        player.volume = if (muted) 0f else volume / 100f
    }

    private companion object {
        const val ERROR_NOT_IN_QUEUE = 30101
        const val ERROR_NOT_IN_LIST = 30102
        val CAPTION_EDGES = mapOf(
            SubtitleEdge.None to CaptionStyleCompat.EDGE_TYPE_NONE,
            SubtitleEdge.Outline to CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            SubtitleEdge.DropShadow to CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            SubtitleEdge.Raised to CaptionStyleCompat.EDGE_TYPE_RAISED,
            SubtitleEdge.Depressed to CaptionStyleCompat.EDGE_TYPE_DEPRESSED,
        )
        val RESIZE_MODES = mapOf(
            ResizeMode.Fit to AspectRatioFrameLayout.RESIZE_MODE_FIT,
            ResizeMode.Fill to AspectRatioFrameLayout.RESIZE_MODE_FILL,
            ResizeMode.Zoom to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            ResizeMode.FixedWidth to AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
            ResizeMode.FixedHeight to AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        )
        val REPEAT_MODES = mapOf(
            RepeatMode.Off to Player.REPEAT_MODE_OFF,
            RepeatMode.One to Player.REPEAT_MODE_ONE,
            RepeatMode.All to Player.REPEAT_MODE_ALL,
        )
        val SEEK_MODES = mapOf(
            SeekMode.Exact to SeekParameters.EXACT,
            SeekMode.ClosestKeyframe to SeekParameters.CLOSEST_SYNC,
        )
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
        val ORIENTATIONS = mapOf(
            FullscreenOrientation.Auto to null,
            FullscreenOrientation.Landscape to ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            FullscreenOrientation.Portrait to ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            FullscreenOrientation.Sensor to ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
        )
    }

    private fun dispatch(event: String, vararg args: Any) {
        mainThread.post { EventDispatcher.dispatchEvent(this, event, *args) }
    }

    private fun applySettings() {
        ui?.apply {
            accent = accentColor
            seekIncrementMs = seekIncrement * 1000L
            controlsTimeoutMs = controlsTimeout.toLong()
            enabledGestures = this@Media3Player.enabledGestures
            resizeMode = this@Media3Player.resizeMode
            fullscreenOrientation = ORIENTATIONS[this@Media3Player.fullscreenOrientation]
            captionStyle = this@Media3Player.captionStyle()
            hiddenControls = this@Media3Player.hiddenControls
            playerBackground = this@Media3Player.playerBackground
            controlColor = this@Media3Player.controlColor
            scrimColor = this@Media3Player.scrimColor
            seekBarTrackColor = this@Media3Player.seekBarTrackColor
            seekBarBufferedColor = this@Media3Player.seekBarBufferedColor
            cornerRadius = cornerRadiusPx()
            showRemainingTime = this@Media3Player.showRemainingTime
            controlsEnabled = showControls
            keepScreenOnWhilePlaying = this@Media3Player.keepScreenOn
            subtitleScale = subtitleSize / 100f
            subtitleBottomFraction = subtitleBottomMargin / 100f
            chapters = this@Media3Player.chapters.toList()
        }
    }
}
