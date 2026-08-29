package com.mikazuki.pocketfamiliar.story.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.story.model.StoryAccent
import com.mikazuki.pocketfamiliar.story.model.StoryBeat
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import com.mikazuki.pocketfamiliar.story.model.StoryPanelTreatment
import com.mikazuki.pocketfamiliar.story.model.StoryTransition
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Kinetic-comic story renderer.
 *
 * Story art is treated as optional presentation, never a reason to crash the app.
 * If a drawable cannot be decoded, the player substitutes an ink panel and the
 * episode can still be completed.
 */
@Composable
fun StoryPlayer(
    episode: StoryEpisode,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(episode.id) { mutableIntStateOf(0) }
    val beat = episode.beats.getOrNull(index)

    BackHandler(onBack = onExit)

    fun advance() {
        if (index < episode.beats.lastIndex) index += 1 else onComplete()
    }

    LaunchedEffect(index, beat?.autoAdvanceMs) {
        val delayMs = beat?.autoAdvanceMs ?: return@LaunchedEffect
        delay(delayMs)
        advance()
    }

    val accent = beatAccent(beat)
    val canTapAdvance = beat != null && beat !is StoryBeat.Interaction && beat.autoAdvanceMs == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030507))
            .clickable(enabled = canTapAdvance, onClick = ::advance),
    ) {
        KineticBackdrop(accent)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            StoryHeader(episode, index, accent)

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (beat != null) {
                    key(index) {
                        BeatView(beat = beat, onInteractionComplete = ::advance)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        beat is StoryBeat.Interaction -> "Interact to continue"
                        beat?.autoAdvanceMs != null -> ""
                        else -> "Tap to continue"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.46f),
                )
                Text(
                    text = "${(index + 1).coerceAtMost(episode.beats.size)} / ${episode.beats.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.46f),
                )
            }
        }
    }
}

@Composable
private fun StoryHeader(episode: StoryEpisode, index: Int, accent: StoryAccent) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = episode.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = accentColor(accent),
        )
        Text(
            text = episode.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.58f),
        )
        LinearProgressIndicator(
            progress = { ((index + 1).toFloat() / episode.beats.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            color = accentColor(accent),
            trackColor = Color.White.copy(alpha = 0.08f),
        )
    }
}

@Composable
private fun BeatView(beat: StoryBeat, onInteractionComplete: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(190),
        label = "storyAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else transitionStartScale(beat),
        animationSpec = tween(310),
        label = "storyScale",
    )
    val offsetX by animateFloatAsState(
        targetValue = if (entered) 0f else transitionStartX(beat),
        animationSpec = tween(260),
        label = "storyOffsetX",
    )
    val rotation by animateFloatAsState(
        targetValue = if (entered) 0f else transitionStartRotation(beat),
        animationSpec = tween(290),
        label = "storyRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                rotationZ = rotation
            },
        contentAlignment = Alignment.Center,
    ) {
        when (beat) {
            is StoryBeat.Panel -> PanelBeat(beat)
            is StoryBeat.Dialogue -> DialogueBeat(beat)
            is StoryBeat.InkShadow -> InkShadowBeat(beat)
            is StoryBeat.Flash -> FlashBeat(beat)
            is StoryBeat.Interaction -> InteractionBeat(beat, onInteractionComplete)
            is StoryBeat.MemoryUnlock -> MemoryUnlockBeat(beat)
            is StoryBeat.End -> EndBeat()
        }
    }
}

@Composable
private fun PanelBeat(beat: StoryBeat.Panel) {
    val accent = accentColor(beat.accent)
    val painter = try {
        painterResource(beat.imageResId)
    } catch (_: Exception) {
        null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(380.dp)
                .graphicsLayer {
                    scaleX = beat.cameraZoom
                    scaleY = beat.cameraZoom
                    rotationZ = if (beat.treatment == StoryPanelTreatment.GLITCHED) -1.2f else 0f
                }
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF080B10))
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                MissingArtFallback(beat.accent)
            }

            ComicInkOverlay(accent, beat.treatment)
        }

        beat.caption?.let { caption ->
            SpeechBubble(
                speaker = null,
                text = caption,
                accent = beat.accent,
                thought = false,
                compact = true,
            )
        }
    }
}

