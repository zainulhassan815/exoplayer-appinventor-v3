package dev.dreamerslab.media3.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal class Detached(private val view: View) {
    private var parent: ViewGroup? = null
    private var index = 0
    private var params: ViewGroup.LayoutParams? = null
    private var placeholder: View? = null

    val isDetached: Boolean get() = parent != null

    fun into(target: ViewGroup, targetParams: ViewGroup.LayoutParams) {
        if (isDetached) return
        val currentParent = view.parent as? ViewGroup ?: return
        parent = currentParent
        index = currentParent.indexOfChild(view)
        params = view.layoutParams
        placeholder = View(view.context).apply { setBackgroundColor(0xFF000000.toInt()) }
        currentParent.removeView(view)
        currentParent.addView(placeholder, index, ViewGroup.LayoutParams(params?.width ?: MATCH, view.height.takeIf { it > 0 } ?: WRAP))
        target.addView(view, targetParams)
    }

    fun restore() {
        if (!isDetached) return
        (view.parent as? ViewGroup)?.removeView(view)
        parent?.let { original ->
            original.removeView(placeholder)
            original.addView(view, index, params)
        }
        placeholder = null
        parent = null
    }
}

internal class Fullscreen(private val activity: Activity, view: View) {
    private val content: ViewGroup = activity.findViewById(android.R.id.content)
    private val detached = Detached(view)
    private var orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    var isActive = false
        private set

    fun enter(requestedOrientation: Int) {
        if (isActive) return
        detached.into(content, FrameLayout.LayoutParams(MATCH, MATCH))
        orientation = activity.requestedOrientation
        activity.requestedOrientation = requestedOrientation
        systemBars().apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        isActive = true
    }

    fun exit() {
        if (!isActive) return
        detached.restore()
        activity.requestedOrientation = orientation
        systemBars().show(WindowInsetsCompat.Type.systemBars())
        isActive = false
    }

    private fun systemBars() = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
}

internal class PlayerDialog(activity: Activity, view: View, private val onBackdropTap: () -> Unit) {
    private val content: ViewGroup = activity.findViewById(android.R.id.content)
    private val detached = Detached(view)
    private val backdrop = FrameLayout(activity).apply {
        setBackgroundColor(0x99000000.toInt())
        isClickable = true
        setOnClickListener { onBackdropTap() }
    }

    val isShowing: Boolean get() = detached.isDetached

    fun show() {
        if (isShowing) return
        content.addView(backdrop, FrameLayout.LayoutParams(MATCH, MATCH))
        val margin = backdrop.dp(16)
        val width = (content.width.takeIf { it > 0 } ?: content.resources.displayMetrics.widthPixels) - 2 * margin
        detached.into(backdrop, FrameLayout.LayoutParams(width, width * 9 / 16, Gravity.CENTER))
    }

    fun dismiss() {
        if (!isShowing) return
        detached.restore()
        content.removeView(backdrop)
    }
}

internal class BackInterceptor(private val activity: Activity, private val view: View) {
    private var enabled = false
    private var platformCallback: Any? = null

    fun enable(onBack: () -> Unit) {
        if (enabled) return
        enabled = true
        if (Build.VERSION.SDK_INT >= 33) {
            platformCallback = OnBackInvokedCallback { onBack() }.also {
                activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, it)
            }
        }
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_BACK) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) onBack()
            true
        }
    }

    fun disable() {
        if (!enabled) return
        enabled = false
        if (Build.VERSION.SDK_INT >= 33) {
            (platformCallback as? OnBackInvokedCallback)?.let { activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        platformCallback = null
        view.setOnKeyListener(null)
        view.isFocusableInTouchMode = false
    }
}
