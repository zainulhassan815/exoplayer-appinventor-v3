package dev.dreamerslab.media3.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.ceil

internal class IconButton(
    context: Context,
    icon: Icon,
    description: String,
    sizeDp: Int = 40,
    iconDp: Int = 24,
) : ImageView(context) {
    private val glyph = IconDrawable(icon)

    var icon: Icon by glyph::icon

    var tint: Int by glyph::color

    init {
        setImageDrawable(glyph)
        contentDescription = description
        val inset = dp((sizeDp - iconDp) / 2)
        setPadding(inset, inset, inset, inset)
        background = ovalRipple()
        isClickable = true
        isFocusable = true
        layoutParams = frame(dp(sizeDp), dp(sizeDp))
    }
}

internal class SeekJumpButton(context: Context, private val forward: Boolean) : FrameLayout(context) {
    private val label = context.text(9f, medium = true).apply { gravity = Gravity.CENTER }
    private val glyph = IconDrawable(Icon.REPLAY)

    var tint: Int
        get() = glyph.color
        set(value) {
            glyph.color = value
            label.setTextColor(value)
        }

    var seconds: Int = 10
        set(value) {
            field = value
            label.text = value.toString()
            contentDescription = "${if (forward) "Forward" else "Rewind"} $value seconds"
        }

    init {
        background = ovalRipple()
        isClickable = true
        isFocusable = true
        val size = dp(52)
        layoutParams = frame(size, size)
        addView(ImageView(context).apply {
            setImageDrawable(glyph)
            scaleX = if (forward) -1f else 1f
        }, frame(dp(34), dp(34), Gravity.CENTER))
        addView(label, frame(MATCH, MATCH).margins(top = dp(3)))
        seconds = 10
    }
}

internal class PillButton(context: Context, text: String, icon: Icon? = null, private val filled: Boolean = true) :
    TextView(context) {

    var accent: Int = DEFAULT_ACCENT
        set(value) {
            field = value
            refresh()
        }

    init {
        this.text = text
        setTextColor(TEXT_PRIMARY)
        textSize = 13f
        typeface = MEDIUM
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(9), dp(18), dp(9))
        isClickable = true
        isFocusable = true
        if (icon != null) {
            val glyph = IconDrawable(icon).apply { setBounds(0, 0, dp(18), dp(18)) }
            setCompoundDrawablesRelative(glyph, null, null, null)
            compoundDrawablePadding = dp(6)
        }
        refresh()
    }

    private fun refresh() {
        val color = if (filled) accent else 0x1FFFFFFF
        val shape = rounded(color, dp(999).toFloat())
        background = ripple(rounded(color, dp(999).toFloat()), shape)
    }
}

internal class SpeedChip(context: Context) : TextView(context) {
    var accent: Int = DEFAULT_ACCENT
        set(value) {
            field = value
            refresh()
        }

    var speed: Float = 1f
        set(value) {
            field = value
            text = speedLabel(value)
            refresh()
        }

    init {
        setTextColor(TEXT_PRIMARY)
        textSize = 12f
        typeface = MEDIUM
        setPadding(dp(10), dp(4), dp(10), dp(4))
        isClickable = true
        isFocusable = true
        contentDescription = "Playback speed"
        speed = 1f
    }

    private fun refresh() {
        val color = if (speed == 1f) 0x24FFFFFF else accent
        background = ripple(rounded(color, dp(999).toFloat()), rounded(color, dp(999).toFloat()))
    }
}

internal fun speedLabel(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "×"

internal class ToggleSwitch(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT_PRIMARY }
    private val rect = RectF()
    private var position = 0f

    var accent: Int = DEFAULT_ACCENT
        set(value) {
            field = value
            invalidate()
        }

    var checked: Boolean = false
        set(value) {
            field = value
            ValueAnimator.ofFloat(position, if (value) 1f else 0f).apply {
                duration = 180
                addUpdateListener {
                    position = it.animatedValue as Float
                    invalidate()
                }
            }.start()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(dp(36), dp(20))
    }

    override fun onDraw(canvas: Canvas) {
        val radius = height / 2f
        track.color = blend(0x40FFFFFF, accent, position)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, track)
        val inset = dp(2).toFloat()
        val thumbRadius = radius - inset
        val x = inset + thumbRadius + (width - 2 * (inset + thumbRadius)) * position
        canvas.drawCircle(x, radius, thumbRadius, thumb)
    }

    private fun blend(from: Int, to: Int, fraction: Float): Int {
        fun channel(shift: Int) = ((from shr shift and 0xFF) + ((to shr shift and 0xFF) - (from shr shift and 0xFF)) * fraction).toInt()
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

internal class CountdownRing(context: Context) : View(context) {
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59000000 }
    private val glyph = IconDrawable(Icon.PLAY)
    private val oval = RectF()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    init {
        background = ovalRipple()
        isClickable = true
        isFocusable = true
        contentDescription = "Play next"
    }

    fun start(seconds: Int, onTick: (Int) -> Unit, onFinished: () -> Unit) {
        cancel()
        var announced = seconds
        onTick(seconds)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = seconds * 1000L
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                val left = ceil(seconds * (1 - progress)).toInt()
                if (left != announced && left > 0) {
                    announced = left
                    onTick(left)
                }
                invalidate()
            }
            doOnEnd(onFinished)
            start()
        }
    }

    fun cancel() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        progress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = width * 0.06f
        ring.strokeWidth = stroke
        oval.set(stroke, stroke, width - stroke, height - stroke)
        canvas.drawOval(oval, fill)
        ring.color = 0x4DFFFFFF
        canvas.drawArc(oval, 0f, 360f, false, ring)
        ring.color = TEXT_PRIMARY
        canvas.drawArc(oval, -90f, 360f * progress, false, ring)
        val icon = (width * 0.42f).toInt()
        val left = (width - icon) / 2
        val top = (height - icon) / 2
        glyph.setBounds(left, top, left + icon, top + icon)
        glyph.draw(canvas)
    }
}

private fun ValueAnimator.doOnEnd(action: () -> Unit) {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) = action()
    })
}
