package dev.dreamerslab.media3

import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.HVArrangement
import com.google.appinventor.components.runtime.OnDestroyListener

@Extension(
    description = "Video and audio player built on AndroidX Media3.",
    icon = "icon.png"
)
class Media3Player(
    private val container: ComponentContainer
) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener, Player.Listener {

    private val context = container.`$context`()
    private var player: ExoPlayer? = null

    init {
        form.registerForOnDestroy(this)
    }

    @SimpleFunction(description = "Shows the player inside the given arrangement.")
    fun Create(arrangement: HVArrangement) {
        val exoPlayer = player ?: ExoPlayer.Builder(context).build().also {
            it.addListener(this)
            player = it
        }
        val playerView = PlayerView(context).apply { player = exoPlayer }
        (arrangement.view as ViewGroup).addView(
            playerView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    @SimpleFunction(description = "Plays the media at the given url.")
    fun Play(url: String) {
        player?.run {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    @SimpleEvent(description = "The playback state changed to IDLE, BUFFERING, READY or ENDED.")
    fun PlaybackStateChanged(state: String) {
        EventDispatcher.dispatchEvent(this, "PlaybackStateChanged", state)
    }

    @SimpleEvent(description = "Playback failed.")
    fun PlayerError(message: String) {
        EventDispatcher.dispatchEvent(this, "PlayerError", message)
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

    override fun onPlayerError(error: PlaybackException) {
        PlayerError(error.message ?: error.errorCodeName)
    }

    override fun onDestroy() {
        player?.release()
        player = null
    }
}
