package com.mikazuki.pocketfamiliar.story.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import kotlin.math.abs

/**
 * Episode 0 plays the approved manga storyboard art directly.
 *
 * The bundled storyboard panels are the source of truth instead of recreating
 * them from avatar placeholders. Panel 10 remains interactive, panel 16 is the
 * generated Memory Fragment reward card, and every bitmap load is guarded so a
 * damaged panel shows a fallback rather than crashing story playback.
 */
@Composable
fun StoryboardStoryPlayer(
    episode: StoryEpisode,
    selectedFamiliarId: String,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val perspective = selectedFamiliarId // Episode 0 is canonically Emi's perspective.
    var panel by remember(episode.id) { mutableIntStateOf(1) }
    var staticCleared by remember(episode.id) { mutableStateOf(false) }
    val panelScale = remember(panel) { Animatable(1.045f) }

    LaunchedEffect(panel) {
        panelScale.snapTo(1.045f)
        panelScale.animateTo(1f, animationSpec = tween(1_200))
    }

    BackHandler {
        if (panel > 1) panel-- else onExit()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF05070B))) {
        HalftoneBackdrop()
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("POCKET FAMILIAR", color = Color.White, fontWeight = FontWeight.Black)
                    Text("EPISODE 0 · THE SIGNAL", color = Color(0xFFFFC928), style = MaterialTheme.typography.labelMedium)
                }
                Text("$panel / 18", color = Color.White.copy(alpha = .55f))
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (panel == 16) {
                    MemoryFragmentPanel()
                } else {
                    val resId = storyboardPanelRes(panel)
                    if (panel == 10) {
                        InteractiveStaticPanel(
                            resId = resId,
                            cleared = staticCleared,
                            onClear = { staticCleared = true },
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = panelScale.value
                                scaleY = panelScale.value
                            },
                        )
                    } else {
                        SafeStoryImage(
                            resId = resId,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = panelScale.value
                                scaleY = panelScale.value
                            },
                        )
                    }
                }
            }

            if (panel == 10 && !staticCleared) {
                Text(
                    "Drag across the interference to clear the signal.",
                    color = Color.White.copy(alpha = .78f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (panel > 1) {
                    Button(onClick = { panel-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                }
                Button(
                    onClick = {
                        when {
                            panel == 10 && !staticCleared -> Unit
                            panel < 18 -> panel++
                            else -> onComplete()
                        }
                    },
                    enabled = panel != 10 || staticCleared,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            panel == 18 -> "Return to familiar"
                            panel == 10 && !staticCleared -> "Clear the static"
                            else -> "Continue"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveStaticPanel(
    resId: Int,
    cleared: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var drag by remember(resId) { mutableFloatStateOf(0f) }
    Box(
        modifier.pointerInput(cleared) {
            if (!cleared) detectDragGestures { change, amount ->
                change.consume()
                drag = (drag + abs(amount.x) + abs(amount.y)).coerceAtMost(1_800f)
                if (drag >= 1_250f) onClear()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        SafeStoryImage(resId, Modifier.fillMaxSize())
        if (!cleared) {
            Canvas(Modifier.fillMaxSize()) {
                val alpha = (.52f * (1f - drag / 1_800f)).coerceAtLeast(.10f)
                repeat(32) { i ->
                    val y = i * size.height / 32f
                    val shift = if (i % 2 == 0) 34f else -28f
                    drawLine(
                        Color.White.copy(alpha = alpha),
                        androidx.compose.ui.geometry.Offset(0f, y),
                        androidx.compose.ui.geometry.Offset(size.width, y + shift),
                        strokeWidth = if (i % 5 == 0) 6f else 2f,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryFragmentPanel() {
    Box(Modifier.fillMaxSize().background(Color(0xFF080B12)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(80) { i ->
                val x = (i * 83f) % size.width
                val y = (i * 137f) % size.height
                drawCircle(
                    if (i % 3 == 0) Color(0xFFFFC928).copy(alpha = .18f) else Color(0xFF8A75E8).copy(alpha = .16f),
                    if (i % 7 == 0) 5f else 2f,
                    androidx.compose.ui.geometry.Offset(x, y),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SIGNAL FRAGMENT 00", color = Color(0xFFFFC928), fontWeight = FontWeight.Black)
            Text("RECOVERED", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "A voice behind the screen called through a broken halo.",
                color = Color.White.copy(alpha = .70f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(.75f),
            )
        }
    }
}

@Composable
private fun SafeStoryImage(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(resId) {
        runCatching { BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier.background(Color(0xFF171923)), contentAlignment = Alignment.Center) {
            Text("ART SIGNAL LOST", color = Color.White.copy(alpha = .45f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun storyboardPanelRes(panel: Int): Int = when (panel) {
    1 -> R.drawable.story_ep0_01
    2 -> R.drawable.story_ep0_02
    3 -> R.drawable.story_ep0_03
    4 -> R.drawable.story_ep0_04
    5 -> R.drawable.story_ep0_05
    6 -> R.drawable.story_ep0_06
    7 -> R.drawable.story_ep0_07
    8 -> R.drawable.story_ep0_08
    9 -> R.drawable.story_ep0_09
    10 -> R.drawable.story_ep0_10
    11 -> R.drawable.story_ep0_11
    12 -> R.drawable.story_ep0_12
    13 -> R.drawable.story_ep0_13
    14 -> R.drawable.story_ep0_14
    15 -> R.drawable.story_ep0_15
    17 -> R.drawable.story_ep0_17
    18 -> R.drawable.story_ep0_18
    else -> R.drawable.story_ep0_01
}

@Composable
private fun HalftoneBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF05070B))
        repeat(60) { i ->
            val x = (i * 79f) % size.width
            val y = (i * 131f) % size.height
            val color = if (i % 4 == 0) Color(0xFFFFC928) else Color(0xFF6F63D9)
            drawCircle(color.copy(alpha = .07f), if (i % 5 == 0) 4f else 2f, androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}
