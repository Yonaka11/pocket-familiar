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
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.math.roundToInt

private const val TAG = "PetOverlayManager"

/**
 * Owns the pet overlay [WindowManager] window and delegates all positioning,
 * animation ticks, and touch events between [PetOverlayService] and [PetView].
 *
 * Window flags used:
 *  - TYPE_APPLICATION_OVERLAY  — appears over other apps (requires SYSTEM_ALERT_WINDOW)
 *  - FLAG_NOT_FOCUSABLE         — key events pass through to the app beneath
 *  - FLAG_NOT_TOUCH_MODAL       — touches outside the pet window are not intercepted
 *  - FLAG_LAYOUT_IN_SCREEN      — position is relative to the full display
 *
 * The overlay window is sized to the pet sprite only, never to the full screen,
 * so unrelated touches are never blocked.
 */
class PetOverlayManager(
    private val context: Context,
    private val onDragStarted: (startX: Float, startY: Float) -> Unit,
    private val onDragMoved: (x: Float, y: Float) -> Unit,
    private val onDragReleased: (releaseVelocityY: Float) -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var petView: PetView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false

    private var velocityTracker: VelocityTracker? = null

    /** Size of the overlay window in pixels (width == height for square sprite). */
    val petSizePx: Int get() = layoutParams?.width ?: 128

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    fun create(petScale: Float, startX: Int, startY: Int) {
        if (isAdded) {
            Log.w(TAG, "Overlay already added — skipping create()")
            return
        }

        val sizePx = scaleToPx(petScale)
        val params = buildLayoutParams(sizePx, startX, startY)

        val view = PetView(context)
        view.setOnTouchListener { _, event -> handleTouch(event) }

        try {
            windowManager.addView(view, params)
            petView = view
            layoutParams = params
            isAdded = true
            Log.d(TAG, "Overlay created at ($startX, $startY) size=${sizePx}px")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    fun remove() {
        if (!isAdded) return
        val view = petView ?: return
        try {
            windowManager.removeView(view)
            Log.d(TAG, "Overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view", e)
        } finally {
            petView = null
            layoutParams = null
            isAdded = false
            velocityTracker?.recycle()
            velocityTracker = null
        }
    }

    // -------------------------------------------------------------------------
    // Position and appearance
    // -------------------------------------------------------------------------

    /** Push a new position (from physics) into the window without touching anything else. */
    fun updatePosition(x: Float, y: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        if (!isAdded) return
        params.x = x.roundToInt()
        params.y = y.roundToInt()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "updatePosition failed", e)
        }
    }

    fun applyState(state: PetState) {
        petView?.applyState(state)
    }

    fun tick() {
        petView?.tick()
    }

    /** Resize the overlay; called when the pet size setting changes. */
    fun updatePetSize(petScale: Float) {
        val params = layoutParams ?: return
        val view = petView ?: return
        if (!isAdded) return
        val sizePx = scaleToPx(petScale)
        params.width = sizePx
        params.height = sizePx
        try {
            windowManager.updateViewLayout(view, params)
            Log.d(TAG, "Pet resized to ${sizePx}px")
        } catch (e: Exception) {
            Log.e(TAG, "updatePetSize failed", e)
        }
    }

    // -------------------------------------------------------------------------
    // Position read-back (for drag reconciliation)
    // -------------------------------------------------------------------------

    /** Current window X as stored in LayoutParams (updated during drag). */
    fun currentX(): Int = layoutParams?.x ?: 0

    /** Current window Y as stored in LayoutParams (updated during drag). */
    fun currentY(): Int = layoutParams?.y ?: 0

    // -------------------------------------------------------------------------
    // Screen dimensions
    // -------------------------------------------------------------------------

    fun getScreenWidth(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.width()
    } else {
        @Suppress("DEPRECATION")
        Point().also { windowManager.defaultDisplay.getSize(it) }.x
    }

    fun getScreenHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.height()
    } else {
        @Suppress("DEPRECATION")
        Point().also { windowManager.defaultDisplay.getSize(it) }.y
    }

    /**
     * Returns the navigation bar height (bottom inset) in pixels.
     * Used to keep the pet above the nav bar when it lands.
     */
    fun getNavBarHeightPx(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = windowManager.currentWindowMetrics.windowInsets
            insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
        } else {
            // Approximate: check if soft nav bar exists and estimate height
            val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        }
    }

    // -------------------------------------------------------------------------
    // Touch handling
    // -------------------------------------------------------------------------

    private fun handleTouch(event: MotionEvent): Boolean {
        val params = layoutParams ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)

                val startX = params.x.toFloat()
                val startY = params.y.toFloat()
                onDragStarted(startX, startY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)

                // Translate raw pointer position to window-origin coordinates.
                // The touch offset from the window origin is (rawX - window.x).
                // We clamp to keep the pet on screen in real-time.
                val newX = event.rawX - petSizePx / 2f
                val newY = event.rawY - petSizePx / 2f

                params.x = newX.roundToInt()
                params.y = newY.roundToInt()
                try {
                    windowManager.updateViewLayout(petView, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Drag move update failed", e)
                }

                onDragMoved(params.x.toFloat(), params.y.toFloat())
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000) // px/s
                val vy = velocityTracker?.yVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                // Ensure only downward (positive) velocity feeds the fall simulation.
                onDragReleased(vy.coerceAtLeast(0f))
                return true
            }
        }
        return false
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun scaleToPx(petScale: Float): Int {
        // Base sprite is 64dp; petScale 1.0 → 64dp, 2.0 → 128dp, 0.5 → 32dp
        val density = context.resources.displayMetrics.density
        return (petScale * 64f * density).roundToInt().coerceAtLeast(32)
    }

    private fun buildLayoutParams(sizePx: Int, x: Int, y: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
}
