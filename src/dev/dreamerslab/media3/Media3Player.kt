package dev.dreamerslab.media3

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.common.util.concurrent.ListenableFuture
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
import com.google.appinventor.components.runtime.util.OnInitializeListener
import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.media3.helpers.ClippingConfigurationField
import dev.dreamerslab.media3.helpers.DrmConfigurationField
import dev.dreamerslab.media3.helpers.DrmScheme
import dev.dreamerslab.media3.helpers.FullscreenOrientation
import dev.dreamerslab.media3.helpers.MediaItemField
import dev.dreamerslab.media3.helpers.MediaType
import dev.dreamerslab.media3.helpers.RepeatMode
import dev.dreamerslab.media3.helpers.ResizeMode
import dev.dreamerslab.media3.helpers.SeekMode
import dev.dreamerslab.media3.helpers.SubtitleConfigurationField
import dev.dreamerslab.media3.helpers.SubtitleEdge
import dev.dreamerslab.media3.helpers.ThumbnailsField
import dev.dreamerslab.media3.ui.Chapter
import dev.dreamerslab.media3.ui.Choice
import dev.dreamerslab.media3.ui.Control
import dev.dreamerslab.media3.ui.DEFAULT_ACCENT
import dev.dreamerslab.media3.ui.DEFAULT_SCRIM
import dev.dreamerslab.media3.ui.DEFAULT_SEEK_BAR_BUFFERED
import dev.dreamerslab.media3.ui.DEFAULT_SEEK_BAR_TRACK
import dev.dreamerslab.media3.ui.DEFAULT_SUBTITLE_BACKGROUND
import dev.dreamerslab.media3.ui.Gesture
import dev.dreamerslab.media3.ui.PlayerUi
import dev.dreamerslab.media3.ui.ThumbnailSource
import java.io.File

