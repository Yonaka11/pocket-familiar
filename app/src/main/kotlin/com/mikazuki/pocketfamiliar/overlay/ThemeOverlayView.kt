package com.mikazuki.pocketfamiliar.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.FamiliarTheme
import com.mikazuki.pocketfamiliar.model.FamiliarThemeCatalog
import com.mikazuki.pocketfamiliar.model.ThemeVisual
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight, click-through visual layer behind the familiar.
 * Effects are drawn procedurally so theme rewards add almost no APK weight.
 *
 * The earned theme is rendered normally. Debug themes may be stacked on top so
 * individual effects can be tested without changing unlock state.
 */
class ThemeOverlayView(context: Context) : View(context) {
    private var primaryTheme: FamiliarTheme = FamiliarThemeCatalog.getById(FamiliarThemeCatalog.DEFAULT_THEME_ID)
    private var debugThemes: List<FamiliarTheme> = emptyList()
    private var animationStartMs = SystemClock.uptimeMillis()
    private var elapsedSeconds = 0f
    private var scope: CoroutineScope? = null
    private var settingsJob: Job? = null

    private val softPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.25f * resources.displayMetrics.density
    }
    private val petalPath = Path()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animationStartMs = SystemClock.uptimeMillis()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        settingsJob = newScope.launch {
            PetSettingsRepository(context.applicationContext).settingsFlow.collect { settings ->
                primaryTheme = FamiliarThemeCatalog.getById(settings.selectedThemeId)
                debugThemes = FamiliarThemeCatalog.all.filter { theme ->
                    theme.visual != ThemeVisual.NONE && theme.id in settings.debugThemeIds
                }
                animationStartMs = SystemClock.uptimeMillis()
                invalidate()
            }
        }
    }

    override fun onDetachedFromWindow() {
        settingsJob?.cancel()
        settingsJob = null
        scope?.cancel()
        scope = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        elapsedSeconds = (SystemClock.uptimeMillis() - animationStartMs) / 1000f

        val activeThemes = buildList {
            if (primaryTheme.visual != ThemeVisual.NONE) add(primaryTheme)
            debugThemes.forEach { debug -> if (none { it.id == debug.id }) add(debug) }
        }
        activeThemes.forEach { drawTheme(canvas, it.visual) }
        if (activeThemes.isNotEmpty()) postInvalidateOnAnimation()
    }

    private fun drawTheme(canvas: Canvas, visual: ThemeVisual) {
        when (visual) {
            ThemeVisual.NONE -> Unit
            ThemeVisual.SAKURA_DRIFT -> drawSakura(canvas)
            ThemeVisual.NEON_TECH_FRAME -> drawTechFrame(canvas)
            ThemeVisual.COZY_STUDY_GLOW -> drawCozyGlow(canvas)
            ThemeVisual.CELESTIAL_HALO -> drawCelestial(canvas)
            ThemeVisual.MOON_DUST -> drawMoonDust(canvas)
            ThemeVisual.BLOOM_GARDEN -> drawBloomGarden(canvas)
        }
    }

    private fun drawSakura(canvas: Canvas) {
        repeat(26) { i ->
            val phase = i * 0.173f
            val y = ((elapsedSeconds * (34f + i % 5 * 7f) + phase * height) % (height + 80f)) - 40f
            val xBase = ((i * 71f) % width.coerceAtLeast(1))
            val x = xBase + sin(elapsedSeconds * 0.9f + i) * 36f
            val size = 5.5f + (i % 4) * 1.8f
            softPaint.color = if (i % 3 == 0) Color.rgb(255, 205, 220) else Color.rgb(255, 170, 198)
            softPaint.alpha = 190
            drawPetal(canvas, x, y, size, elapsedSeconds * 42f + i * 19f)
        }
    }

    private fun drawTechFrame(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val inset = 14f * d
        val corner = 62f * d
        val pulse = (175 + 70 * (0.5f + 0.5f * sin(elapsedSeconds * 2.3f))).toInt()
        linePaint.color = Color.rgb(30, 125, 255)
        linePaint.alpha = pulse
        linePaint.strokeWidth = 2.25f * d

        fun cornerPath(left: Float, top: Float, sx: Float, sy: Float) {
            canvas.drawLine(left, top, left + sx * corner, top, linePaint)
            canvas.drawLine(left, top, left, top + sy * corner, linePaint)
            canvas.drawLine(left + sx * corner * 0.34f, top + sy * 8f * d, left + sx * corner * 0.74f, top + sy * 8f * d, linePaint)
            canvas.drawLine(left + sx * corner * 0.72f, top, left + sx * corner * 0.82f, top + sy * 7f * d, linePaint)
        }
        cornerPath(inset, inset, 1f, 1f)
        cornerPath(width - inset, inset, -1f, 1f)
        cornerPath(inset, height - inset, 1f, -1f)
        cornerPath(width - inset, height - inset, -1f, -1f)

        linePaint.color = Color.rgb(255, 187, 24)
        linePaint.alpha = 185
        repeat(7) { i ->
            val y = height * (0.16f + i * 0.115f)
            canvas.drawLine(inset, y, inset + (9f + i % 3 * 6f) * d, y, linePaint)
            canvas.drawLine(width - inset, y, width - inset - (9f + i % 3 * 6f) * d, y, linePaint)
        }
    }

    private fun drawCozyGlow(canvas: Canvas) {
        val d = resources.displayMetrics.density
        repeat(20) { i ->
            val angle = i * 2f * PI.toFloat() / 20f
            val radiusX = width * 0.48f
            val radiusY = height * 0.46f
            val wobble = sin(elapsedSeconds * 0.7f + i) * 12f * d
            val x = width / 2f + cos(angle) * (radiusX + wobble)
            val y = height / 2f + sin(angle) * (radiusY + wobble)
            softPaint.color = Color.rgb(255, 177, 83)
            softPaint.alpha = 95 + (i % 4) * 24
            canvas.drawCircle(x, y, (2.8f + i % 3) * d, softPaint)
        }
    }

    private fun drawCelestial(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        linePaint.color = Color.rgb(255, 210, 95)
        linePaint.alpha = 135
        linePaint.strokeWidth = 1.7f * d
        repeat(3) { ring ->
            val radius = (46f + ring * 20f) * d
            canvas.save()
            canvas.rotate(elapsedSeconds * (8f + ring * 5f), cx, cy)
            canvas.drawCircle(cx, cy, radius, linePaint)
            repeat(4 + ring * 2) { i ->
                val a = i * 2f * PI.toFloat() / (4 + ring * 2)
                val x = cx + cos(a) * radius
                val y = cy + sin(a) * radius
                softPaint.color = Color.WHITE
                softPaint.alpha = 180
                canvas.drawCircle(x, y, 2.0f * d, softPaint)
            }
            canvas.restore()
        }
        drawEdgeStars(canvas, Color.rgb(255, 231, 155), 18)
    }

    private fun drawMoonDust(canvas: Canvas) {
        val d = resources.displayMetrics.density
        repeat(36) { i ->
            val speed = 8f + (i % 6) * 3f
            val y = height - ((elapsedSeconds * speed + i * 83f) % (height + 50f))
            val x = ((i * 137f) % width.coerceAtLeast(1)) + sin(elapsedSeconds * 0.35f + i) * 18f * d
            softPaint.color = if (i % 4 == 0) Color.rgb(180, 205, 255) else Color.rgb(220, 225, 245)
            softPaint.alpha = 70 + (i % 5) * 24
            canvas.drawCircle(x, y, (1.35f + i % 3 * 0.75f) * d, softPaint)
        }
    }

    private fun drawBloomGarden(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val edge = 12f * d
        repeat(20) { i ->
            val progress = i / 19f
            val x = edge + progress * (width - edge * 2)
            val bob = sin(elapsedSeconds * 0.8f + i * 0.7f) * 4f * d
            val topY = edge + bob
            val bottomY = height - edge + bob
            softPaint.color = if (i % 2 == 0) Color.rgb(61, 153, 120) else Color.rgb(238, 179, 74)
            softPaint.alpha = 145
            canvas.drawCircle(x, if (i % 3 == 0) topY else bottomY, (2.6f + i % 3) * d, softPaint)
        }
        linePaint.color = Color.rgb(50, 130, 100)
        linePaint.alpha = 95
        linePaint.strokeWidth = 2.2f * d
        canvas.drawLine(edge, edge, edge, height - edge, linePaint)
        canvas.drawLine(width - edge, edge, width - edge, height - edge, linePaint)
        drawEdgeStars(canvas, Color.rgb(255, 218, 126), 12)
    }

    private fun drawEdgeStars(canvas: Canvas, color: Int, count: Int) {
        val d = resources.displayMetrics.density
        repeat(count) { i ->
            val x = ((i * 193f + 37f) % width.coerceAtLeast(1))
            val y = if (i % 2 == 0) 28f * d + (i % 4) * 15f * d else height - 32f * d - (i % 3) * 18f * d
            val pulse = 0.45f + 0.55f * (0.5f + 0.5f * sin(elapsedSeconds * 2f + i))
            softPaint.color = color
            softPaint.alpha = (195 * pulse).toInt()
            canvas.drawCircle(x, y, (1.7f + i % 3) * d, softPaint)
        }
    }

    private fun drawPetal(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float) {
        petalPath.reset()
        petalPath.moveTo(0f, -size)
        petalPath.quadTo(size, -size * 0.15f, 0f, size)
        petalPath.quadTo(-size, -size * 0.15f, 0f, -size)
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.drawPath(petalPath, softPaint)
        canvas.restore()
    }
}
