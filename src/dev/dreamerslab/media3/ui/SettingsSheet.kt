package dev.dreamerslab.media3.ui

import android.R
import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

internal class Choice(val label: String, val selected: Boolean, val select: () -> Unit)

internal interface TrackChoices {
    val qualities: List<Choice>
    val subtitles: List<Choice>
    val audioTracks: List<Choice>
    val hasSubtitles: Boolean
    var subtitlesEnabled: Boolean
    var speed: Float
    var loop: Boolean
    val autoAdvance: Boolean
    val upNextSeconds: Int
}

internal interface PlaybackSettings : TrackChoices {
    var zoomToFill: Boolean
}

internal val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

internal class SettingsSheet(activity: Activity, private val settings: PlaybackSettings) {

    enum class Page { MAIN, QUALITY, SPEED, SUBTITLES, AUDIO }

    var accent: Int = DEFAULT_ACCENT

    var onDismiss: () -> Unit = {}

    private val context = activity
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val panel = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
    }
    private val scrim = FrameLayout(context).apply {
        setBackgroundColor(0x66000000)
        setOnClickListener { dismiss() }
        isFocusable = false
        addView(panel)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            panel.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }
    }
    private val dialog = Dialog(activity, R.style.Theme_Translucent_NoTitleBar).apply {
        setContentView(scrim)
        setOnDismissListener { onDismiss() }
    }
    private var side = false

    val isShowing: Boolean get() = dialog.isShowing

    fun show(page: Page, side: Boolean) {
        this.side = side
        render(page)
        if (isShowing) return
        panel.layoutParams = if (side) {
            frame(context.dp(320), MATCH, Gravity.END)
        } else {
            frame(MATCH, WRAP, Gravity.BOTTOM)
        }
        val radius = context.dp(18).toFloat()
        panel.background = if (side) rounded(SURFACE, radius, 0f, 0f, radius) else rounded(SURFACE, radius, radius, 0f, 0f)
        panel.setOnClickListener {}
        panel.isFocusable = false
        dialog.show()
        if (side) dialog.window?.hideSystemBars()
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(200)
        panel.post {
            if (side) panel.translationX = panel.width.toFloat() else panel.translationY = panel.height.toFloat()
            panel.animate().translationX(0f).translationY(0f).setDuration(250)
        }
    }

    fun dismiss(animate: Boolean = true) {
        if (!isShowing) return
        if (!animate) {
            dialog.dismiss()
            return
        }
        val exit = panel.animate().setDuration(200)
        if (side) exit.translationX(panel.width.toFloat()) else exit.translationY(panel.height.toFloat())
        scrim.animate().alpha(0f).setDuration(200).withEndAction { dialog.dismiss() }
    }

    private fun render(page: Page) {
        content.removeAllViews()
        content.setPadding(0, context.dp(if (side) 14 else 8), 0, context.dp(20))
        if (!side) content.addView(grabber())
        when (page) {
            Page.MAIN -> renderMain()
            Page.QUALITY -> renderChoices("Quality", settings.qualities)
            Page.SUBTITLES -> renderChoices("Subtitles", settings.subtitles)
            Page.AUDIO -> renderChoices("Audio track", settings.audioTracks)
            Page.SPEED -> renderSpeeds()
        }
        if (isShowing && !content.isInTouchMode) content.requestFocus()
    }

    private fun renderMain() {
        fun selectedLabel(choices: List<Choice>, fallback: String) = choices.firstOrNull { it.selected }?.label ?: fallback
        content.addView(navigationRow(Icon.QUALITY, "Quality", selectedLabel(settings.qualities, "Auto"), Page.QUALITY))
        content.addView(navigationRow(Icon.SPEED, "Playback speed", if (settings.speed == 1f) "Normal" else speedLabel(settings.speed), Page.SPEED))
        content.addView(navigationRow(Icon.SUBTITLES, "Subtitles", selectedLabel(settings.subtitles, "Off"), Page.SUBTITLES))
        content.addView(navigationRow(Icon.AUDIO, "Audio track", selectedLabel(settings.audioTracks, "Default"), Page.AUDIO))
        content.addView(toggleRow(Icon.ZOOM, "Zoom to fill", settings.zoomToFill) { settings.zoomToFill = it })
        content.addView(toggleRow(Icon.LOOP, "Loop", settings.loop) { settings.loop = it })
    }

    private fun renderChoices(title: String, choices: List<Choice>) {
        content.addView(header(title))
        if (choices.isEmpty()) {
            content.addView(row().apply {
                addView(context.text(14f, TEXT_SECONDARY).apply { text = "Not available for this media" })
            })
        }
        choices.forEach { choice ->
            content.addView(row().apply {
                val color = if (choice.selected) accent else TEXT_PRIMARY
                addView(icon(Icon.CHECK, accent).apply { visibility = if (choice.selected) View.VISIBLE else View.INVISIBLE })
                addView(label(choice.label, color), linear(0, WRAP, 1f))
                setOnClickListener {
                    choice.select()
                    dismiss()
                }
            })
        }
    }

    private fun renderSpeeds() {
        content.addView(header("Playback speed"))
        SPEEDS.chunked(4).forEach { speeds ->
            content.addView(LinearLayout(context).apply {
                setPadding(context.dp(18), context.dp(4), context.dp(10), context.dp(4))
                speeds.forEach { speed ->
                    val selected = speed == settings.speed
                    addView(TextView(context).apply {
                        text = speedLabel(speed)
                        setTextColor(TEXT_PRIMARY)
                        textSize = 13f
                        typeface = MEDIUM
                        gravity = Gravity.CENTER
                        setPadding(0, context.dp(10), 0, context.dp(10))
                        val radius = context.dp(10).toFloat()
                        val shape = if (selected) rounded(accent, radius) else outline(radius)
                        background = ripple(rounded(TEXT_PRIMARY, radius), shape)
                        setOnClickListener {
                            settings.speed = speed
                            dismiss()
                        }
                    }, linear(0, WRAP, 1f).margins(end = context.dp(8)))
                }
            })
        }
    }

    private fun navigationRow(glyph: Icon, title: String, value: String, page: Page) = row().apply {
        addView(icon(glyph, TEXT_SECONDARY))
        addView(label(title, TEXT_PRIMARY), linear(0, WRAP, 1f))
        addView(context.text(13f, TEXT_SECONDARY).apply { text = value })
        addView(icon(Icon.CHEVRON_RIGHT, TEXT_SECONDARY))
        setOnClickListener { render(page) }
    }

    private fun toggleRow(glyph: Icon, title: String, checked: Boolean, onChange: (Boolean) -> Unit) = row().apply {
        val toggle = ToggleSwitch(context).apply {
            accent = this@SettingsSheet.accent
            this.checked = checked
        }
        addView(icon(glyph, TEXT_SECONDARY))
        addView(label(title, TEXT_PRIMARY), linear(0, WRAP, 1f))
        addView(toggle)
        setOnClickListener {
            toggle.checked = !toggle.checked
            onChange(toggle.checked)
        }
    }

    private fun header(title: String) = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(8), 0, context.dp(16), context.dp(4))
        addView(IconButton(context, Icon.BACK, "Back").apply { setOnClickListener { render(Page.MAIN) } })
        addView(context.text(15f, medium = true).apply { text = title }, linear(WRAP, WRAP).margins(start = context.dp(4)))
    }

    private fun row() = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(48)
        setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
        background = ripple(rounded(TEXT_PRIMARY, 0f))
        isClickable = true
    }

    private fun icon(glyph: Icon, color: Int) = ImageView(context).apply {
        setImageDrawable(IconDrawable(glyph, color))
        layoutParams = linear(context.dp(20), context.dp(20))
    }

    private fun label(text: String, color: Int) = context.text(14f, color, medium = true).apply {
        this.text = text
        setPadding(context.dp(14), 0, context.dp(8), 0)
    }

    private fun grabber() = View(context).apply {
        background = rounded(0x40FFFFFF, context.dp(2).toFloat())
        layoutParams = linear(context.dp(36), context.dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = context.dp(10)
        }
    }

    private fun outline(radius: Float) = rounded(0, radius).apply { setStroke(context.dp(1), 0x2EFFFFFF) }
}
