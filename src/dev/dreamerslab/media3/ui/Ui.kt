package dev.dreamerslab.media3.ui

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

internal const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
internal const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

internal const val TEXT_PRIMARY = Color.WHITE
internal const val TEXT_SECONDARY = 0xB3FFFFFF.toInt()
internal const val DEFAULT_SCRIM = 0xB3000000.toInt()
internal const val DEFAULT_SEEK_BAR_TRACK = 0x40FFFFFF
internal const val DEFAULT_SEEK_BAR_BUFFERED = 0x73FFFFFF
internal const val DEFAULT_SUBTITLE_BACKGROUND = 0xB8000000.toInt()
internal const val SCRIM_DARK = 0xC708080C.toInt()
internal const val SURFACE = 0xFF1B1B24.toInt()
internal const val ERROR = 0xFFFF6B6B.toInt()
internal const val LIVE_RED = 0xFFFF3B30.toInt()
internal const val DEFAULT_ACCENT = 0xFF7C5CFF.toInt()

internal val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

internal fun Context.dp(value: Number): Int = (value.toFloat() * resources.displayMetrics.density).roundToInt()

internal fun View.dp(value: Number): Int = context.dp(value)

internal fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

internal fun View.pill(color: Int): GradientDrawable = rounded(color, dp(999).toFloat())

internal fun rounded(color: Int, radius: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

internal fun rounded(color: Int, topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
    }

internal fun verticalGradient(top: Int, bottom: Int): GradientDrawable =
    GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom))

internal fun ripple(mask: Drawable, content: Drawable? = null): RippleDrawable =
    RippleDrawable(RIPPLE_COLORS, content, mask)

private val RIPPLE_COLORS = ColorStateList(
    arrayOf(intArrayOf(R.attr.state_focused), intArrayOf()),
    intArrayOf(0x73FFFFFF, 0x33FFFFFF),
)

internal fun ovalRipple(content: Drawable? = null): RippleDrawable = ripple(ShapeDrawable(OvalShape()), content)

internal fun Context.text(sizeSp: Float, color: Int = TEXT_PRIMARY, medium: Boolean = false): TextView =
    TextView(this).apply {
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        includeFontPadding = false
        if (medium) typeface = MEDIUM
    }

internal fun frame(width: Int, height: Int, gravity: Int = 0): FrameLayout.LayoutParams =
    FrameLayout.LayoutParams(width, height, gravity)

internal fun linear(width: Int, height: Int, weight: Float = 0f): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(width, height, weight)

internal fun <T : ViewGroup.MarginLayoutParams> T.margins(start: Int = 0, top: Int = 0, end: Int = 0, bottom: Int = 0): T =
    apply {
        marginStart = start
        topMargin = top
        marginEnd = end
        bottomMargin = bottom
    }

internal fun Window.hideSystemBars() {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

internal fun Window.showSystemBars() {
    WindowCompat.getInsetsController(this, decorView).show(WindowInsetsCompat.Type.systemBars())
}

internal fun View.fadeTo(visible: Boolean, duration: Long = 200) {
    animate().cancel()
    if (visible) {
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(duration)
    } else {
        animate().alpha(0f).setDuration(duration).withEndAction { visibility = View.INVISIBLE }
    }
}

internal fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val rest = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
}