@Composable
private fun DialogueBeat(beat: StoryBeat.Dialogue) {
    SpeechBubble(
        speaker = beat.speaker,
        text = beat.text,
        accent = beat.accent,
        thought = beat.thought,
        compact = false,
    )
}

@Composable
private fun SpeechBubble(
    speaker: String?,
    text: String,
    accent: StoryAccent,
    thought: Boolean,
    compact: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(if (compact) 0.88f else 0.92f)
            .graphicsLayer { rotationZ = if (accent == StoryAccent.UNKNOWN) -1.4f else 0.6f }
            .clip(RoundedCornerShape(if (thought) 48.dp else 34.dp))
            .background(Color(0xFFF6F4F1))
            .border(3.dp, Color.Black, RoundedCornerShape(if (thought) 48.dp else 34.dp))
            .padding(horizontal = if (compact) 18.dp else 24.dp, vertical = if (compact) 14.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (speaker != null) {
            Text(
                text = speaker.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = accentColor(accent).copy(alpha = 0.92f),
            )
        }
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
            fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Bold,
            fontStyle = if (thought) FontStyle.Italic else FontStyle.Normal,
            color = Color(0xFF0B0A0C),
            lineHeight = if (compact) MaterialTheme.typography.titleMedium.lineHeight else MaterialTheme.typography.headlineMedium.lineHeight,
        )
    }
}

@Composable
private fun InkShadowBeat(beat: StoryBeat.InkShadow) {
    val accent = accentColor(beat.accent)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(420.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF050609))
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Comic halftone field.
                val step = 18f
                var y = 9f
                while (y < size.height) {
                    var x = 9f
                    while (x < size.width) {
                        val emphasis = ((x + y).toInt() / 18) % 4 == 0
                        drawCircle(
                            color = accent.copy(alpha = if (emphasis) 0.14f else 0.055f),
                            radius = if (emphasis) 2.2f else 1.25f,
                            center = androidx.compose.ui.geometry.Offset(x, y),
                        )
                        x += step
                    }
                    y += step
                }

                // Jagged ink strokes around the frame.
                repeat(34) { i ->
                    val f = i / 34f
                    drawLine(
                        color = Color.Black.copy(alpha = 0.78f),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height * f),
                        end = androidx.compose.ui.geometry.Offset(size.width * (0.18f + (i % 6) * 0.05f), size.height * (f + 0.06f)),
                        strokeWidth = 3f + (i % 4),
                    )
                }

                if (beat.halo) {
                    drawCircle(
                        color = accent.copy(alpha = 0.88f),
                        radius = size.minDimension * 0.22f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.24f),
                        style = Stroke(width = 8f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.25f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.24f),
                        style = Stroke(width = 2f),
                    )
                }

                // Human-like silhouette. Kept abstract on purpose so early Seraphi is incomplete.
                val cx = if (beat.looming) size.width * 0.52f else size.width * 0.50f
                val top = if (beat.looming) size.height * 0.03f else size.height * 0.18f
                val path = Path().apply {
                    moveTo(cx, top)
                    cubicTo(cx - 70f, top + 60f, cx - 110f, top + 175f, cx - 96f, top + 280f)
                    lineTo(cx - 155f, size.height)
                    lineTo(cx + 155f, size.height)
                    lineTo(cx + 96f, top + 280f)
                    cubicTo(cx + 110f, top + 175f, cx + 70f, top + 60f, cx, top)
                    close()
                }
                drawPath(path, color = Color.Black.copy(alpha = if (beat.looming) 0.97f else 0.86f))

                // Broken signal slices across the silhouette.
                repeat(9) { i ->
                    val yy = size.height * (0.17f + i * 0.075f)
                    drawLine(
                        color = accent.copy(alpha = 0.36f + (i % 3) * 0.12f),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, yy),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, yy + if (i % 2 == 0) 9f else -12f),
                        strokeWidth = if (i % 3 == 0) 7f else 3f,
                    )
                }
            }
        }

        beat.caption?.let {
            SpeechBubble(
                speaker = null,
                text = it,
                accent = beat.accent,
                thought = beat.looming,
                compact = true,
            )
        }
    }
}

