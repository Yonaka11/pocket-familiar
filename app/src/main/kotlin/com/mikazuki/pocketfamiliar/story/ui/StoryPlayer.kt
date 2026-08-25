package com.mikazuki.pocketfamiliar.story.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.story.model.StoryAccent
import com.mikazuki.pocketfamiliar.story.model.StoryBeat
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import com.mikazuki.pocketfamiliar.story.model.StoryTransition
import kotlinx.coroutines.delay
import kotlin.math.abs

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
            .background(Color(0xFF05070A))
            .clickable(enabled = canTapAdvance, onClick = ::advance),
    ) {
        KineticBackdrop(accent)

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor(accent),
                )
                Text(
                    text = episode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
                LinearProgressIndicator(
                    progress = { ((index + 1).toFloat() / episode.beats.size).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = accentColor(accent),
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (beat != null) {
                    key(index) {
                        BeatView(beat = beat, onInteractionComplete = ::advance)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    color = Color.White.copy(alpha = 0.45f),
                )
                Text(
                    text = "${(index + 1).coerceAtMost(episode.beats.size)} / ${episode.beats.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        }
    }
}

@Composable
private fun BeatView(beat: StoryBeat, onInteractionComplete: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(220),
        label = "storyAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else transitionStartScale(beat),
        animationSpec = tween(320),
        label = "storyScale",
    )
    val offsetX by animateFloatAsState(
        targetValue = if (entered) 0f else transitionStartX(beat),
        animationSpec = tween(280),
        label = "storyOffsetX",
    )
    val rotation by animateFloatAsState(
        targetValue = if (entered) 0f else transitionStartRotation(beat),
        animationSpec = tween(300),
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(390.dp)
                .graphicsLayer { scaleX = beat.cameraZoom; scaleY = beat.cameraZoom }
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(beat.imageResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(28.dp),
            )
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                drawLine(accent.copy(alpha = 0.65f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = stroke)
                drawLine(accent.copy(alpha = 0.35f), start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = stroke)
            }
        }
        beat.caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f),
            )
        }
    }
}

@Composable
private fun DialogueBeat(beat: StoryBeat.Dialogue) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xE611151B))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = beat.speaker,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor(beat.accent),
        )
        Text(
            text = beat.text,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun FlashBeat(beat: StoryBeat.Flash) {
    Box(
        modifier = Modifier.fillMaxSize().background(accentColor(beat.accent).copy(alpha = 0.9f)),
    )
}

@Composable
private fun MemoryUnlockBeat(beat: StoryBeat.MemoryUnlock) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF0131720))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("MEMORY FRAGMENT RECOVERED", color = accentColor(beat.accent), fontWeight = FontWeight.Bold)
        Text(beat.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(beat.description, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun InteractionBeat(beat: StoryBeat.Interaction, onComplete: () -> Unit) {
    var cleared by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = if (completed) beat.completionText else beat.prompt,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(360.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0B0E13))
                .pointerInput(completed) {
                    if (completed) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val amount = (abs(dragAmount.x) + abs(dragAmount.y)) / 1_350f
                        cleared = (cleared + amount).coerceIn(0f, 1f)
                        if (cleared >= 1f) completed = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (completed) "SIGNAL STABLE" else "///  S E R A P H I  ///",
                color = accentColor(beat.accent).copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
            )
            Canvas(Modifier.fillMaxSize().alpha(1f - cleared * 0.82f)) {
                val lineColor = Color.White.copy(alpha = 0.16f + (1f - cleared) * 0.35f)
                val spacing = size.height / 22f
                repeat(24) { i ->
                    val y = i * spacing
                    val skew = if (i % 2 == 0) 34f else -22f
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, (y + skew).coerceIn(0f, size.height)),
                        strokeWidth = if (i % 3 == 0) 4f else 2f,
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { cleared },
            modifier = Modifier.fillMaxWidth(0.72f),
            color = accentColor(beat.accent),
            trackColor = Color.White.copy(alpha = 0.1f),
        )

        if (completed) {
            Button(onClick = onComplete) { Text("Continue") }
        }
    }
}

@Composable
private fun EndBeat() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("POCKET FAMILIAR", color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(8.dp))
        Text("The screen is quiet again.", color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun KineticBackdrop(accent: StoryAccent) {
    val color = accentColor(accent)
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF05070A))
        val gap = size.width / 9f
        repeat(12) { i ->
            val x = i * gap - size.width * 0.15f
            drawLine(
                color = color.copy(alpha = if (i % 3 == 0) 0.16f else 0.06f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x + size.width * 0.45f, size.height),
                strokeWidth = if (i % 3 == 0) 3f else 1f,
            )
        }
    }
}

private fun beatAccent(beat: StoryBeat?): StoryAccent = when (beat) {
    is StoryBeat.Panel -> beat.accent
    is StoryBeat.Dialogue -> beat.accent
    is StoryBeat.Flash -> beat.accent
    is StoryBeat.Interaction -> beat.accent
    is StoryBeat.MemoryUnlock -> beat.accent
    else -> StoryAccent.CELESTIAL
}

private fun accentColor(accent: StoryAccent): Color = when (accent) {
    StoryAccent.ELECTRIC -> Color(0xFFFFD400)
    StoryAccent.BLOOM -> Color(0xFF28B79A)
    StoryAccent.SCHOLAR -> Color(0xFFB65E86)
    StoryAccent.CELESTIAL -> Color(0xFFE8E4FF)
}

private fun transitionStartScale(beat: StoryBeat): Float = when ((beat as? StoryBeat.Panel)?.transition ?: (beat as? StoryBeat.Dialogue)?.transition) {
    StoryTransition.GLITCH -> 1.08f
    StoryTransition.WHITE_FLASH -> 1.18f
    StoryTransition.PAGE_TURN -> 0.94f
    else -> 0.98f
}

private fun transitionStartX(beat: StoryBeat): Float = when ((beat as? StoryBeat.Panel)?.transition ?: (beat as? StoryBeat.Dialogue)?.transition) {
    StoryTransition.SLIDE_LEFT -> 180f
    StoryTransition.GLITCH -> 26f
    else -> 0f
}

private fun transitionStartRotation(beat: StoryBeat): Float = when ((beat as? StoryBeat.Panel)?.transition ?: (beat as? StoryBeat.Dialogue)?.transition) {
    StoryTransition.GLITCH -> -2.4f
    StoryTransition.PAGE_TURN -> 3.5f
    else -> 0f
}
