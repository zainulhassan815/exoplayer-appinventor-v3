package dev.dreamerslab.media3.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

internal class TimeBar(context: Context) : View(context) {

    interface Listener {
        fun onScrubStart()
        fun onScrubMove(positionMs: Long)
        fun onScrubStop(positionMs: Long)
    }

    var listener: Listener? = null

    var accent: Int = DEFAULT_ACCENT
        set(value) {
            field = value
            progressPaint.color = value
            thumbPaint.color = value
            haloPaint.color = withAlpha(value, 0x40)
            invalidate()
        }

    var trackColor: Int
        get() = trackPaint.color
        set(value) {
            trackPaint.color = value
            invalidate()
        }

    var bufferColor: Int
        get() = bufferPaint.color
        set(value) {
            bufferPaint.color = value
            invalidate()
        }

    var chapterStarts: List<Long> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var isScrubbing = false
        private set

    private var duration = 0L
    private var position = 0L
    private var buffered = 0L

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF }
    private val bufferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x73FFFFFF }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint().apply { color = 0x99000000.toInt() }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bar = RectF()

    init {
        accent = DEFAULT_ACCENT
    }

    fun update(positionMs: Long, bufferedMs: Long, durationMs: Long) {
        duration = durationMs.coerceAtLeast(0)
        buffered = bufferedMs
        if (!isScrubbing) position = positionMs
        invalidate()
    }

    fun thumbX(): Float = xOf(position)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(28))
    }

    override fun onDraw(canvas: Canvas) {
        val thickness = dp(if (isScrubbing) 5 else 3).toFloat()
        val center = height / 2f
        val radius = thickness / 2
        fun drawSpan(endX: Float, paint: Paint) {
            bar.set(start, center - radius, endX.coerceAtLeast(start), center + radius)
            canvas.drawRoundRect(bar, radius, radius, paint)
        }
        drawSpan(end, trackPaint)
        drawSpan(xOf(buffered), bufferPaint)
        drawSpan(xOf(position), progressPaint)
        val markerWidth = dp(2).toFloat()
        chapterStarts.filter { it in 1 until duration }.forEach {
            val x = xOf(it)
            canvas.drawRect(x - markerWidth / 2, center - radius, x + markerWidth / 2, center + radius, markerPaint)
        }
        val thumbX = xOf(position)
        if (isScrubbing) canvas.drawCircle(thumbX, center, dp(14).toFloat(), haloPaint)
        canvas.drawCircle(thumbX, center, dp(if (isScrubbing) 8 else 6).toFloat(), thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (duration <= 0) return false
                parent.requestDisallowInterceptTouchEvent(true)
                isScrubbing = true
                position = positionAt(event.x)
                listener?.onScrubStart()
                listener?.onScrubMove(position)
            }
            MotionEvent.ACTION_MOVE -> if (isScrubbing) {
                position = positionAt(event.x)
                listener?.onScrubMove(position)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isScrubbing) {
                isScrubbing = false
                listener?.onScrubStop(position)
            }
        }
        invalidate()
        return true
    }

    private val start get() = paddingStart + dp(8).toFloat()

    private val end get() = width - paddingEnd - dp(8).toFloat()

    private fun xOf(ms: Long): Float =
        if (duration <= 0) start else start + (end - start) * (ms.toFloat() / duration).coerceIn(0f, 1f)

    private fun positionAt(x: Float): Long =
        (((x - start) / (end - start)).coerceIn(0f, 1f) * duration).toLong()
}
