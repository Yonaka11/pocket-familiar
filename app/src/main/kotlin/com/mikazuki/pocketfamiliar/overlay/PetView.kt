package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.model.PetProfile
import com.mikazuki.pocketfamiliar.model.PetRegistry
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.PetFrame
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * Custom [View] that renders the active pet sprite.
 *
 * Rendering approach: plain View with manual frame stepping. Built-in pets can
 * still use one drawable per frame, while larger character packs can use a
 * compact bitmap atlas. Atlas bitmaps are cached and drawn without filtering so
 * pixel-art edges stay crisp when the overlay is scaled.
 */
class PetView(context: Context) : View(context) {

    private var profile: PetProfile = PetRegistry.all.first()
    private var currentAnimation: PetAnimation? = null
    private var currentFrameIndex: Int = 0
    private var lastFrameTimeMs: Long = 0L
    private var currentDrawable: Drawable? = null
    private var currentAtlasFrame: PetFrame.Atlas? = null
    private var currentAtlasBitmap: Bitmap? = null
    private var flipped: Boolean = false

    private val atlasCache = mutableMapOf<Int, Bitmap>()
    private val pixelPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    // ── Profile switching ────────────────────────────────────────────────────

    fun setProfile(newProfile: PetProfile) {
        if (newProfile.id == profile.id) return
        profile = newProfile
        currentAnimation = null
        currentDrawable = null
        currentAtlasFrame = null
        currentAtlasBitmap = null
        currentFrameIndex = 0
        invalidate()
    }

    // ── State transitions ────────────────────────────────────────────────────

    fun applyState(state: PetState) {
        val newAnim = profile.animationForState(state)
        val newFlip = profile.isFlippedForState(state)

        if (newAnim != currentAnimation || newFlip != flipped) {
            currentAnimation = newAnim
            currentFrameIndex = 0
            lastFrameTimeMs = System.currentTimeMillis()
            flipped = newFlip
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
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        if (flipped) {
            canvas.save()
            canvas.scale(-1f, 1f, w / 2f, h / 2f)
        }

        when (val frame = currentAtlasFrame) {
            null -> currentDrawable?.let { drawable ->
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
            }

            else -> currentAtlasBitmap?.let { bitmap ->
                val cellWidth = bitmap.width / frame.columns
                val cellHeight = bitmap.height / frame.rows
                val column = frame.frameIndex % frame.columns
                val row = frame.frameIndex / frame.columns
                val source = Rect(
                    column * cellWidth,
                    row * cellHeight,
                    (column + 1) * cellWidth,
                    (row + 1) * cellHeight,
                )
                canvas.drawBitmap(bitmap, source, Rect(0, 0, w, h), pixelPaint)
            }
        }

        if (flipped) canvas.restore()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun loadCurrentFrame() {
        val anim = currentAnimation ?: return
        when (val frame = anim.frames[currentFrameIndex]) {
            is PetFrame.Resource -> {
                currentAtlasFrame = null
                currentAtlasBitmap = null
                currentDrawable = ContextCompat.getDrawable(context, frame.resId)
            }

            is PetFrame.Atlas -> {
                currentDrawable = null
                currentAtlasFrame = frame
                currentAtlasBitmap = atlasCache.getOrPut(frame.resId) {
                    requireNotNull(BitmapFactory.decodeResource(resources, frame.resId)) {
                        "Unable to decode sprite atlas resource ${frame.resId}"
                    }
                }
            }
        }
    }
}
