package dev.dreamerslab.media3.ui

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

internal abstract class StateOverlay(context: Context, onCollapse: () -> Unit) : FrameLayout(context) {
    protected val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    private val collapse = IconButton(context, Icon.COLLAPSE, "Exit fullscreen").apply { setOnClickListener { onCollapse() } }

    val isShowing: Boolean get() = visibility == VISIBLE

    var fullscreen: Boolean = false
        set(value) {
            field = value
            collapse.visibility = if (value) VISIBLE else GONE
            onSizeClass(value)
        }

    init {
        setBackgroundColor(SCRIM_DARK)
        isClickable = true
        visibility = GONE
        addView(content, frame(WRAP, WRAP, Gravity.CENTER).margins(start = dp(24), end = dp(24)))
        addView(collapse, frame(dp(40), dp(40), Gravity.TOP or Gravity.START).margins(start = dp(20), top = dp(18)))
        collapse.visibility = GONE
    }

    fun hide() {
        onHide()
        visibility = GONE
    }

    protected abstract fun onSizeClass(fullscreen: Boolean)

    protected open fun onHide() = Unit

    protected fun TextView.sized(compactSp: Float, fullscreenSp: Float) =
        setTextSize(TypedValue.COMPLEX_UNIT_SP, if (fullscreen) fullscreenSp else compactSp)
}

internal class ErrorOverlay(context: Context, onRetry: () -> Unit, onCollapse: () -> Unit) : StateOverlay(context, onCollapse) {
    private val icon = ImageView(context).apply { setImageDrawable(IconDrawable(Icon.ERROR, ERROR)) }
    private val title = context.text(16f, medium = true).apply { text = "Can't play this video" }
    private val message = context.text(13f, TEXT_SECONDARY).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    private val code = context.text(11f, 0x80FFFFFF.toInt())
    private val retry = PillButton(context, "Retry", Icon.REPLAY).apply { setOnClickListener { onRetry() } }

    var accent: Int by retry::accent

    init {
        setBackgroundColor(0xE60C0C10.toInt())
        content.addView(icon)
        content.addView(title, linear(WRAP, WRAP).margins(top = dp(10)))
        content.addView(message, linear(WRAP, WRAP).margins(top = dp(4)))
        content.addView(code, linear(WRAP, WRAP).margins(top = dp(4)))
        content.addView(retry, linear(WRAP, WRAP).margins(top = dp(14)))
        onSizeClass(false)
    }

    fun show(reason: String, errorCode: Int) {
        message.text = reason
        code.text = "Error $errorCode"
        visibility = VISIBLE
    }

    override fun onSizeClass(fullscreen: Boolean) {
        val size = dp(if (fullscreen) 44 else 32)
        icon.layoutParams = linear(size, size)
        title.sized(15f, 18f)
        message.sized(12f, 14f)
        message.maxWidth = dp(if (fullscreen) 420 else 300)
    }
}

internal class EndedOverlay(
    context: Context,
    private val onReplay: () -> Unit,
    private val onPlayNext: () -> Unit,
    onCollapse: () -> Unit,
) : StateOverlay(context, onCollapse) {

    private val caption = context.text(11f, TEXT_SECONDARY, medium = true).apply { letterSpacing = 0.08f }
    private val nextTitle = context.text(16f, medium = true).apply {
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    private val countdown = CountdownRing(context).apply { setOnClickListener { playNext() } }
    private val cancel = PillButton(context, "Cancel", filled = false).apply { setOnClickListener { cancelCountdown() } }
    private val upNext = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(caption, linear(WRAP, WRAP))
        addView(nextTitle, linear(WRAP, WRAP).margins(top = dp(4)))
        addView(countdown)
        addView(cancel, linear(WRAP, WRAP).margins(top = dp(10)))
    }

    private val replay = IconButton(context, Icon.REPLAY, "Replay").apply {
        background = ovalRipple(rounded(0x26FFFFFF, 999f))
        setOnClickListener { replay() }
    }
    private val replayLabel = context.text(12f, TEXT_SECONDARY, medium = true).apply { text = "Replay" }
    private val playNextButton = PillButton(context, "Play next", Icon.PLAY).apply { setOnClickListener { playNext() } }
    private val finished = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(replay)
        addView(replayLabel, linear(WRAP, WRAP).margins(top = dp(8)))
        addView(playNextButton, linear(WRAP, WRAP).margins(top = dp(14)))
    }

    var accent: Int by playNextButton::accent

    private var next: String? = null

    init {
        content.addView(upNext)
        content.addView(finished)
        onSizeClass(false)
    }

    fun show(next: String?, countdownSeconds: Int) {
        this.next = next
        visibility = VISIBLE
        if (next == null || countdownSeconds <= 0) {
            showFinished()
            return
        }
        nextTitle.text = next
        upNext.visibility = VISIBLE
        finished.visibility = GONE
        countdown.start(countdownSeconds, onTick = { caption.text = "UP NEXT IN $it" }, onFinished = ::playNext)
    }

    override fun onSizeClass(fullscreen: Boolean) {
        nextTitle.sized(14f, 18f)
        nextTitle.maxWidth = dp(if (fullscreen) 420 else 280)
        val ring = dp(if (fullscreen) 68 else 52)
        countdown.layoutParams = linear(ring, ring).margins(top = dp(if (fullscreen) 18 else 10))
        val button = dp(if (fullscreen) 72 else 56)
        val inset = (button - dp(if (fullscreen) 36 else 30)) / 2
        replay.layoutParams = linear(button, button)
        replay.setPadding(inset, inset, inset, inset)
    }

    override fun onHide() = countdown.cancel()

    private fun cancelCountdown() {
        countdown.cancel()
        showFinished()
    }

    private fun showFinished() {
        upNext.visibility = GONE
        finished.visibility = VISIBLE
        playNextButton.visibility = if (next != null) VISIBLE else GONE
    }

    private fun replay() {
        countdown.cancel()
        onReplay()
    }

    private fun playNext() {
        countdown.cancel()
        onPlayNext()
    }
}
