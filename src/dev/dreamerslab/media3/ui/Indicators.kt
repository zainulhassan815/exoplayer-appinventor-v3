package dev.dreamerslab.media3.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

internal class DoubleTapIndicator(context: Context, private val forward: Boolean) : FrameLayout(context) {
    private val shape = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF }
    private val oval = RectF()
    private val label = context.text(12f, medium = true)
    private val arrows = List(3) {
        ImageView(context).apply {
            setImageDrawable(IconDrawable(Icon.PLAY))
            scaleX = if (forward) 1f else -1f
            alpha = 0.3f
        }
    }
    private var accumulated = 0
    private val pulse = ValueAnimator.ofFloat(0f, 3f).apply {
        duration = 600
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animator ->
            val phase = animator.animatedValue as Float
            arrows.forEachIndexed { index, arrow -> arrow.alpha = 0.3f + 0.7f * peak(phase - index) }
        }
    }
    private val hide = Runnable {
        animate().alpha(0f).setDuration(200).withEndAction {
            accumulated = 0
            pulse.cancel()
        }
    }

    init {
        setWillNotDraw(false)
        alpha = 0f
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            val ordered = if (forward) arrows else arrows.reversed()
            ordered.forEach { addView(it, linear(dp(16), dp(16))) }
        }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(row)
            addView(label, linear(WRAP, WRAP).margins(top = dp(6)))
        }
        addView(column, frame(WRAP, WRAP, Gravity.CENTER))
    }

    fun trigger(seconds: Int) {
        accumulated += seconds
        label.text = "$accumulated seconds"
        removeCallbacks(hide)
        animate().cancel()
        animate().alpha(1f).setDuration(120)
        if (!pulse.isRunning) pulse.start()
        postDelayed(hide, 700)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(hide)
        pulse.cancel()
        accumulated = 0
        alpha = 0f
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (forward) oval.set(0f, -h * 0.25f, w * 2, h * 1.25f) else oval.set(-w, -h * 0.25f, w, h * 1.25f)
        canvas.drawOval(oval, shape)
    }

    private fun peak(phase: Float): Float {
        val wrapped = ((phase % 3f) + 3f) % 3f
        return if (wrapped < 1f) 1f - abs(2 * wrapped - 1f) else 0f
    }
}

internal class LevelIndicator(context: Context, icon: Icon) : View(context) {
    private val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8C000000.toInt() }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT_PRIMARY }
    private val glyph = IconDrawable(icon)
    private val rect = RectF()
    private var level = 0f
    private val hide = Runnable { fadeTo(false) }

    init {
        alpha = 0f
        visibility = INVISIBLE
    }

    fun show(level: Float) {
        this.level = level.coerceIn(0f, 1f)
        invalidate()
        removeCallbacks(hide)
        if (visibility != VISIBLE || alpha < 1f) fadeTo(true, 120)
        postDelayed(hide, 700)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        rect.set(0f, 0f, w, height.toFloat())
        canvas.drawRoundRect(rect, w / 2, w / 2, background)
        val iconSize = dp(20)
        val iconTop = height - dp(12) - iconSize
        glyph.setBounds(((w - iconSize) / 2).toInt(), iconTop, ((w + iconSize) / 2).toInt(), iconTop + iconSize)
        glyph.draw(canvas)
        val barWidth = dp(6).toFloat()
        val barTop = dp(14).toFloat()
        val barBottom = iconTop - dp(10).toFloat()
        rect.set((w - barWidth) / 2, barTop, (w + barWidth) / 2, barBottom)
        canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, track)
        rect.top = barBottom - (barBottom - barTop) * level
        canvas.drawRoundRect(rect, barWidth / 2, barWidth / 2, fill)
    }
}

internal class Badge(context: Context, icon: Icon, text: String) : TextView(context) {
    init {
        this.text = text
        setTextColor(TEXT_PRIMARY)
        textSize = 13f
        typeface = MEDIUM
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(7), dp(14), dp(7))
        background = pill(0xA6000000.toInt())
        val glyph = IconDrawable(icon).apply { setBounds(0, 0, dp(18), dp(18)) }
        setCompoundDrawablesRelative(glyph, null, null, null)
        compoundDrawablePadding = dp(6)
        alpha = 0f
        visibility = INVISIBLE
    }
}
