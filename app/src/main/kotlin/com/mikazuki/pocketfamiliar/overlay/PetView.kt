package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimationSet
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * A lightweight custom [View] that renders the pet sprite.
 *
 * We chose a plain View over ComposeView because:
 *  1. The overlay window is detached from the Activity lifecycle; Compose
 *     requires a ViewTreeLifecycleOwner/SavedStateRegistryOwner which adds
 *     significant boilerplate to attach to a bare WindowManager view.
 *  2. A custom View with manual frame stepping is simpler, has zero extra
 *     dependencies, and draws only what it needs — ideal for a small overlay.
 *  3. Invalidation is controlled externally by the service ticker, not by
 *     Compose's recomposition scheduler.
 */
class PetView(context: Context) : View(context) {

    private var currentAnimation: PetAnimation? = null
    private var currentFrameIndex: Int = 0
    private var lastFrameTimeMs: Long = 0L
    private var currentDrawable: Drawable? = null
    private var flipped: Boolean = false

    /**
     * Update the displayed animation when the pet state changes.
     * Resets the frame counter so the new animation always starts at frame 0.
     */
    fun applyState(state: PetState) {
        val newAnimation = PetAnimationSet.forState(state)
        val shouldFlip = PetAnimationSet.isFlipped(state)

        if (newAnimation != currentAnimation || shouldFlip != flipped) {
            currentAnimation = newAnimation

            currentFrameIndex = 0
            lastFrameTimeMs = System.currentTimeMillis()
            flipped = shouldFlip
            loadCurrentFrame()
            invalidate()
        }
    }

    /**
     * Called by the service ticker on every animation update.
     * Advances the frame if enough time has passed.
     */
    fun tick() {
        val anim = currentAnimation ?: return
        val now = System.currentTimeMillis()
        if (now - lastFrameTimeMs >= anim.frameDurationMs) {
            lastFrameTimeMs = now
            currentFrameIndex = if (anim.loop) {
                (currentFrameIndex + 1) % anim.frames.size
            } else {
                (currentFrameIndex + 1).coerceAtMost(anim.frames.size - 1)
            }
            loadCurrentFrame()
            invalidate()
        }
    }

    private fun loadCurrentFrame() {
        val anim = currentAnimation ?: return
        currentDrawable = ContextCompat.getDrawable(context, anim.frames[currentFrameIndex])
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = currentDrawable ?: return

        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        if (flipped) {
            // Flip horizontally around the center
            canvas.save()
            canvas.scale(-1f, 1f, w / 2f, h / 2f)
        }

        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)

        if (flipped) {
            canvas.restore()
        }
    }
}
