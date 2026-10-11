package dev.dreamerslab.media3.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.media.AudioManager
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlin.math.abs
import kotlin.math.roundToInt

internal class Chapter(val startMs: Long, val title: String)

internal enum class Control { PREVIOUS_NEXT, SEEK_BUTTONS, SPEED, SUBTITLES, SETTINGS, FULLSCREEN, LOCK, TITLE, TIME_BAR }

private const val LIVE_EDGE_TOLERANCE_MS = 5_000L

@SuppressLint("ViewConstructor")
internal class PlayerUi(
    private val activity: Activity,
    private val player: ExoPlayer,
    trackChoices: TrackChoices,
) : FrameLayout(activity), Player.Listener, PlaybackSettings, TrackChoices by trackChoices {

    var onFullscreenChanged: (Boolean) -> Unit = {}

    var accent: Int = DEFAULT_ACCENT
        set(value) {
            field = value
            applyAccent()
        }

    var seekIncrementMs: Long = 10_000
        set(value) {
            field = value
            val seconds = (value / 1000).toInt()
            rewind.seconds = seconds
            forward.seconds = seconds
        }

    var controlsTimeoutMs: Long = 3_000

    var gesturesEnabled = true

    var chapters: List<Chapter> = emptyList()
        set(value) {
            field = value.sortedBy { it.startMs }
            timeBar.chapterStarts = field.map { it.startMs }
        }

    var resizeMode: Int
        get() = playerView.resizeMode
        set(value) {
            playerView.resizeMode = value
        }

    override var zoomToFill: Boolean
        get() = resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        set(value) {
            resizeMode = if (value) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

    var fullscreenOrientation: Int? = null

    var hiddenControls: Set<Control> = emptySet()
        set(value) {
            field = value
            applyControlVisibility()
        }

    var controlsEnabled = true
        set(value) {
            field = value
            if (!value) {
                sheet.dismiss()
                errorOverlay.hide()
                endedOverlay.hide()
            }
            setControlsVisible(value)
        }

    var controlColor = TEXT_PRIMARY
        set(value) {
            field = value
            applyControlColor()
        }

    var playerBackground = Color.BLACK
        set(value) {
            field = value
            setBackgroundColor(value)
            playerView.setShutterBackgroundColor(value)
        }

    var scrimColor = DEFAULT_SCRIM
        set(value) {
            field = value
            topScrim.background = verticalGradient(value, Color.TRANSPARENT)
            bottomScrim.background = verticalGradient(Color.TRANSPARENT, value)
        }

    var cornerRadius = 0f
        set(value) {
            field = value
            applyCorners()
        }

    var showRemainingTime = false

    var seekBarTrackColor: Int
        get() = timeBar.trackColor
        set(value) {
            timeBar.trackColor = value
        }

    var seekBarBufferedColor: Int
        get() = timeBar.bufferColor
        set(value) {
            timeBar.bufferColor = value
        }

    var keepScreenOnWhilePlaying = true
        set(value) {
            field = value
            keepScreenOn = value && player.isPlaying
        }

    var captionStyle = CaptionStyleCompat(Color.WHITE, 0xB8000000.toInt(), Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, Color.BLACK, null)
        set(value) {
            field = value
            playerView.subtitleView?.setStyle(value)
        }

    var subtitleScale = 1f
        set(value) {
            field = value
            playerView.subtitleView?.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * value)
        }

    var subtitleBottomFraction = SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION
        set(value) {
            field = value
            updateSubtitlePosition()
        }

    val isFullscreen: Boolean get() = fullscreen.isActive

    private val playerView = PlayerView(activity).apply {
        useController = false
        player = this@PlayerUi.player
        setShutterBackgroundColor(Color.BLACK)
        setKeepContentOnPlayerReset(true)
        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
    }

    private val collapse = IconButton(activity, Icon.COLLAPSE, "Exit fullscreen").apply { setOnClickListener { exitFullscreen() } }
    private val title = activity.text(16f, medium = true).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    private val lockButton = IconButton(activity, Icon.LOCK_OPEN, "Lock controls").apply { setOnClickListener { lock() } }
    private val subtitlesButton = IconButton(activity, Icon.SUBTITLES, "Subtitles").apply { setOnClickListener { toggleSubtitles() } }
    private val settingsButton = IconButton(activity, Icon.SETTINGS, "Settings").apply { setOnClickListener { openSettings(SettingsSheet.Page.MAIN) } }
    private val topBar = LinearLayout(activity).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(collapse)
        addView(title, linear(0, WRAP, 1f).margins(start = dp(4)))
        addView(lockButton)
        addView(subtitlesButton)
        addView(settingsButton)
    }

    private val previous = IconButton(activity, Icon.SKIP_PREVIOUS, "Previous", 44, 28).apply { setOnClickListener { skip(player::seekToPreviousMediaItem) } }
    private val next = IconButton(activity, Icon.SKIP_NEXT, "Next", 44, 28).apply { setOnClickListener { skip(player::seekToNextMediaItem) } }
    private val rewind = SeekJumpButton(activity, forward = false).apply { setOnClickListener { seekBy(-seekIncrementMs) } }
    private val playPause = IconButton(activity, Icon.PLAY, "Play").apply {
        background = ovalRipple(rounded(0x59000000, 999f))
        setOnClickListener { togglePlayback() }
    }
    private val forward = SeekJumpButton(activity, forward = true).apply { setOnClickListener { seekBy(seekIncrementMs) } }
    private val center = LinearLayout(activity).apply {
        gravity = Gravity.CENTER
        addView(previous)
        addView(rewind)
        addView(playPause)
        addView(forward)
        addView(next)
    }

    private val time = activity.text(12f).apply {
        setOnClickListener { if (player.isCurrentMediaItemLive) player.seekToDefaultPosition() }
    }
    private val speedChip = SpeedChip(activity).apply { setOnClickListener { openSettings(SettingsSheet.Page.SPEED) } }
    private val fullscreenButton = IconButton(activity, Icon.FULLSCREEN, "Fullscreen").apply {
        setOnClickListener { if (isFullscreen) exitFullscreen() else enterFullscreen() }
    }
    private val timeBar = TimeBar(activity)
    private val bottomBar = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(time, linear(0, WRAP, 1f).margins(start = dp(8)))
            addView(speedChip)
            addView(fullscreenButton)
        })
        addView(timeBar, linear(MATCH, WRAP))
    }

    private val scrubTime = activity.text(12f, medium = true)
    private val scrubChapter = activity.text(11f, TEXT_SECONDARY)
    private val scrubPreview = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = rounded(0xB3000000.toInt(), dp(8).toFloat())
        addView(scrubChapter)
        addView(scrubTime, linear(WRAP, WRAP).margins(top = dp(2)))
        visibility = INVISIBLE
    }

    private val topScrim = View(activity).apply { background = verticalGradient(DEFAULT_SCRIM, Color.TRANSPARENT) }
    private val bottomScrim = View(activity).apply { background = verticalGradient(Color.TRANSPARENT, DEFAULT_SCRIM) }
    private val controls = FrameLayout(activity).apply {
        addView(topScrim, frame(MATCH, MATCH))
        addView(bottomScrim, frame(MATCH, MATCH))
        addView(topBar, frame(MATCH, WRAP, Gravity.TOP))
        addView(center, frame(WRAP, WRAP, Gravity.CENTER))
        addView(bottomBar, frame(MATCH, WRAP, Gravity.BOTTOM))
        addView(scrubPreview, frame(WRAP, WRAP))
    }

    private val rewindIndicator = DoubleTapIndicator(activity, forward = false)
    private val forwardIndicator = DoubleTapIndicator(activity, forward = true)
    private val brightnessLevel = LevelIndicator(activity, Icon.BRIGHTNESS)
    private val volumeLevel = LevelIndicator(activity, Icon.VOLUME)
    private val boostBadge = Badge(activity, Icon.FAST_FORWARD, "2× speed")
    private val spinner = ProgressBar(activity).apply {
        isIndeterminate = true
        visibility = INVISIBLE
    }
    private val errorOverlay = ErrorOverlay(activity, onRetry = { retry() }, onCollapse = { exitFullscreen() })
    private val endedOverlay = EndedOverlay(
        activity,
        onReplay = { replay() },
        onPlayNext = { playNext() },
        onCollapse = { exitFullscreen() },
    )
    private val unlockBadge = Badge(activity, Icon.LOCK, "Tap to unlock").apply {
        isClickable = true
        setOnClickListener { unlock() }
    }

    private val sheet = SettingsSheet(activity.findViewById(android.R.id.content), this).apply {
        onDismiss = {
            if (!isFullscreen) back.disable()
            scheduleHide()
        }
    }
    private val fullscreen = Fullscreen(activity, this)
    private val back = BackInterceptor(activity, this)
    private val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var controlsVisible = false
    private var locked = false
    private var boosting = false
    private var adjusting: LevelIndicator? = null
    private var adjustStart = 0f

    private val hideControls = Runnable { setControlsVisible(false) }
    private val hideUnlock = Runnable { unlockBadge.fadeTo(false) }
    private val progress = object : Runnable {
        override fun run() {
            updateProgress()
            postDelayed(this, 250)
        }
    }

    private val scrubbing = object : TimeBar.Listener {
        private var wasPlaying = false
        private var seekParameters = SeekParameters.DEFAULT

        override fun onScrubStart() {
            removeCallbacks(hideControls)
            wasPlaying = player.playWhenReady
            seekParameters = player.seekParameters
            player.setSeekParameters(SeekParameters.CLOSEST_SYNC)
            scrubPreview.visibility = VISIBLE
        }

        override fun onScrubMove(positionMs: Long) {
            player.seekTo(positionMs)
            scrubTime.text = formatTime(positionMs)
            val chapter = chapters.lastOrNull { it.startMs <= positionMs }
            scrubChapter.text = chapter?.title ?: ""
            scrubChapter.visibility = if (chapter == null) GONE else VISIBLE
            positionPreview()
        }

        override fun onScrubStop(positionMs: Long) {
            player.setSeekParameters(seekParameters)
            player.seekTo(positionMs)
            player.playWhenReady = wasPlaying
            scrubPreview.visibility = INVISIBLE
            scheduleHide()
        }
    }

    private val gestures = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!controlsEnabled) return false
            if (locked) flashUnlock() else setControlsVisible(!controlsVisible)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (locked || !controlsEnabled || !gesturesEnabled) return false
            when {
                e.x < width * 0.4f -> seekBy(-seekIncrementMs)
                e.x > width * 0.6f -> seekBy(seekIncrementMs)
                else -> togglePlayback()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (locked || !controlsEnabled || !gesturesEnabled || !player.isPlaying) return
            boosting = true
            player.setPlaybackSpeed(2f)
            boostBadge.fadeTo(true)
            setControlsVisible(false)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (e1 == null || locked || !controlsEnabled || !gesturesEnabled || !isFullscreen) return false
            val indicator = adjusting ?: run {
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dy) < dp(12) || abs(dy) < abs(dx)) return false
                val left = e1.x < width / 2f
                adjustStart = if (left) brightness() else volume()
                setControlsVisible(false)
                (if (left) brightnessLevel else volumeLevel).also { adjusting = it }
            }
            val level = (adjustStart - (e2.y - e1.y) / (height * 0.8f)).coerceIn(0f, 1f)
            if (indicator === brightnessLevel) setBrightness(level) else setVolume(level)
            indicator.show(level)
            return true
        }
    })

    init {
        setBackgroundColor(Color.BLACK)
        playerView.subtitleView?.setStyle(captionStyle)
        addView(playerView, frame(MATCH, MATCH))
        addView(rewindIndicator, frame(0, MATCH, Gravity.START))
        addView(forwardIndicator, frame(0, MATCH, Gravity.END))
        addView(controls, frame(MATCH, MATCH))
        addView(brightnessLevel, frame(dp(44), dp(168), Gravity.CENTER_VERTICAL or Gravity.START))
        addView(volumeLevel, frame(dp(44), dp(168), Gravity.CENTER_VERTICAL or Gravity.END))
        addView(boostBadge, frame(WRAP, WRAP, Gravity.TOP or Gravity.CENTER_HORIZONTAL).margins(top = dp(16)))
        addView(spinner, frame(dp(52), dp(52), Gravity.CENTER))
        addView(errorOverlay, frame(MATCH, MATCH))
        addView(endedOverlay, frame(MATCH, MATCH))
        addView(unlockBadge, frame(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).margins(bottom = dp(18)))
        timeBar.listener = scrubbing
        player.addListener(this)
        applyLayout(fullscreen = false)
        applyControlVisibility()
        applyAccent()
        syncWithPlayer()
        setControlsVisible(true)
    }

    fun enterFullscreen() {
        if (isFullscreen) return
        sheet.dismiss()
        fullscreen.enter(fullscreenOrientation ?: videoOrientation())
        applyLayout(fullscreen = true)
        back.enable(::handleBack)
        onFullscreenChanged(true)
    }

    private fun videoOrientation(): Int {
        val video = player.videoSize
        return if (video.height > video.width) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        sheet.dismiss()
        fullscreen.exit()
        resetBrightness()
        applyLayout(fullscreen = false)
        back.disable()
        onFullscreenChanged(false)
    }

    fun release() {
        exitFullscreen()
        sheet.dismiss()
        endedOverlay.hide()
        player.removeListener(this)
        playerView.player = null
        removeCallbacks(progress)
        removeCallbacks(hideControls)
        removeCallbacks(hideUnlock)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (boosting) stopBoost()
            adjusting = null
        }
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(progress)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(progress)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val indicatorWidth = (w * 0.4f).roundToInt()
        post {
            listOf(rewindIndicator, forwardIndicator).forEach { indicator ->
                indicator.layoutParams = indicator.layoutParams.apply { width = indicatorWidth }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playPause.icon = if (isPlaying) Icon.PAUSE else Icon.PLAY
        playPause.contentDescription = if (isPlaying) "Pause" else "Play"
        keepScreenOn = keepScreenOnWhilePlaying && isPlaying
        if (isPlaying) {
            endedOverlay.hide()
            errorOverlay.hide()
        }
        if (isPlaying) scheduleHide() else if (!locked && !boosting && !overlayShowing) setControlsVisible(true)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        val buffering = playbackState == Player.STATE_BUFFERING
        spinner.fadeTo(buffering)
        playPause.visibility = if (buffering) INVISIBLE else VISIBLE
        if (playbackState == Player.STATE_ENDED) showEnded()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) showEnded()
    }

    override fun onPlayerError(error: PlaybackException) {
        if (!controlsEnabled) return
        endedOverlay.hide()
        errorOverlay.show(describe(error), error.errorCode)
        setControlsVisible(false)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        errorOverlay.hide()
        endedOverlay.hide()
        updateQueueButtons()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = updateQueueButtons()

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        title.text = mediaMetadata.title ?: ""
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        if (!boosting) speedChip.speed = playbackParameters.speed
    }

    override fun onTracksChanged(tracks: Tracks) {
        subtitlesButton.tint = if (subtitlesEnabled && hasSubtitles) accent else controlColor
        subtitlesButton.alpha = if (hasSubtitles) 1f else 0.5f
    }

    private val overlayShowing: Boolean get() = errorOverlay.isShowing || endedOverlay.isShowing

    private fun positionPreview() {
        val thumb = bottomBar.left + timeBar.left + timeBar.thumbX()
        val half = scrubPreview.width / 2f
        val margin = dp(8).toFloat()
        scrubPreview.x = (thumb - half).coerceIn(margin, width - scrubPreview.width - margin)
        scrubPreview.y = bottomBar.top + timeBar.top - scrubPreview.height.toFloat()
    }

    private fun syncWithPlayer() {
        updateQueueButtons()
        onIsPlayingChanged(player.isPlaying)
        onPlaybackStateChanged(player.playbackState)
        onMediaMetadataChanged(player.mediaMetadata)
        onPlaybackParametersChanged(player.playbackParameters)
        onTracksChanged(player.currentTracks)
    }

    private fun updateProgress() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        timeBar.update(player.currentPosition, player.bufferedPosition, duration)
        time.text = if (player.isCurrentMediaItemLive) liveTime() else SpannableStringBuilder(formatTime(player.currentPosition)).apply {
            val start = length
            append(" / ")
            append(if (showRemainingTime) "-" + formatTime((duration - player.currentPosition).coerceAtLeast(0)) else formatTime(duration))
            setSpan(ForegroundColorSpan(secondaryControlColor), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun liveTime(): CharSequence {
        val window = player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
        val behindMs = window.defaultPositionMs - player.currentPosition
        val atLiveEdge = behindMs < LIVE_EDGE_TOLERANCE_MS
        return SpannableStringBuilder("● LIVE").apply {
            setSpan(ForegroundColorSpan(if (atLiveEdge) LIVE_RED else secondaryControlColor), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (!atLiveEdge) {
                val start = length
                append("  -").append(formatTime(behindMs))
                setSpan(ForegroundColorSpan(secondaryControlColor), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible && controlsEnabled
        controls.fadeTo(controlsVisible)
        updateSubtitlePosition()
        if (visible) scheduleHide() else removeCallbacks(hideControls)
    }

    private fun updateSubtitlePosition() {
        playerView.subtitleView?.setBottomPaddingFraction(if (controlsVisible) maxOf(0.26f, subtitleBottomFraction) else subtitleBottomFraction)
    }

    private fun scheduleHide() {
        removeCallbacks(hideControls)
        if (player.isPlaying && !timeBar.isScrubbing && !sheet.isShowing) postDelayed(hideControls, controlsTimeoutMs)
    }

    private fun togglePlayback() {
        when {
            player.playbackState == Player.STATE_ENDED -> replay()
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    private fun seekBy(deltaMs: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0, duration))
        (if (deltaMs < 0) rewindIndicator else forwardIndicator).trigger((abs(deltaMs) / 1000).toInt())
        if (controlsVisible) scheduleHide()
    }

    private fun skip(seek: () -> Unit) {
        seek()
        player.play()
        scheduleHide()
    }

    private fun updateQueueButtons() {
        val queued = player.mediaItemCount > 1
        listOf(previous to player.hasPreviousMediaItem(), next to player.hasNextMediaItem()).forEach { (button, available) ->
            button.visibility = if (queued && Control.PREVIOUS_NEXT !in hiddenControls) VISIBLE else GONE
            button.isEnabled = available
            button.alpha = if (available) 1f else 0.35f
        }
    }

    private fun stopBoost() {
        boosting = false
        player.setPlaybackSpeed(speedChip.speed)
        boostBadge.fadeTo(false)
    }

    private fun showEnded() {
        if (!controlsEnabled) return
        val next = if (player.hasNextMediaItem()) {
            player.getMediaItemAt(player.nextMediaItemIndex).mediaMetadata.title?.toString() ?: "Next video"
        } else {
            null
        }
        setControlsVisible(false)
        endedOverlay.show(next, if (autoAdvance) upNextSeconds else 0)
    }

    private fun replay() {
        endedOverlay.hide()
        player.seekTo(player.currentMediaItemIndex, 0)
        player.play()
    }

    private fun playNext() {
        endedOverlay.hide()
        player.seekToNextMediaItem()
        player.play()
    }

    private fun retry() {
        errorOverlay.hide()
        player.prepare()
        player.play()
    }

    private fun lock() {
        locked = true
        setControlsVisible(false)
        flashUnlock()
    }

    private fun unlock() {
        locked = false
        removeCallbacks(hideUnlock)
        unlockBadge.fadeTo(false)
        setControlsVisible(true)
    }

    private fun flashUnlock() {
        unlockBadge.fadeTo(true)
        removeCallbacks(hideUnlock)
        postDelayed(hideUnlock, 2500)
    }

    private fun toggleSubtitles() {
        if (!hasSubtitles) {
            openSettings(SettingsSheet.Page.SUBTITLES)
            return
        }
        subtitlesEnabled = !subtitlesEnabled
        scheduleHide()
    }

    private fun openSettings(page: SettingsSheet.Page) {
        removeCallbacks(hideControls)
        if (!isFullscreen) back.enable(::handleBack)
        sheet.show(page, side = isFullscreen)
    }

    private fun handleBack() {
        if (sheet.isShowing) sheet.dismiss() else exitFullscreen()
    }

    private fun brightness(): Float {
        val current = activity.window.attributes.screenBrightness
        if (current >= 0) return current
        return runCatching {
            Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        }.getOrDefault(0.5f)
    }

    private fun setBrightness(level: Float) {
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = level.coerceAtLeast(0.01f) }
    }

    private fun resetBrightness() {
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private fun volume(): Float =
        audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private fun setVolume(level: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0)
    }

    private fun applyLayout(fullscreen: Boolean) {
        collapse.visibility = if (fullscreen) VISIBLE else GONE
        title.visibility = if (fullscreen && Control.TITLE !in hiddenControls) VISIBLE else INVISIBLE
        topBar.setPadding(dp(if (fullscreen) 20 else 4), dp(if (fullscreen) 18 else 6), dp(if (fullscreen) 28 else 6), 0)
        bottomBar.setPadding(dp(if (fullscreen) 24 else 8), 0, dp(if (fullscreen) 24 else 8), dp(if (fullscreen) 12 else 2))
        val gap = dp(if (fullscreen) 18 else 6)
        val playSize = dp(if (fullscreen) 76 else 60)
        val inset = (playSize - dp(if (fullscreen) 40 else 34)) / 2
        playPause.setPadding(inset, inset, inset, inset)
        listOf(previous to dp(44), rewind to dp(52), playPause to playSize, forward to dp(52), next to dp(44)).forEach { (button, size) ->
            button.layoutParams = linear(size, size).margins(start = gap, end = gap)
        }
        fullscreenButton.icon = if (fullscreen) Icon.FULLSCREEN_EXIT else Icon.FULLSCREEN
        fullscreenButton.contentDescription = if (fullscreen) "Exit fullscreen" else "Fullscreen"
        errorOverlay.fullscreen = fullscreen
        endedOverlay.fullscreen = fullscreen
        applyCorners()
        val levelMargin = dp(if (fullscreen) 28 else 14)
        (brightnessLevel.layoutParams as LayoutParams).marginStart = levelMargin
        (volumeLevel.layoutParams as LayoutParams).marginEnd = levelMargin
        requestLayout()
    }

    private fun applyControlVisibility() {
        fun show(view: View, control: Control) {
            view.visibility = if (control in hiddenControls) GONE else VISIBLE
        }
        show(rewind, Control.SEEK_BUTTONS)
        show(forward, Control.SEEK_BUTTONS)
        show(speedChip, Control.SPEED)
        show(subtitlesButton, Control.SUBTITLES)
        show(settingsButton, Control.SETTINGS)
        show(fullscreenButton, Control.FULLSCREEN)
        show(lockButton, Control.LOCK)
        show(timeBar, Control.TIME_BAR)
        updateQueueButtons()
        applyLayout(isFullscreen)
    }

    private val secondaryControlColor: Int get() = withAlpha(controlColor, 0xB3)

    private fun applyControlColor() {
        listOf(collapse, lockButton, settingsButton, previous, next, playPause, fullscreenButton).forEach { it.tint = controlColor }
        rewind.tint = controlColor
        forward.tint = controlColor
        title.setTextColor(controlColor)
        time.setTextColor(controlColor)
        onTracksChanged(player.currentTracks)
    }

    private fun applyCorners() {
        val radius = if (isFullscreen) 0f else cornerRadius
        clipToOutline = radius > 0
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    private fun applyAccent() {
        timeBar.accent = accent
        speedChip.accent = accent
        errorOverlay.accent = accent
        endedOverlay.accent = accent
        sheet.accent = accent
        spinner.indeterminateTintList = ColorStateList.valueOf(accent)
        onTracksChanged(player.currentTracks)
    }

    private fun describe(error: PlaybackException): String =
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Check your connection and try again."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "The video couldn't be found."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "This video format isn't supported."
            else -> "Something went wrong while playing."
        }
}