@Composable
private fun FlashBeat(beat: StoryBeat.Flash) {
    Box(modifier = Modifier.fillMaxSize().background(accentColor(beat.accent).copy(alpha = 0.92f)))
}

@Composable
private fun MemoryUnlockBeat(beat: StoryBeat.MemoryUnlock) {
    val accent = accentColor(beat.accent)
    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xF20A0D12))
            .border(2.dp, accent.copy(alpha = 0.62f), RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("MEMORY FRAGMENT RECOVERED", color = accent, fontWeight = FontWeight.Black)
        Text(beat.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(
            beat.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        Canvas(Modifier.size(92.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val diamond = Path().apply {
                moveTo(center.x, 4f)
                lineTo(size.width - 12f, center.y)
                lineTo(center.x, size.height - 4f)
                lineTo(12f, center.y)
                close()
            }
            drawPath(diamond, color = accent.copy(alpha = 0.28f))
            drawPath(diamond, color = accent, style = Stroke(width = 4f))
        }
    }
}

@Composable
private fun InteractionBeat(beat: StoryBeat.Interaction, onComplete: () -> Unit) {
    var cleared by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }
    val accent = accentColor(beat.accent)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SpeechBubble(
            speaker = null,
            text = if (completed) beat.completionText else beat.prompt,
            accent = beat.accent,
            thought = false,
            compact = true,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(390.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF05070A))
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
                .pointerInput(completed) {
                    if (completed) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val amount = (abs(dragAmount.x) + abs(dragAmount.y)) / 1_260f
                        cleared = (cleared + amount).coerceIn(0f, 1f)
                        if (cleared >= 1f) completed = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Finger/static storyboard beat recreated procedurally.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF06080C))
                repeat(30) { i ->
                    val yy = size.height * (i / 30f)
                    val offset = if (i % 2 == 0) 42f else -28f
                    drawLine(
                        color = accent.copy(alpha = (0.18f + (1f - cleared) * 0.45f)),
                        start = androidx.compose.ui.geometry.Offset(0f, yy),
                        end = androidx.compose.ui.geometry.Offset(size.width, (yy + offset).coerceIn(0f, size.height)),
                        strokeWidth = if (i % 4 == 0) 6f else 2f,
                    )
                }
                // A bright tear behind the static.
                val tearX = size.width * 0.55f
                drawLine(
                    color = Color.White.copy(alpha = 0.30f + cleared * 0.65f),
                    start = androidx.compose.ui.geometry.Offset(tearX - 70f, 0f),
                    end = androidx.compose.ui.geometry.Offset(tearX + 60f, size.height),
                    strokeWidth = 16f,
                )
                drawLine(
                    color = accent.copy(alpha = 0.45f + cleared * 0.45f),
                    start = androidx.compose.ui.geometry.Offset(tearX - 62f, 0f),
                    end = androidx.compose.ui.geometry.Offset(tearX + 52f, size.height),
                    strokeWidth = 5f,
                )
            }

            Text(
                text = if (completed) "SIGNAL STABLE" else "/// CLEAR THE TEAR ///",
                color = if (completed) Color.White else accent,
                fontWeight = FontWeight.Black,
            )
        }

        LinearProgressIndicator(
            progress = { cleared },
            modifier = Modifier.fillMaxWidth(0.72f),
            color = accent,
            trackColor = Color.White.copy(alpha = 0.1f),
        )

        if (completed) Button(onClick = onComplete) { Text("Continue") }
    }
}

