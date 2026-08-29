package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.mikazuki.pocketfamiliar.model.PetProfile
import com.mikazuki.pocketfamiliar.model.PetRegistry
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.PetFrame
import com.mikazuki.pocketfamiliar.pet.behavior.PetState
import kotlin.math.abs
import kotlin.math.sin

private const val TAG = "PetView"

/** Renders atlas/strip sprites plus procedural motion for single-frame familiar forms. */
class PetView(context: Context) : View(context) {

    private var profile: PetProfile = PetRegistry.all.first()
    private var currentAnimation: PetAnimation? = null
    private var currentState: PetState = PetState.Idle
    private var currentFrameIndex: Int = 0
    private var lastFrameTimeMs: Long = 0L
    private var currentDrawable: Drawable? = null
    private var currentAtlasFrame: PetFrame.Atlas? = null
    private var currentAtlasBitmap: Bitmap? = null
    private var flipped: Boolean = false
    private var usingAtlasFallback: Boolean = false

    private val atlasCache = mutableMapOf<Int, Bitmap>()
    private val failedAtlasResources = mutableSetOf<Int>()
    private val pixelPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    fun setProfile(newProfile: PetProfile) {
        if (newProfile == profile) return
        profile = newProfile
        currentAnimation = null
        currentDrawable = null
        currentAtlasFrame = null
        currentAtlasBitmap = null
        currentFrameIndex = 0
        usingAtlasFallback = false
        loadAnimationForCurrentState(forceRestart = true)
        invalidate()
    }

    fun applyState(state: PetState) {
        val stateChanged = state != currentState
        currentState = state
        // One-shot reactions/specials must replay when Character Lab or touch input
        // forces the same state again. Looping locomotion keeps its current cycle.
        val shouldReplayOneShot = currentAnimation?.loop == false
        loadAnimationForCurrentState(forceRestart = stateChanged || shouldReplayOneShot)
    }

    private fun loadAnimationForCurrentState(forceRestart: Boolean = false) {
        val newAnim = profile.animationForState(currentState)
        val newFlip = profile.isFlippedForState(currentState)
        if (forceRestart || newAnim != currentAnimation || newFlip != flipped) {
            currentAnimation = newAnim
            currentFrameIndex = 0
            lastFrameTimeMs = System.currentTimeMillis()
            flipped = newFlip
            loadCurrentFrame()
        }
        invalidate()
    }

