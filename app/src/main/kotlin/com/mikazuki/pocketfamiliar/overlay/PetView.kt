package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.model.PetProfile
import com.mikazuki.pocketfamiliar.model.PetRegistry
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * Custom [View] that renders the active pet sprite.
 *
 * Rendering approach: plain View with manual frame stepping.
 * The service calls [tick] on every animation tick and [applyState] on every
 * state transition; [invalidate] is only called when the frame actually changes,
 * so battery impact is minimal.
 *
 * Horizontal flipping for left-facing states is done by scaling the canvas
 * around its centre, so we don't need mirrored sprite assets.
 */
class PetView(context: Context) : View(context) {

    private var profile: PetProfile = PetRegistry.all.first()
    private var currentAnimation: PetAnimation? = null
    private var currentFrameIndex: Int = 0
    private var lastFrameTimeMs: Long = 0L
    private var currentDrawable: Drawable? = null
    private var flipped: Boolean = false

    // ── Profile switching ────────────────────────────────────────────────────

    fun setProfile(newProfile: PetProfile) {
        if (newProfile.id == profile.id) return
        profile = newProfile
        currentAnimation = null
        currentDrawable = null
        currentFrameIndex = 0
        invalidate()
    }

    // ── State transitions ────────────────────────────────────────────────────

    fun applyState(state: PetState) {
        val newAnim  = profile.animationForState(state)
        val newFlip  = profile.isFlippedForState(state)

        if (newAnim != currentAnimation || newFlip != flipped) {
            currentAnimation  = newAnim
            currentFrameIndex = 0
            lastFrameTimeMs   = System.currentTimeMillis()
            flipped           = newFlip
            loadCurrentFrame()
            invalidate()
        }
    }

    // ── Animation tick ───────────────────────────────────────────────────────

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

    // ── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val drawable = currentDrawable ?: return
        val w = width; val h = height
        if (w <= 0 || h <= 0) return

        if (flipped) {
            canvas.save()
            canvas.scale(-1f, 1f, w / 2f, h / 2f)
        }
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        if (flipped) canvas.restore()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun loadCurrentFrame() {
        val anim = currentAnimation ?: return
        currentDrawable = ContextCompat.getDrawable(context, anim.frames[currentFrameIndex])
    }
}