@Extension(
    description = "Video and audio player built on AndroidX Media3, with a modern player UI, gestures, " +
        "playlists, subtitles, quality and speed selection.",
    icon = "icon.png"
)
class Media3Player(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()),
    OnInitializeListener, OnPauseListener, OnResumeListener, OnDestroyListener, Player.Listener {

    private val activity: Activity = container.`$context`()
    private val mediaSources = MediaSources(activity)
    private val audioEffects = AudioEffects()
    private val renderers = object : DefaultRenderersFactory(activity) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
            DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setAudioProcessors(arrayOf(audioEffects.channelMixer))
                .build()
    }.setEnableDecoderFallback(true)
    private val player = ExoPlayer.Builder(activity, renderers)
        .setMediaSourceFactory(mediaSources)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setDeviceVolumeControlEnabled(true)
        .setHandleAudioBecomingNoisy(true)
        .build()
    private val tracks = TrackSettings(player)
    private val chapters = mutableListOf<Chapter>()
    private var ui: PlayerUi? = null
    private var resumeOnForeground = false
    private val mainThread = Handler(Looper.getMainLooper())
    private val eventsBeforeInitialize = ArrayDeque<() -> Unit>()

    private var accentColor = DEFAULT_ACCENT
    private var seekIncrement = 10
    private var controlsTimeout = 3000
    private var enabledGestures = Gesture.entries.toSet()
    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var fullscreenOrientation = FullscreenOrientation.Auto
    private var autoplay = true
    private var volume = 100
    private var muted = false
    private var audioFocus = true
    private var userAgent = ""
    private var targetLiveOffset = 0L
    private val headers = mutableMapOf<String, String>()
    private var updatingQueue = false
    private var lastAddedId = ""
    private var positionUpdateInterval = 0
    private var cacheSizeMb = 0
    private var preloadSeconds = 0
    private var listeningToDownloads = false
    private var droppedFrames = 0
    private var isBuffering = false
    private val positionUpdates = object : Runnable {
        override fun run() {
            PositionChanged(player.currentPosition, Duration())
            mainThread.postDelayed(this, positionUpdateInterval.toLong())
        }
    }
    private var session: MediaSession? = null
    private var controller: ListenableFuture<MediaController>? = null
    private var showSubtitles = false
    private var showControls = true
    private var keepScreenOn = true
    private var hiddenControls = emptySet<Control>()
    private var playerBackground = Color.BLACK
    private var controlColor = Color.WHITE
    private var scrimColor = DEFAULT_SCRIM
    private var seekBarTrackColor = DEFAULT_SEEK_BAR_TRACK
    private var seekBarBufferedColor = DEFAULT_SEEK_BAR_BUFFERED
    private var cornerRadius = 0
    private var posterImage = ""
    private var showRemainingTime = false
    private var subtitlesShownFor: String? = null
    private var subtitleSize = 100
    private var subtitleTextColor = Color.WHITE
    private var subtitleBackgroundColor = DEFAULT_SUBTITLE_BACKGROUND
    private var subtitleEdge = SubtitleEdge.None
    private var subtitleBottomMargin = 8

    init {
        player.pauseAtEndOfMediaItems = true
        player.addListener(this)
        audioEffects.attach(player.audioSessionId)
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, count: Int, elapsedMs: Long) {
                droppedFrames += count
            }
        })
        form.registerForOnInitialize(this)
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
            onDialogClosed = { DialogClosed() }
        }
        ui = view
        applySettings()
        attach(view, arrangement)
    }

    @SimpleFunction(description = "Plays the media at the given url or asset name, replacing the queue.")
    fun Play(source: String, title: String) = PlayFrom(source, title, 0)

    @SimpleFunction(
        description = "Plays the media at the given url or asset name from the given position in milliseconds, " +
            "replacing the queue."
    )
    fun PlayFrom(source: String, title: String, positionMs: Long) {
        player.setMediaItem(newItem(source, title), positionMs)
        startPlayback()
    }

    @SimpleFunction(description = "Adds the media at the given url or asset name to the end of the queue.")
    fun AddToQueue(source: String, title: String) = insert(player.mediaItemCount, source, title)

    @SimpleFunction(
        description = "Inserts the media at the given url or asset name at the given position in the queue. " +
            "Index starts at 1; one more than the queue length adds it to the end."
    )
    fun InsertAt(index: Int, source: String, title: String) {
        if (!inQueue("InsertAt", index, player.mediaItemCount + 1)) return
        insert(index - 1, source, title)
    }

    @SimpleFunction(description = "Removes the media at the given position from the queue. Index starts at 1.")
    fun RemoveAt(index: Int) {
        if (!inQueue("RemoveAt", index)) return
        player.removeMediaItem(index - 1)
    }

    @SimpleFunction(description = "Moves the media at one position in the queue to another. Indexes start at 1.")
    fun MoveItem(from: Int, to: Int) {
        if (!inQueue("MoveItem", from) || !inQueue("MoveItem", to)) return
        player.moveMediaItem(from - 1, to - 1)
    }

    @SimpleFunction(
        description = "Plays the given media items, replacing the queue. Each media item is a dictionary whose " +
            "keys are MediaItemField choices; only Uri is required."
    )
    fun SetMediaItems(mediaItems: YailList) {
        val items = readMediaItemList("SetMediaItems", mediaItems) ?: return
        rememberLastAdded(items)
        player.setMediaItems(items)
        startPlayback()
    }

    @SimpleFunction(
        description = "Adds a media item to the end of the queue. The media item is a dictionary whose keys are " +
            "MediaItemField choices; only Uri is required."
    )
    fun AddMediaItem(mediaItem: YailDictionary) {
        val item = readMediaItem("AddMediaItem", mediaItem) ?: return
        rememberLastAdded(listOf(item))
        player.addMediaItem(item)
        prepareIfIdle()
    }

    @SimpleFunction(description = "Adds a list of media items to the end of the queue, like AddMediaItem for each.")
    fun AddMediaItems(mediaItems: YailList) {
        val items = readMediaItemList("AddMediaItems", mediaItems) ?: return
        rememberLastAdded(items)
        player.addMediaItems(items)
        prepareIfIdle()
    }

    @SimpleFunction(
        description = "Replaces the media item at the given position in the queue with another, which plays from " +
            "the start if it was the current media. Index starts at 1."
    )
    fun ReplaceMediaItem(index: Int, mediaItem: YailDictionary) {
        if (!inQueue("ReplaceMediaItem", index)) return
        val item = readMediaItem("ReplaceMediaItem", mediaItem) ?: return
        val isCurrent = index - 1 == player.currentMediaItemIndex
        replaceItemAt(index - 1, item, 0)
        QueueChanged(player.mediaItemCount)
        if (isCurrent) MediaChanged(index, item.mediaMetadata.titleText)
    }

    @SimpleFunction(description = "The media item at the given position in the queue, as a dictionary. Index starts at 1.")
    fun GetMediaItemAt(index: Int): YailDictionary {
        if (!inQueue("GetMediaItemAt", index)) return YailDictionary()
        return MediaItemRecord.toYail(player.getMediaItemAt(index - 1))
    }

    @SimpleFunction(
        description = "Changes one field of the media item at the given position in the queue. Index starts at 1."
    )
    fun SetMediaItemField(index: Int, @Options(MediaItemField::class) field: String, value: Any) =
        updateMediaItem("SetMediaItemField", index) { it[field] = value }

    @SimpleFunction(
        description = "Changes one DRM setting of the media item at the given position in the queue. Index starts at 1."
    )
    fun SetDrmConfigurationField(index: Int, @Options(DrmConfigurationField::class) field: String, value: Any) =
        updateMediaItem("SetDrmConfigurationField", index) { it.child(MediaItemField.DrmConfiguration)[field] = value }

    @SimpleFunction(
        description = "Changes where the media item at the given position in the queue starts or ends. Index starts at 1."
    )
    fun SetClippingConfigurationField(index: Int, @Options(ClippingConfigurationField::class) field: String, value: Any) =
        updateMediaItem("SetClippingConfigurationField", index) { it.child(MediaItemField.ClippingConfiguration)[field] = value }

    @SimpleFunction(
        description = "Changes one setting of a subtitle of the media item at the given position in the queue. " +
            "Both indexes start at 1."
    )
    fun SetSubtitleConfigurationField(
        index: Int,
        subtitleIndex: Int,
        @Options(SubtitleConfigurationField::class) field: String,
        value: Any
    ) = updateMediaItem("SetSubtitleConfigurationField", index) { it.subtitle(subtitleIndex)[field] = value }

    @SimpleFunction(
        description = "Changes one thumbnails setting of the media item at the given position in the queue. " +
            "Index starts at 1."
    )
    fun SetThumbnailsField(index: Int, @Options(ThumbnailsField::class) field: String, value: Any) =
        updateMediaItem("SetThumbnailsField", index) { it.child(MediaItemField.Thumbnails)[field] = value }

    private fun updateMediaItem(function: String, index: Int, change: MediaItemRecord.(YailDictionary) -> Unit) {
        if (!inQueue(function, index)) return
        val item = orReportInvalid(function) { MediaItemRecord.update(player.getMediaItemAt(index - 1), change) } ?: return
        replaceItemAt(index - 1, item, player.currentPosition)
    }

    private fun readMediaItem(function: String, mediaItem: YailDictionary): MediaItem? =
        orReportInvalid(function) { MediaItemRecord.fromYail(mediaItem, "mediaItem") }

    private fun readMediaItemList(function: String, mediaItems: YailList): List<MediaItem>? = orReportInvalid(function) {
        mediaItems.toArray().mapIndexed { index, item -> MediaItemRecord.fromYail(item, "mediaItems[${index + 1}]") }
    }

    private fun <T> orReportInvalid(function: String, read: () -> T): T? =
        try {
            read()
        } catch (e: InvalidRecordException) {
            reportError(function, ERROR_INVALID_MEDIA_ITEM, e.message.orEmpty())
            null
        }

    @Deprecated("Use a media item with MediaItemField.SubtitleConfigurations.")
    @SimpleFunction(
        description = "Adds a subtitle file (WebVTT, SRT, TTML or SSA) to the most recently added media. " +
            "Language is a code like \"en\"; label is the name shown in the subtitles menu."
    )
    fun AddSubtitle(source: String, language: String, label: String) =
        updateLastAdded { MediaItems.withSubtitle(it, source, language, label) }

    @Deprecated("Use a media item with MediaItemField.ClippingConfiguration.")
    @SimpleFunction(
        description = "Plays only part of the most recently added media, from the start to the end position in " +
            "milliseconds. An end of 0 plays until the end of the media."
    )
    fun ClipMedia(startMs: Long, endMs: Long) = updateLastAdded { MediaItems.clipped(it, startMs, endMs) }

    @Deprecated("Use a media item with MediaItemField.DrmConfiguration.")
    @SimpleFunction(
        description = "Plays the most recently added media with DRM. Headers is a dictionary of HTTP headers sent " +
            "with license requests; pass an empty dictionary if the license server needs none."
    )
    fun SetDrm(@Options(DrmScheme::class) scheme: String, licenseUrl: String, headers: YailDictionary) {
        val uuid = MediaItems.DRM_SCHEMES.getValue(DrmScheme.fromUnderlyingValue(scheme) ?: return)
        val requestHeaders = headers.entries.associate { (name, value) -> name.toString() to value.toString() }
        updateLastAdded { MediaItems.withDrm(it, uuid, licenseUrl, requestHeaders) }
    }

    @Deprecated("Use a media item with DrmConfigurationField.ClearKeys.")
    @SimpleFunction(
        description = "Plays the most recently added media with ClearKey DRM using known keys, without a license " +
            "server. Keys is a dictionary of key IDs to keys, each 32 hex digits."
    )
    fun SetClearKeys(keys: YailDictionary) {
        val hexKeys = keys.entries.associate { (kid, key) -> kid.toString() to key.toString() }
        val license = runCatching { ClearKeys.license(hexKeys) }
            .getOrElse { return reportError("SetClearKeys", ERROR_CLEAR_KEYS, it.message.orEmpty()) }
        updateLastAdded { ClearKeys.withLicense(it, license) }
    }

    @Deprecated("Use a media item with MediaItemField.MimeType.")
    @SimpleFunction(
        description = "Sets the format of the most recently added media, for URLs that don't end in a known " +
            "extension."
    )
    fun SetMediaType(@Options(MediaType::class) type: String) {
        val mediaType = MediaType.fromUnderlyingValue(type) ?: return
        updateLastAdded { MediaItems.withMimeType(it, MediaItems.MIME_TYPES[mediaType]) }
    }

    @Deprecated("Use a media item with MediaItemField.Artist, Description and ArtworkUri.")
    @SimpleFunction(
        description = "Sets the artist, description and artwork of the most recently added media. Artwork is a " +
            "URL or asset name."
    )
    fun SetMetadata(artist: String, description: String, artworkUrl: String) =
        updateLastAdded { MediaItems.withMetadata(it, artist, description, artworkUrl) }

    @Deprecated("Use a media item with MediaItemField.Thumbnails.")
    @SimpleFunction(
        description = "Shows thumbnails while scrubbing the most recently added media. The sprite is one image (URL " +
            "or asset) with the given columns and rows of equally sized thumbnails, one every intervalSeconds, " +
            "left to right and top to bottom."
    )
    fun SetThumbnails(spriteUrl: String, columns: Int, rows: Int, intervalSeconds: Double) {
        if (columns < 1 || rows < 1 || intervalSeconds <= 0) {
            reportError("SetThumbnails", ERROR_THUMBNAILS, "Columns and rows must be at least 1, and the interval more than 0.")
            return
        }
        val grid = ThumbnailSource.Grid(MediaItems.resolve(spriteUrl), columns, rows, (intervalSeconds * 1000).toLong())
        updateLastAdded { MediaItems.withThumbnails(it, grid) }
    }

    @Deprecated("Use a media item with MediaItemField.Thumbnails.")
    @SimpleFunction(
        description = "Shows thumbnails while scrubbing the most recently added media, from a WebVTT thumbnail " +
            "track (URL or asset) like the storyboard.vtt files video services provide. Each cue points to an " +
            "image, optionally a region of a sprite sheet with #xywh=x,y,width,height."
    )
    fun SetThumbnailTrack(vttUrl: String) =
        updateLastAdded { MediaItems.withThumbnails(it, ThumbnailSource.Track(MediaItems.resolve(vttUrl))) }

    @SimpleFunction(description = "Sends the given HTTP header with every media request.")
    fun SetHeader(name: String, value: String) {
        headers[name] = value
        mediaSources.http.setDefaultRequestProperties(headers)
    }

    @SimpleFunction(description = "Stops sending the headers set with SetHeader.")
    fun ClearHeaders() {
        headers.clear()
        mediaSources.http.setDefaultRequestProperties(headers)
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

    @SimpleFunction(description = "Uses the equalizer preset with the given name from EqualizerPresets.")
    fun UseEqualizerPreset(name: String) {
        if (!audioEffects.usePreset(name)) {
            reportError("UseEqualizerPreset", ERROR_NOT_IN_LIST, "No equalizer preset is called \"$name\".")
        }
    }

    @SimpleFunction(
        description = "Sets the level of an equalizer band in decibels, between EqualizerMinLevel and " +
            "EqualizerMaxLevel. Band starts at 1, in the order of EqualizerBands."
    )
    fun SetBandLevel(band: Int, decibels: Int) {
        if (!inList("SetBandLevel", "Band", band, audioEffects.bandFrequencies.size)) return
        val (min, max) = audioEffects.levelRangeDb
        audioEffects.setBandLevel(band - 1, decibels.coerceIn(min, max))
    }

    @SimpleFunction(description = "Level of an equalizer band in decibels. Band starts at 1.")
    fun BandLevel(band: Int): Int =
        if (inList("BandLevel", "Band", band, audioEffects.bandFrequencies.size)) audioEffects.bandLevelDb(band - 1) else 0

    @SimpleFunction(
        description = "Downloads the media at the given url for offline playback. Playing the same url plays the " +
            "download, even without a connection."
    )
    fun Download(source: String, title: String) {
        val uri = MediaItems.resolve(source)
        val request = DownloadRequest.Builder(uri.toString(), uri)
            .setMimeType(downloadMimeType(uri))
            .setData(title.toByteArray())
            .build()
        downloads()
        DownloadService.sendAddDownload(activity, DownloadsService::class.java, request, false)
    }

    @SimpleFunction(description = "Deletes the download of the media at the given url.")
    fun RemoveDownload(source: String) =
        DownloadService.sendRemoveDownload(activity, DownloadsService::class.java, MediaItems.resolve(source).toString(), false)

    @SimpleFunction(description = "Deletes all downloads.")
    fun RemoveAllDownloads() = DownloadService.sendRemoveAllDownloads(activity, DownloadsService::class.java, false)

    @SimpleFunction(description = "Whether the media at the given url is fully downloaded.")
    fun IsDownloaded(source: String): Boolean = download(source)?.state == Download.STATE_COMPLETED

    @SimpleFunction(description = "How much of the media at the given url has downloaded, from 0 to 100.")
    fun DownloadProgress(source: String): Double =
        download(source)?.percentDownloaded?.takeIf { it >= 0 }?.toDouble() ?: 0.0

    @SimpleFunction(description = "Turns the phone's media volume up by one step.")
    fun IncreaseDeviceVolume() = player.increaseDeviceVolume(0)

    @SimpleFunction(description = "Turns the phone's media volume down by one step.")
    fun DecreaseDeviceVolume() = player.decreaseDeviceVolume(0)

    @SimpleFunction(description = "Deletes the media stored by the cache.")
    fun ClearCache() = OfflineStorage.clearStreamCache()

    @SimpleFunction(description = "Jumps to the live edge of a live stream.")
    fun SeekToLiveEdge() = player.seekToDefaultPosition()

    @SimpleFunction(description = "Resumes playback.")
    fun Resume() {
        prepareIfIdle()
        player.play()
    }

    @SimpleFunction(description = "Pauses playback, keeping the position and the buffered media. Resume plays on.")
    fun Pause() = player.pause()

    @SimpleFunction(description = "Seeks to the given position in milliseconds in the current media.")
    fun SeekTo(positionMs: Long) = player.seekTo(positionMs)

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

    @SimpleFunction(description = "Moves the player into another arrangement, keeping what it plays.")
    fun MoveTo(arrangement: HVArrangement) {
        val view = ui ?: return Create(arrangement)
        view.closeDialog()
        view.exitFullscreen()
        (view.parent as? ViewGroup)?.removeView(view)
        attach(view, arrangement)
    }

    @SimpleFunction(description = "Shows the player in a dialog over the screen. Tapping outside or pressing back closes it.")
    fun ShowAsDialog() {
        ui?.showDialog()
    }

    @SimpleFunction(description = "Closes the dialog shown by ShowAsDialog and returns the player to its arrangement.")
    fun CloseDialog() {
        ui?.closeDialog()
    }

    @SimpleFunction(
        description = "Saves the current video frame as a PNG image and fires FrameCaptured with its path. A " +
            "file name without a folder is saved in the app's files folder. Protected (DRM) video can't be captured."
    )
    fun CaptureFrame(fileName: String) {
        val view = ui ?: return reportError("CaptureFrame", ERROR_CAPTURE, "Call Create before capturing a frame.")
        val file = File(fileName).takeIf { it.isAbsolute } ?: File(activity.getExternalFilesDir(null), fileName)
        view.captureFrame { bitmap ->
            if (bitmap == null) {
                reportError("CaptureFrame", ERROR_CAPTURE, "There is no video frame to capture.")
                return@captureFrame
            }
            Thread {
                runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                    .onSuccess { FrameCaptured(file.absolutePath) }
                    .onFailure { reportError("CaptureFrame", ERROR_CAPTURE, "Couldn't save the frame: ${it.message}") }
            }.start()
        }
    }

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

    @SimpleProperty(
        description = "Keeps playing when the app goes to the background, with a media notification and controls " +
            "on the lock screen and for headsets.",
        category = PropertyCategory.BEHAVIOR
    )
    fun BackgroundPlayback(): Boolean = session != null

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun BackgroundPlayback(enabled: Boolean) {
        if (enabled) startSession() else stopSession()
    }

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
        description = "Image shown before the video starts, and for audio without its own artwork. A URL or " +
            "asset name.",
        category = PropertyCategory.APPEARANCE
    )
    fun PosterImage(): String = posterImage

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    fun PosterImage(image: String) {
        posterImage = image
        ui?.posterUri = posterUri()
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
        description = "Seconds skipped by the rewind and forward buttons and double taps, at least 1.",
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
        controlsTimeout = milliseconds.coerceAtLeast(0)
        ui?.controlsTimeoutMs = controlsTimeout.toLong()
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
    fun RepeatMode(): String = repeatModeName(tracks.repeatMode)

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

    @SimpleProperty(description = "Starts playing as soon as Play, PlayFrom or SetMediaItems loads media.", category = PropertyCategory.BEHAVIOR)
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

    @SimpleProperty(
        description = "Pauses this player when other audio starts, like a call, another app or another player in " +
            "this app, as media players should. Turn off to play independently, for example several players at " +
            "once. Muted players never take part.",
        category = PropertyCategory.BEHAVIOR
    )
    fun AudioFocus(): Boolean = audioFocus

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun AudioFocus(enabled: Boolean) {
        audioFocus = enabled
        applyAudioFocus()
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
        mediaSources.http.setUserAgent(userAgent.ifBlank { null })
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
        mediaSources.setLiveTargetOffset(if (targetLiveOffset > 0) targetLiveOffset else C.TIME_UNSET)
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

    @SimpleProperty(description = "Turns on the equalizer.", category = PropertyCategory.BEHAVIOR)
    fun EqualizerEnabled(): Boolean = audioEffects.equalizerEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun EqualizerEnabled(enabled: Boolean) {
        audioEffects.equalizerEnabled = enabled
    }

    @SimpleProperty(description = "Names of the equalizer presets the device offers, like Rock or Jazz.")
    fun EqualizerPresets(): YailList = YailList.makeList(audioEffects.presets)

    @SimpleProperty(description = "Name of the equalizer preset in use, or empty after setting band levels.")
    fun EqualizerPreset(): String = audioEffects.currentPreset

    @SimpleProperty(description = "Center frequency of each equalizer band in hertz, from low to high.")
    fun EqualizerBands(): YailList = YailList.makeList(audioEffects.bandFrequencies)

    @SimpleProperty(description = "Lowest level an equalizer band can be set to, in decibels.")
    fun EqualizerMinLevel(): Int = audioEffects.levelRangeDb.first

    @SimpleProperty(description = "Highest level an equalizer band can be set to, in decibels.")
    fun EqualizerMaxLevel(): Int = audioEffects.levelRangeDb.second

    @SimpleProperty(description = "Strength of the bass boost, from 0 (off) to 100.", category = PropertyCategory.BEHAVIOR)
    fun BassBoost(): Int = audioEffects.bassBoostPercent

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun BassBoost(percent: Int) {
        audioEffects.bassBoostPercent = percent.coerceIn(0, 100)
    }

    @SimpleProperty(
        description = "Makes quiet media louder, in decibels from 0 (off) to 20.",
        category = PropertyCategory.BEHAVIOR
    )
    fun LoudnessBoost(): Int = audioEffects.loudnessGainDb

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun LoudnessBoost(decibels: Int) {
        audioEffects.loudnessGainDb = decibels.coerceIn(0, 20)
    }

    @SimpleProperty(description = "Plays stereo audio as mono, the same in both ears.", category = PropertyCategory.BEHAVIOR)
    fun Mono(): Boolean = audioEffects.mono

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    fun Mono(mono: Boolean) {
        audioEffects.mono = mono
        applyAudioMix()
    }

    @SimpleProperty(
        description = "Balance between the left and right channels, from -100 (left only) to 100 (right only).",
        category = PropertyCategory.BEHAVIOR
    )
    fun Balance(): Int = audioEffects.balance

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun Balance(balance: Int) {
        audioEffects.balance = balance.coerceIn(-100, 100)
        applyAudioMix()
    }

    @SimpleProperty(
        description = "Megabytes of streamed media kept on the device so it plays again without downloading. " +
            "0 turns the cache off. The size can't change while the app runs once media was cached.",
        category = PropertyCategory.ADVANCED
    )
    fun CacheSize(): Int = cacheSizeMb

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun CacheSize(megabytes: Int) {
        cacheSizeMb = megabytes.coerceAtLeast(0)
        mediaSources.setCacheSize(cacheSizeMb)
    }

    @SimpleProperty(description = "Urls of the downloaded and downloading media.")
    fun Downloads(): YailList {
        val urls = mutableListOf<String>()
        downloads().downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) urls += cursor.download.request.id
        }
        return YailList.makeList(urls)
    }

    @SimpleProperty(description = "Megabytes the cache currently uses on the device.")
    fun CacheUsed(): Double = (OfflineStorage.streamCacheBytes * 100 / (1024 * 1024)) / 100.0

    @SimpleProperty(
        description = "Seconds of the next media in the queue to load in advance, so it starts without waiting. " +
            "0 loads nothing in advance.",
        category = PropertyCategory.BEHAVIOR
    )
    fun PreloadNext(): Int = preloadSeconds

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun PreloadNext(seconds: Int) {
        preloadSeconds = seconds.coerceAtLeast(0)
        player.preloadConfiguration = if (preloadSeconds > 0) {
            ExoPlayer.PreloadConfiguration(preloadSeconds * 1_000_000L)
        } else {
            ExoPlayer.PreloadConfiguration.DEFAULT
        }
    }

    @SimpleProperty(
        description = "The phone's media volume, shared by all apps, between DeviceMinVolume and DeviceMaxVolume. " +
            "Volume sets only this player's volume."
    )
    fun DeviceVolume(): Int = player.deviceVolume

    @SimpleProperty
    fun DeviceVolume(volume: Int) {
        val info = player.deviceInfo
        player.setDeviceVolume(volume.coerceIn(info.minVolume, info.maxVolume), 0)
    }

    @SimpleProperty(description = "Lowest value of DeviceVolume.")
    fun DeviceMinVolume(): Int = player.deviceInfo.minVolume

    @SimpleProperty(description = "Highest value of DeviceVolume.")
    fun DeviceMaxVolume(): Int = player.deviceInfo.maxVolume

    @SimpleProperty(description = "Whether the phone's media volume is muted. Muted silences only this player.")
    fun DeviceMuted(): Boolean = player.isDeviceMuted

    @SimpleProperty
    fun DeviceMuted(muted: Boolean) = player.setDeviceMuted(muted, 0)

    @SimpleProperty(description = "Number of media in the queue.")
    fun QueueLength(): Int = player.mediaItemCount

    @SimpleProperty(description = "Titles of the media in the queue, in order.")
    fun Queue(): YailList =
        YailList.makeList((0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaMetadata.titleText })

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

    @SimpleProperty(
        description = "Milliseconds between PositionChanged events while media plays. 0 sends none.",
        category = PropertyCategory.BEHAVIOR
    )
    fun PositionUpdateInterval(): Int = positionUpdateInterval

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty
    fun PositionUpdateInterval(milliseconds: Int) {
        positionUpdateInterval = milliseconds.coerceAtLeast(0)
        schedulePositionUpdates(player.isPlaying)
    }

    @SimpleProperty(description = "Position up to which media has loaded, in milliseconds.")
    fun BufferedPosition(): Long = player.bufferedPosition

    @SimpleProperty(description = "How much of the current media has loaded, from 0 to 100.")
    fun BufferedPercentage(): Int = player.bufferedPercentage

    @SimpleProperty(description = "Bitrate of the playing video in kilobits per second, or 0 if unknown.")
    fun VideoBitrate(): Int = player.videoFormat?.bitrate?.takeIf { it > 0 }?.div(1000) ?: 0

    @SimpleProperty(description = "Width of the playing video in pixels.")
    fun VideoWidth(): Int = player.videoSize.width

    @SimpleProperty(description = "Height of the playing video in pixels.")
    fun VideoHeight(): Int = player.videoSize.height

    @SimpleProperty(description = "Number of video frames dropped because the device couldn't keep up.")
    fun DroppedFrames(): Int = droppedFrames

    @SimpleProperty(description = "Estimated network speed in kilobits per second, or 0 if unknown.")
    fun BandwidthEstimate(): Long = DefaultBandwidthMeter.getSingletonInstance(activity).bitrateEstimate / 1000

    @SimpleProperty(description = "Artist of the current media.")
    fun Artist(): String = player.mediaMetadata.artistText

    @SimpleProperty(description = "Whether media is playing.")
    fun IsPlaying(): Boolean = player.isPlaying

    @SimpleProperty(description = "The playback state: IDLE, BUFFERING, READY or ENDED.")
    fun PlaybackState(): String = playbackStateName(player.playbackState)

    @SimpleProperty(description = "Whether the player is shown in a dialog.")
    fun IsDialogShowing(): Boolean = ui?.isInDialog == true

    @SimpleProperty(description = "Whether the player is fullscreen.")
    fun IsFullscreen(): Boolean = ui?.isFullscreen == true

    @SimpleProperty(description = "Current position in milliseconds.")
    fun Position(): Long = player.currentPosition

    @SimpleProperty(description = "Duration of the current media in milliseconds, or 0 if unknown.")
    fun Duration(): Long = player.duration.takeIf { it != C.TIME_UNSET } ?: 0

    @SimpleProperty(description = "Title of the current media.")
    fun Title(): String = player.mediaMetadata.titleText

    @SimpleEvent(description = "The playback state changed to IDLE, BUFFERING, READY or ENDED.")
    fun PlaybackStateChanged(state: String) {
        dispatch("PlaybackStateChanged", state)
    }

    @SimpleEvent(description = "Playback started or stopped.")
    fun IsPlayingChanged(isPlaying: Boolean) {
        dispatch("IsPlayingChanged", isPlaying)
    }

    @SimpleEvent(description = "Playback failed.")
    fun PlayerError(message: String, errorCode: Int, category: String) {
        dispatch("PlayerError", message, errorCode, category)
    }

    @SimpleEvent(description = "Sent every PositionUpdateInterval milliseconds while media plays.")
    fun PositionChanged(positionMs: Long, durationMs: Long) {
        dispatch("PositionChanged", positionMs, durationMs)
    }

    @SimpleEvent(description = "The player started or stopped waiting for media to load.")
    fun BufferingChanged(isBuffering: Boolean) {
        dispatch("BufferingChanged", isBuffering)
    }

    @SimpleEvent(description = "The size of the video changed, for example when another quality started playing.")
    fun VideoSizeChanged(width: Int, height: Int) {
        dispatch("VideoSizeChanged", width, height)
    }

    @SimpleEvent(description = "The first frame of the video appeared.")
    fun FirstFrameRendered() {
        dispatch("FirstFrameRendered")
    }

    @SimpleEvent(description = "The playback speed changed.")
    fun SpeedChanged(speed: Float) {
        dispatch("SpeedChanged", speed)
    }

    @SimpleEvent(description = "The repeat mode changed, from blocks or from the loop setting in the player.")
    fun RepeatModeChanged(@Options(RepeatMode::class) mode: String) {
        dispatch("RepeatModeChanged", mode)
    }

    @SimpleEvent(description = "Shuffle was turned on or off.")
    fun ShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        dispatch("ShuffleModeEnabledChanged", shuffleModeEnabled)
    }

    @SimpleEvent(description = "The title or artist of the current media changed.")
    fun MetadataChanged(title: String, artist: String) {
        dispatch("MetadataChanged", title, artist)
    }

    @SimpleEvent(
        description = "A stream announced what is playing now, like the song on an internet radio station. Either " +
            "may be empty; radio stations often send \"Artist - Song\" as the title."
    )
    fun StreamTitleChanged(title: String, artist: String) {
        dispatch("StreamTitleChanged", title, artist)
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

    @SimpleEvent(description = "Media was added to, removed from, moved in or replaced in the queue.")
    fun QueueChanged(length: Int) {
        dispatch("QueueChanged", length)
    }

    @SimpleEvent(
        description = "A download changed state: Queued, Downloading, Completed, Failed, Stopped, Removing, " +
            "Restarting or Removed."
    )
    fun DownloadChanged(source: String, state: String, percent: Double) {
        dispatch("DownloadChanged", source, state, percent)
    }

    @SimpleEvent(
        description = "The subtitle on screen changed. Text holds its lines, or is empty when no subtitle shows. " +
            "Useful to show subtitles your own way, for example with ShowControls off."
    )
    fun SubtitleChanged(text: String) {
        dispatch("SubtitleChanged", text)
    }

    @SimpleEvent(description = "The phone's media volume changed, from this app or outside it.")
    fun DeviceVolumeChanged(volume: Int, muted: Boolean) {
        dispatch("DeviceVolumeChanged", volume, muted)
    }

    @SimpleEvent(description = "CaptureFrame saved a video frame to the given path.")
    fun FrameCaptured(path: String) {
        dispatch("FrameCaptured", path)
    }

    @SimpleEvent(description = "The dialog shown by ShowAsDialog was closed.")
    fun DialogClosed() {
        dispatch("DialogClosed")
    }

    @SimpleEvent(description = "The player entered or left fullscreen.")
    fun FullscreenChanged(isFullscreen: Boolean) {
        dispatch("FullscreenChanged", isFullscreen)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val buffering = playbackState == Player.STATE_BUFFERING
        if (buffering != isBuffering) {
            isBuffering = buffering
            BufferingChanged(buffering)
        }
        PlaybackStateChanged(playbackStateName(playbackState))
    }

    private fun repeatModeName(repeatMode: Int) = REPEAT_MODES.entries.first { it.value == repeatMode }.key.toUnderlyingValue()

    private fun playbackStateName(playbackState: Int) = when (playbackState) {
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "IDLE"
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        schedulePositionUpdates(isPlaying)
        IsPlayingChanged(isPlaying)
    }

    private fun schedulePositionUpdates(isPlaying: Boolean) {
        mainThread.removeCallbacks(positionUpdates)
        if (isPlaying && positionUpdateInterval > 0) mainThread.post(positionUpdates)
    }

    override fun onPlayerError(error: PlaybackException) =
        PlayerError(error.message ?: error.errorCodeName, error.errorCode, errorCategory(error.errorCode))

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        if (videoSize.width > 0) VideoSizeChanged(videoSize.width, videoSize.height)
    }

    override fun onRenderedFirstFrame() = FirstFrameRendered()

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = SpeedChanged(playbackParameters.speed)

    override fun onRepeatModeChanged(repeatMode: Int) = RepeatModeChanged(repeatModeName(repeatMode))

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = ShuffleModeEnabledChanged(shuffleModeEnabled)

    override fun onCues(cueGroup: CueGroup) =
        SubtitleChanged(cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n"))

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = DeviceVolumeChanged(volume, muted)

    override fun onMetadata(metadata: Metadata) {
        var title = ""
        var artist = ""
        for (index in 0 until metadata.length()) {
            when (val entry = metadata[index]) {
                is IcyInfo -> title = entry.title ?: title
                is TextInformationFrame -> when (entry.id) {
                    "TIT2" -> title = entry.values.firstOrNull() ?: title
                    "TPE1" -> artist = entry.values.firstOrNull() ?: artist
                }
            }
        }
        if (title.isNotEmpty() || artist.isNotEmpty()) StreamTitleChanged(title, artist)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) =
        MetadataChanged(mediaMetadata.titleText, mediaMetadata.artistText)

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
        if (mediaItem != null && !updatingQueue) MediaChanged(player.currentMediaItemIndex + 1, mediaItem.mediaMetadata.titleText)
    }

    override fun onInitialize() {
        mainThread.post {
            while (eventsBeforeInitialize.isNotEmpty()) eventsBeforeInitialize.removeFirst()()
        }
    }

    override fun onPause() {
        if (session != null) return
        resumeOnForeground = player.playWhenReady
        player.pause()
    }

    override fun onResume() {
        if (resumeOnForeground) player.play()
        resumeOnForeground = false
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) = audioEffects.attach(audioSessionId)

    override fun onDestroy() {
        if (listeningToDownloads) downloads().removeListener(downloadListener)
        mainThread.removeCallbacks(positionUpdates)
        audioEffects.release()
        stopSession()
        ui?.release()
        player.release()
    }

    private fun posterUri() = posterImage.ifBlank { null }?.let(MediaItems::resolve)

    private fun cornerRadiusPx() = cornerRadius * activity.resources.displayMetrics.density

    private fun enableGesture(gesture: Gesture, enabled: Boolean) {
        enabledGestures = if (enabled) enabledGestures + gesture else enabledGestures - gesture
        ui?.enabledGestures = enabledGestures
    }

    private fun startSession() {
        if (session != null) return
        val reopenApp = PendingIntent.getActivity(
            activity,
            0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val newSession = MediaSession.Builder(activity, player).setSessionActivity(reopenApp).build()
        session = newSession
        PlaybackService.session = newSession
        controller = MediaController.Builder(activity, SessionToken(activity, ComponentName(activity, PlaybackService::class.java)))
            .buildAsync()
    }

    private fun stopSession() {
        controller?.let(MediaController::releaseFuture)
        controller = null
        PlaybackService.session = null
        session?.release()
        session = null
    }

    private fun attach(view: PlayerUi, arrangement: HVArrangement) {
        (arrangement.view as ViewGroup).addView(
            view,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    private fun showControl(control: Control, show: Boolean) {
        hiddenControls = if (show) hiddenControls - control else hiddenControls + control
        ui?.hiddenControls = hiddenControls
    }

    private fun labels(choices: List<Choice>): YailList = YailList.makeList(choices.map { it.label })

    private fun select(function: String, choices: List<Choice>, index: Int) {
        if (inList(function, "Index", index, choices.size)) choices[index - 1].select()
    }

    private fun inList(function: String, name: String, index: Int, size: Int): Boolean {
        if (index in 1..size) return true
        reportError(function, ERROR_NOT_IN_LIST, "$name $index is not in the list (1 to $size).")
        return false
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
        MediaItems.create(source, title).also { rememberLastAdded(listOf(it)) }

    private fun rememberLastAdded(items: List<MediaItem>) {
        items.lastOrNull()?.let { lastAddedId = it.mediaId }
    }

    private fun insert(index: Int, source: String, title: String) {
        player.addMediaItem(index, newItem(source, title))
        prepareIfIdle()
    }

    private fun startPlayback() {
        player.prepare()
        player.playWhenReady = autoplay
    }

    private fun prepareIfIdle() {
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    private fun inQueue(function: String, index: Int, size: Int = player.mediaItemCount): Boolean {
        if (index in 1..size) return true
        reportError(function, ERROR_NOT_IN_QUEUE, "Index $index is not in the queue (1 to ${player.mediaItemCount}).")
        return false
    }

    private fun updateLastAdded(update: (MediaItem) -> MediaItem) {
        val index = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == lastAddedId } ?: return
        replaceItemAt(index, update(player.getMediaItemAt(index)), player.currentPosition)
    }

    private fun replaceItemAt(index: Int, item: MediaItem, positionMs: Long) {
        updatingQueue = true
        player.addMediaItem(index + 1, item)
        if (index == player.currentMediaItemIndex) player.seekTo(index + 1, positionMs)
        player.removeMediaItem(index)
        updatingQueue = false
    }

    private fun seekToItem(function: String, index: Int, positionMs: Long) {
        if (!inQueue(function, index)) return
        player.seekTo(index - 1, positionMs)
        Resume()
    }

    private fun reportError(function: String, errorNumber: Int, message: String) {
        postEvent("ErrorOccurred") { form.ErrorOccurred(this, function, errorNumber, message) }
    }

    private fun errorCategory(code: Int): String = when (code) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "NotFound"
        in 2000..2999 -> "Network"
        in 3000..3999 -> "Format"
        in 4000..4999 -> "Decoder"
        in 6000..6999 -> "DRM"
        else -> "Other"
    }

    private fun downloads(): DownloadManager =
        OfflineStorage.downloadManager(activity, mediaSources.http).also { manager ->
            if (!listeningToDownloads) {
                manager.addListener(downloadListener)
                listeningToDownloads = true
            }
        }

    private fun download(source: String): Download? =
        downloads().downloadIndex.getDownload(MediaItems.resolve(source).toString())

    private fun downloadMimeType(uri: Uri): String? = when (Util.inferContentType(uri)) {
        C.CONTENT_TYPE_HLS -> MimeTypes.APPLICATION_M3U8
        C.CONTENT_TYPE_DASH -> MimeTypes.APPLICATION_MPD
        C.CONTENT_TYPE_SS -> MimeTypes.APPLICATION_SS
        else -> null
    }

    private val downloadListener = object : DownloadManager.Listener {
        override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) =
            DownloadChanged(download.request.id, DOWNLOAD_STATES.getValue(download.state), download.percentDownloaded.coerceAtLeast(0f).toDouble())

        override fun onDownloadRemoved(manager: DownloadManager, download: Download) =
            DownloadChanged(download.request.id, "Removed", 0.0)
    }

    private fun applyAudioMix() {
        if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
            player.seekTo(player.currentPosition)
        }
    }

    private fun applyVolume() {
        player.volume = if (muted) 0f else volume / 100f
        applyAudioFocus()
    }

    private fun applyAudioFocus() {
        val silent = muted || volume == 0
        player.setAudioAttributes(AudioAttributes.DEFAULT, audioFocus && !silent)
    }

    private fun dispatch(event: String, vararg args: Any) {
        postEvent(event) { EventDispatcher.dispatchEvent(this, event, *args) }
    }

    private fun postEvent(event: String, fire: () -> Unit) {
        mainThread.post {
            if (eventsBeforeInitialize.isEmpty() && form.canDispatchEvent(this, event)) fire()
            else eventsBeforeInitialize.addLast(fire)
        }
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
            posterUri = posterUri()
            controlsEnabled = showControls
            keepScreenOnWhilePlaying = this@Media3Player.keepScreenOn
            subtitleScale = subtitleSize / 100f
            subtitleBottomFraction = subtitleBottomMargin / 100f
            chapters = this@Media3Player.chapters.toList()
        }
    }

    private companion object {
        const val ERROR_NOT_IN_QUEUE = 30101
        const val ERROR_NOT_IN_LIST = 30102
        const val ERROR_CAPTURE = 30103
        const val ERROR_THUMBNAILS = 30104
        const val ERROR_CLEAR_KEYS = 30105
        const val ERROR_INVALID_MEDIA_ITEM = 30106
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
        val DOWNLOAD_STATES = mapOf(
            Download.STATE_QUEUED to "Queued",
            Download.STATE_DOWNLOADING to "Downloading",
            Download.STATE_COMPLETED to "Completed",
            Download.STATE_FAILED to "Failed",
            Download.STATE_STOPPED to "Stopped",
            Download.STATE_REMOVING to "Removing",
            Download.STATE_RESTARTING to "Restarting",
        )
        val SEEK_MODES = mapOf(
            SeekMode.Exact to SeekParameters.EXACT,
            SeekMode.ClosestKeyframe to SeekParameters.CLOSEST_SYNC,
        )
        val ORIENTATIONS = mapOf(
            FullscreenOrientation.Auto to null,
            FullscreenOrientation.Landscape to ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            FullscreenOrientation.Portrait to ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
            FullscreenOrientation.Sensor to ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
        )
    }
}

private val MediaMetadata.titleText: String get() = title?.toString().orEmpty()

private val MediaMetadata.artistText: String get() = artist?.toString().orEmpty()