@Composable
private fun MissingArtFallback(accent: StoryAccent) {
    val color = accentColor(accent)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(20) { i ->
                val y = size.height * (i / 20f)
                drawLine(
                    color = color.copy(alpha = 0.16f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y + if (i % 2 == 0) 18f else -18f),
                    strokeWidth = 3f,
                )
            }
        }
        Text("SIGNAL IMAGE LOST", color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ComicInkOverlay(accent: Color, treatment: StoryPanelTreatment) {
    Canvas(Modifier.fillMaxSize()) {
        // Halftone corners and speed-line ink give the frames the manga/comic shadow language.
        val dotAlpha = if (treatment == StoryPanelTreatment.GLITCHED) 0.22f else 0.11f
        repeat(10) { row ->
            repeat(13) { col ->
                if ((row + col) % 2 == 0) {
                    drawCircle(
                        color = Color.Black.copy(alpha = dotAlpha),
                        radius = 2f + (row % 3),
                        center = androidx.compose.ui.geometry.Offset(col * 18f + 8f, row * 18f + 8f),
                    )
                }
            }
        }
        repeat(12) { i ->
            val x = size.width * (0.72f + i * 0.025f)
            drawLine(
                color = Color.Black.copy(alpha = 0.40f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x - size.width * 0.22f, size.height),
                strokeWidth = 2f + (i % 3),
            )
        }
        if (treatment == StoryPanelTreatment.GLITCHED) {
            repeat(7) { i ->
                val yy = size.height * (0.12f + i * 0.12f)
                drawLine(
                    color = accent.copy(alpha = 0.30f),
                    start = androidx.compose.ui.geometry.Offset(0f, yy),
                    end = androidx.compose.ui.geometry.Offset(size.width, yy + if (i % 2 == 0) 7f else -8f),
                    strokeWidth = 5f,
                )
            }
        }
    }
}

@Composable
private fun EndBeat() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("POCKET FAMILIAR", color = Color.White.copy(alpha = 0.58f), fontWeight = FontWeight.Black)
        Spacer(Modifier.size(8.dp))
        Text("The screen is quiet again.", color = Color.White.copy(alpha = 0.82f))
    }
}

@Composable
private fun KineticBackdrop(accent: StoryAccent) {
    val color = accentColor(accent)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF030507))
        val gap = size.width / 9f
        repeat(12) { i ->
            val x = i * gap - size.width * 0.15f
            drawLine(
                color = color.copy(alpha = if (i % 3 == 0) 0.13f else 0.045f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + size.width * 0.42f, size.height),
                strokeWidth = if (i % 3 == 0) 3f else 1f,
            )
        }
        // sparse halftone field
        repeat(18) { row ->
            repeat(9) { col ->
                if ((row * 3 + col) % 5 == 0) {
                    drawCircle(
                        color = color.copy(alpha = 0.055f),
                        radius = 1.5f,
                        center = androidx.compose.ui.geometry.Offset(col * size.width / 8f, row * size.height / 17f),
                    )
                }
            }
        }
    }
}

private fun beatAccent(beat: StoryBeat?): StoryAccent = when (beat) {
    is StoryBeat.Panel -> beat.accent
    is StoryBeat.Dialogue -> beat.accent
    is StoryBeat.InkShadow -> beat.accent
    is StoryBeat.Flash -> beat.accent
    is StoryBeat.Interaction -> beat.accent
    is StoryBeat.MemoryUnlock -> beat.accent
    else -> StoryAccent.CELESTIAL
}

private fun accentColor(accent: StoryAccent): Color = when (accent) {
    StoryAccent.ELECTRIC -> Color(0xFFFFD23D)
    StoryAccent.BLOOM -> Color(0xFF4BC0A9)
    StoryAccent.SCHOLAR -> Color(0xFFB75B79)
    StoryAccent.CELESTIAL -> Color(0xFFA78BFA)
    StoryAccent.UNKNOWN -> Color(0xFF8A8496)
}

private fun beatTransition(beat: StoryBeat): StoryTransition? = when (beat) {
    is StoryBeat.Panel -> beat.transition
    is StoryBeat.Dialogue -> beat.transition
    is StoryBeat.InkShadow -> beat.transition
    else -> null
}

private fun transitionStartScale(beat: StoryBeat): Float = when (beatTransition(beat)) {
    StoryTransition.GLITCH -> 1.08f
    StoryTransition.WHITE_FLASH -> 1.16f
    StoryTransition.PAGE_TURN -> 0.94f
    else -> 0.98f
}

private fun transitionStartX(beat: StoryBeat): Float = when (beatTransition(beat)) {
    StoryTransition.SLIDE_LEFT -> 180f
    StoryTransition.GLITCH -> 28f
    else -> 0f
}

private fun transitionStartRotation(beat: StoryBeat): Float = when (beatTransition(beat)) {
    StoryTransition.GLITCH -> -2.8f
    StoryTransition.PAGE_TURN -> 3.5f
    StoryTransition.SLIDE_UP -> 1.2f
    else -> 0f
}
