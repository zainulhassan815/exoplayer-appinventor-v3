package dev.dreamerslab.media3.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal class Fullscreen(private val activity: Activity, private val view: View) {
    private val content: ViewGroup = activity.findViewById(android.R.id.content)
    private var parent: ViewGroup? = null
    private var index = 0
    private var params: ViewGroup.LayoutParams? = null
    private var placeholder: View? = null
    private var orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    var isActive = false
        private set

    fun enter(portraitVideo: Boolean) {
        if (isActive) return
        val currentParent = view.parent as? ViewGroup ?: return
        parent = currentParent
        index = currentParent.indexOfChild(view)
        params = view.layoutParams
        placeholder = View(activity).apply { setBackgroundColor(0xFF000000.toInt()) }
        currentParent.removeView(view)
        currentParent.addView(placeholder, index, ViewGroup.LayoutParams(params?.width ?: MATCH, view.height.takeIf { it > 0 } ?: WRAP))
        content.addView(view, FrameLayout.LayoutParams(MATCH, MATCH))
        orientation = activity.requestedOrientation
        activity.requestedOrientation = if (portraitVideo) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        systemBars().apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        isActive = true
    }

    fun exit() {
        if (!isActive) return
        content.removeView(view)
        parent?.let { original ->
            original.removeView(placeholder)
            original.addView(view, index, params)
        }
        placeholder = null
        parent = null
        activity.requestedOrientation = orientation
        systemBars().show(WindowInsetsCompat.Type.systemBars())
        isActive = false
    }

    private fun systemBars() = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
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
