package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.WindowManager
import com.mikazuki.pocketfamiliar.model.PetProfile
import com.mikazuki.pocketfamiliar.model.TouchInteraction
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val TAG = "PetOverlayManager"
private const val TAP_MAX_MS = 220L
private const val DOUBLE_TAP_WINDOW_MS = 320L
private const val TAP_MOVE_FRACTION = 0.18f
private const val PET_MOVE_FRACTION = 0.65f
private const val TICKLE_MAX_MS = 900L
private const val FLICK_OVERRIDE_PX_S = 180f

class PetOverlayManager(
    private val context: Context,
    private val onDragStarted: (startX: Float, startY: Float) -> Unit,
    private val onDragMoved: (x: Float, y: Float) -> Unit,
    private val onDragReleased: (releaseVelocityX: Float, releaseVelocityY: Float) -> Unit,
    private val onTouchInteraction: (TouchInteraction) -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var petView: PetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false
    private var velocityTracker: VelocityTracker? = null
    private var grabOffsetX = 0f
    private var grabOffsetY = 0f
    private var touchDownRawX = 0f
    private var touchDownRawY = 0f
    private var touchDownMs = 0L
    private var totalTouchTravel = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var lastTapMs = 0L

    val petSizePx: Int get() = layoutParams?.width ?: 128

    fun create(petScale: Float, startX: Int, startY: Int) {
        if (isAdded) return
        val sizePx = scaleToPx(petScale)
        val params = buildLayoutParams(sizePx, startX, startY)
        val view = PetView(context)
        view.setOnTouchListener { _, event -> handleTouch(event) }
        try {
            windowManager.addView(view, params)
            petView = view
            layoutParams = params
            isAdded = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    fun remove() {
        if (!isAdded) return
        val view = petView ?: return
        try { windowManager.removeView(view) } catch (e: Exception) { Log.e(TAG, "Failed to remove overlay view", e) }
        petView = null
        layoutParams = null
        isAdded = false
        velocityTracker?.recycle()
        velocityTracker = null
    }

    fun updatePosition(x: Float, y: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        params.x = x.roundToInt()
        params.y = y.roundToInt()
        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { Log.e(TAG, "updatePosition failed", e) }
    }

    fun applyState(state: PetState) { petView?.applyState(state) }
    fun setProfile(profile: PetProfile) { petView?.setProfile(profile) }
    fun tick() { petView?.tick() }

    fun updatePetSize(petScale: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        val sizePx = scaleToPx(petScale)
        params.width = sizePx
        params.height = sizePx
        try { windowManager.updateViewLayout(view, params) } catch (e: Exception) { Log.e(TAG, "updatePetSize failed", e) }
    }

    fun currentX(): Int = layoutParams?.x ?: 0
    fun currentY(): Int = layoutParams?.y ?: 0

    fun getScreenWidth(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) windowManager.currentWindowMetrics.bounds.width() else {
        @Suppress("DEPRECATION") Point().also { windowManager.defaultDisplay.getSize(it) }.x
    }

    fun getScreenHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) windowManager.currentWindowMetrics.bounds.height() else {
        @Suppress("DEPRECATION") Point().also { windowManager.defaultDisplay.getSize(it) }.y
    }

    fun getNavBarHeightPx(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
    } else {
        val id = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                grabOffsetX = event.x
                grabOffsetY = event.y
                touchDownRawX = event.rawX
                touchDownRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                totalTouchTravel = 0f
                touchDownMs = event.eventTime
                onDragStarted(params.x.toFloat(), params.y.toFloat())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                totalTouchTravel += hypot(event.rawX - lastRawX, event.rawY - lastRawY)
                lastRawX = event.rawX
                lastRawY = event.rawY
                params.x = (event.rawX - grabOffsetX).roundToInt()
                params.y = (event.rawY - grabOffsetY).roundToInt()
                try { windowManager.updateViewLayout(petView, params) } catch (e: Exception) { Log.e(TAG, "Drag move update failed", e) }
                onDragMoved(params.x.toFloat(), params.y.toFloat())
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val vx = velocityTracker?.xVelocity ?: 0f
                val vy = velocityTracker?.yVelocity ?: 0f
                val releaseSpeed = hypot(vx, vy)
                velocityTracker?.recycle()
                velocityTracker = null

                if (event.actionMasked == MotionEvent.ACTION_UP && releaseSpeed < FLICK_OVERRIDE_PX_S) {
                    classifyTouch(event.eventTime, event.x, event.y)?.let(onTouchInteraction)
                }
                onDragReleased(vx, vy)
                return true
            }
        }
        return false
    }

    private fun classifyTouch(upMs: Long, localX: Float, localY: Float): TouchInteraction? {
        val duration = upMs - touchDownMs
        val straightDistance = hypot(lastRawX - touchDownRawX, lastRawY - touchDownRawY)
        val tapLimit = petSizePx * TAP_MOVE_FRACTION
        val petLimit = petSizePx * PET_MOVE_FRACTION

        if (duration <= TAP_MAX_MS && straightDistance <= tapLimit && totalTouchTravel <= tapLimit * 1.5f) {
            if (upMs - lastTapMs <= DOUBLE_TAP_WINDOW_MS) {
                lastTapMs = 0L
                return TouchInteraction.DOUBLE_TAP
            }
            lastTapMs = upMs
            val centered = localX in petSizePx * 0.30f..petSizePx * 0.70f
            val faceZone = localY in petSizePx * 0.15f..petSizePx * 0.48f
            return if (centered && faceZone) TouchInteraction.BOOP else TouchInteraction.TAP
        }

        if (duration in 250L..TICKLE_MAX_MS && totalTouchTravel >= petLimit * 1.5f) {
            return TouchInteraction.TICKLE
        }

        if (duration >= 280L && totalTouchTravel >= petLimit && straightDistance < totalTouchTravel * 0.75f) {
            return TouchInteraction.PET
        }

        return null
    }

    private fun scaleToPx(petScale: Float): Int {
        val density = context.resources.displayMetrics.density
        return (petScale * 64f * density).roundToInt().coerceAtLeast(32)
    }

    private fun buildLayoutParams(sizePx: Int, x: Int, y: Int): WindowManager.LayoutParams = WindowManager.LayoutParams(
        sizePx, sizePx, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        this.x = x
        this.y = y
    }
}
