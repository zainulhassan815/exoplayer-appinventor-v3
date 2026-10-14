package dev.dreamerslab.media3.ui

import android.R
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

internal class Detached(private val view: View) {
    private var parent: ViewGroup? = null
    private var index = 0
    private var params: ViewGroup.LayoutParams? = null
    private var placeholder: View? = null

    val isDetached: Boolean get() = parent != null

    fun into(target: ViewGroup, targetParams: ViewGroup.LayoutParams) {
        if (isDetached) return
        val currentParent = view.parent as? ViewGroup ?: return
        keepingFocus {
            parent = currentParent
            index = currentParent.indexOfChild(view)
            params = view.layoutParams
            placeholder = View(view.context).apply { setBackgroundColor(0xFF000000.toInt()) }
            currentParent.removeView(view)
            currentParent.addView(placeholder, index, ViewGroup.LayoutParams(params?.width ?: MATCH, view.height.takeIf { it > 0 } ?: WRAP))
            target.addView(view, targetParams)
        }
    }

    fun restore() {
        if (!isDetached) return
        keepingFocus {
            (view.parent as? ViewGroup)?.removeView(view)
            parent?.let { original ->
                original.removeView(placeholder)
                original.addView(view, index, params)
            }
            placeholder = null
            parent = null
        }
    }

    private fun keepingFocus(move: () -> Unit) {
        val focused = view.findFocus()
        move()
        focused?.requestFocus()
    }
}

internal class Fullscreen(private val activity: Activity, view: View) {
    private val content: ViewGroup = activity.findViewById(R.id.content)
    private val detached = Detached(view)
    private var orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    var isActive = false
        private set

    fun enter(requestedOrientation: Int) {
        if (isActive) return
        detached.into(content, FrameLayout.LayoutParams(MATCH, MATCH))
        orientation = activity.requestedOrientation
        activity.requestedOrientation = requestedOrientation
        activity.window.hideSystemBars()
        isActive = true
    }

    fun exit() {
        if (!isActive) return
        detached.restore()
        activity.requestedOrientation = orientation
        activity.window.showSystemBars()
        isActive = false
    }
}

internal class PlayerDialog(activity: Activity, view: View, private val onBackdropTap: () -> Unit) {
    private val content: ViewGroup = activity.findViewById(R.id.content)
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
    private var onBack: (() -> Unit)? = null
    private var platformCallback: Any? = null

    fun enable(onBack: () -> Unit) {
        if (this.onBack != null) return
        this.onBack = onBack
        if (Build.VERSION.SDK_INT >= 33) {
            platformCallback = OnBackInvokedCallback { onBack() }.also {
                activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, it)
            }
        }
        view.isFocusableInTouchMode = true
        if (view.isInTouchMode) view.requestFocus()
    }

    fun intercepts(event: KeyEvent): Boolean {
        val onBack = onBack ?: return false
        if (event.keyCode != KeyEvent.KEYCODE_BACK) return false
        if (event.action == KeyEvent.ACTION_UP) onBack()
        return true
    }

    fun disable() {
        if (onBack == null) return
        onBack = null
        if (Build.VERSION.SDK_INT >= 33) {
            (platformCallback as? OnBackInvokedCallback)?.let { activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        platformCallback = null
        view.isFocusableInTouchMode = false
    }
}