    fun tick() {
        val anim = currentAnimation ?: return
        val now = System.currentTimeMillis()
        if (now - lastFrameTimeMs >= anim.frameDurationMs) {
            lastFrameTimeMs = now
            val nextFrame = if (anim.loop) {
                (currentFrameIndex + 1) % anim.frames.size
            } else {
                (currentFrameIndex + 1).coerceAtMost(anim.frames.size - 1)
            }
            if (nextFrame != currentFrameIndex) {
                currentFrameIndex = nextFrame
                loadCurrentFrame()
                // Critical: changing an atlas cell does not mutate the View by
                // itself. Explicitly redraw so the user actually sees the frame.
                invalidate()
            }
        }
        if (profile.proceduralMotion || usingAtlasFallback) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        canvas.save()
        if (flipped) canvas.scale(-1f, 1f, w / 2f, h / 2f)
        if (profile.proceduralMotion || usingAtlasFallback) {
            applyProceduralMotion(canvas, w.toFloat(), h.toFloat())
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
        canvas.restore()
    }

    /** Gives static familiar-form art readable idle/walk/run/reaction animation. */
    private fun applyProceduralMotion(canvas: Canvas, w: Float, h: Float) {
        val t = SystemClock.uptimeMillis() / 1000f
        val cx = w / 2f
        val cy = h / 2f

        var bob = 0f
        var rotation = 0f
        var scaleX = 1f
        var scaleY = 1f

        when (currentState) {
            is PetState.Idle -> {
                bob = sin(t * 3.2f) * h * 0.018f
                scaleY = 1f + sin(t * 3.2f) * 0.018f
            }
            is PetState.WalkLeft, is PetState.WalkRight, is PetState.ClimbLeft, is PetState.ClimbRight -> {
                bob = -abs(sin(t * 8f)) * h * 0.045f
                rotation = sin(t * 8f) * 3.5f
            }
            is PetState.RunLeft, is PetState.RunRight, is PetState.StepActivity -> {
                bob = -abs(sin(t * 12f)) * h * 0.065f
                rotation = sin(t * 12f) * 6f
                scaleX = 1.035f
                scaleY = 0.975f
            }
            is PetState.Sleep, is PetState.DeepSleep, is PetState.Charging, is PetState.LowBattery -> {
                scaleX = 1.04f + sin(t * 2f) * 0.012f
                scaleY = 0.95f - sin(t * 2f) * 0.008f
                bob = h * 0.025f
            }
            is PetState.Happy, is PetState.Music, is PetState.Grooming, is PetState.Eating -> {
                bob = -abs(sin(t * 7f)) * h * 0.075f
                rotation = sin(t * 7f) * 5f
                scaleX = 1.03f
                scaleY = 1.03f
            }
            is PetState.Held -> rotation = sin(t * 4f) * 7f
            is PetState.Thrown, is PetState.Falling, is PetState.Jumping -> rotation = sin(t * 9f) * 11f
            is PetState.HardLanding -> {
                scaleX = 1.10f
                scaleY = 0.84f
                bob = h * 0.06f
            }
            is PetState.Recovering -> {
                scaleX = 1f + sin(t * 5f) * 0.025f
                scaleY = 1f - sin(t * 5f) * 0.025f
            }
        }

        canvas.translate(0f, bob)
        canvas.rotate(rotation, cx, cy)
        canvas.scale(scaleX, scaleY, cx, cy)
    }

    private fun loadCurrentFrame() {
        val anim = currentAnimation ?: return
        when (val frame = anim.frames[currentFrameIndex]) {
            is PetFrame.Resource -> {
                usingAtlasFallback = false
                currentAtlasFrame = null
                currentAtlasBitmap = null
                currentDrawable = ContextCompat.getDrawable(context, frame.resId)
            }
            is PetFrame.Atlas -> {
                if (frame.resId in failedAtlasResources) {
                    usePreviewFallback(frame.resId)
                    return
                }

                val decoded = runCatching {
                    atlasCache.getOrPut(frame.resId) { decodeAtlasBitmap(frame.resId) }
                }.onFailure { error ->
                    Log.e(TAG, "Atlas/strip decode failed for ${profile.id} resource ${frame.resId}; using preview fallback", error)
                }.getOrNull()

                if (decoded == null) {
                    failedAtlasResources += frame.resId
                    usePreviewFallback(frame.resId)
                } else {
                    usingAtlasFallback = false
                    currentDrawable = null
                    currentAtlasFrame = frame
                    currentAtlasBitmap = decoded
                }
            }
        }
    }

    /** A bad art asset must never take down the overlay service. */
    private fun usePreviewFallback(failedResId: Int) {
        usingAtlasFallback = true
        currentAtlasFrame = null
        currentAtlasBitmap = null
        currentDrawable = ContextCompat.getDrawable(context, profile.previewResId)
            ?: ContextCompat.getDrawable(context, PetRegistry.all.first().previewResId)
        Log.w(TAG, "Using preview fallback for ${profile.id}; failed sprite resource=$failedResId")
    }

    private fun decodeAtlasBitmap(resId: Int): Bitmap {
        BitmapFactory.decodeResource(resources, resId)?.let { return it }
        runCatching {
            resources.openRawResource(resId).use { stream -> BitmapFactory.decodeStream(stream) }
        }.getOrNull()?.let { return it }

        val drawable = ContextCompat.getDrawable(context, resId)
            ?: throw IllegalArgumentException("Unable to load sprite resource $resId")
        if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val raster = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(raster)
        }
    }
}
