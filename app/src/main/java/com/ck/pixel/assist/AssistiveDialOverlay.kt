package com.ck.pixel.assist

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ck.pixel.R
import com.ck.pixel.systemSoundIntent
import kotlin.math.abs

class AssistiveDialOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val root = FrameLayout(service)
    private val sound = actionButton(R.drawable.ic_qs_sound, R.string.assistive_sound) {
        closeMenu()
        service.startActivity(systemSoundIntent())
    }
    private val screenOff = actionButton(R.drawable.ic_screen_off, R.string.assistive_screen_off) {
        closeMenu()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
    private val handleWidth = dp(48)
    private val handleHeight = dp(24)
    private val handle = logoButton()
    private var menuOpen = false
    private var added = false

    private val params = WindowManager.LayoutParams(
        handleWidth,
        handleHeight,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        android.graphics.PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.END
        x = 0
        y = dp(72)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fitInsetsTypes = 0
            isFitInsetsIgnoringVisibility = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    fun show() {
        if (added) return
        root.addView(screenOff, placed(dp(4), dp(108)))
        root.addView(sound, placed(dp(96), dp(52)))
        root.addView(
            handle,
            FrameLayout.LayoutParams(handleWidth, handleHeight, Gravity.BOTTOM or Gravity.END),
        )
        attachDrag()
        closeMenu()
        windowManager.addView(root, params)
        added = true
    }

    fun hide() {
        if (!added) return
        windowManager.removeView(root)
        added = false
    }

    private fun toggleMenu() {
        if (menuOpen) closeMenu() else openMenu()
    }

    private fun openMenu() {
        menuOpen = true
        resize(dp(220), dp(180))
        sound.visibility = View.VISIBLE
        screenOff.visibility = View.VISIBLE
    }

    private fun closeMenu() {
        menuOpen = false
        sound.visibility = View.GONE
        screenOff.visibility = View.GONE
        resize(handleWidth, handleHeight)
    }

    private fun resize(width: Int, height: Int) {
        params.width = width
        params.height = height
        if (added) windowManager.updateViewLayout(root, params)
    }

    private fun placed(marginEnd: Int, marginBottom: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END,
        ).apply {
            this.marginEnd = marginEnd
            bottomMargin = marginBottom
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDrag() {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(8) || abs(dy) > dp(8)) dragged = true
                    if (dragged && !menuOpen) {
                        val (screenWidth, screenHeight) = screenSize()
                        val maxX = (screenWidth - params.width).coerceAtLeast(0)
                        val maxY = (screenHeight - params.height).coerceAtLeast(0)
                        params.x = (startX - dx.toInt()).coerceIn(0, maxX)
                        params.y = (startY - dy.toInt()).coerceIn(0, maxY)
                        windowManager.updateViewLayout(root, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) toggleMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun actionButton(icon: Int, label: Int, onClick: () -> Unit): LinearLayout {
        val button = circleButton(dp(48), icon, label)
        val text = TextView(service).apply {
            setText(label)
            setTextColor(Color.parseColor("#FFF4F1EA"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(button)
            addView(text)
            contentDescription = service.getString(label)
            setOnClickListener { onClick() }
            button.setOnClickListener { onClick() }
        }
    }

    private fun logoButton(): TextView {
        return TextView(service).apply {
            text = service.getString(R.string.assistive_logo)
            setTextColor(Color.parseColor("#FFF4F1EA"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            includeFontPadding = false
            contentDescription = service.getString(R.string.assistive_dial_title)
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(Color.parseColor("#FF1C1C1E"))
                setStroke(dp(1), Color.parseColor("#FFF4F1EA"))
            }
        }
    }

    private fun circleButton(size: Int, icon: Int, description: Int, filled: Boolean = true): ImageView {
        return ImageView(service).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
            setImageResource(icon)
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = service.getString(description)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#FFF4F1EA"))
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(Color.parseColor("#FF1C1C1E"))
                setStroke(dp(1), Color.parseColor("#FFF4F1EA"))
            }
            if (!filled) {
                imageTintList = ColorStateList.valueOf(Color.parseColor("#FFF4F1EA"))
            }
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private fun dp(value: Int): Int {
        return (value * service.resources.displayMetrics.density).toInt()
    }
}
