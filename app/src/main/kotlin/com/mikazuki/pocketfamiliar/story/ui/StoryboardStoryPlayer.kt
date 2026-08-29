package com.mikazuki.pocketfamiliar.story.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import kotlin.math.abs

/**
 * Episode 0 motion-comic player.
 *
 * The approved storyboard panels are the actual presentation now, rather than
 * being packaged but ignored while avatars and placeholder UI recreate them.
 * Panel 10 keeps the interactive static-clearing beat. Panel 16 is intentionally
 * generated in-app because the source pack has no story_ep0_16 asset.
 */
@Composable
fun StoryboardStoryPlayer(
    episode: StoryEpisode,
    selectedFamiliarId: String,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    var page by remember(episode.id) { mutableIntStateOf(0) }
    var staticCleared by remember(episode.id) { mutableStateOf(false) }

    BackHandler {
        if (page > 0) page-- else onExit()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF06080D))) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("POCKET FAMILIAR", color = Color.White, fontWeight = FontWeight.Black)
                    Text("EPISODE 0 · THE SIGNAL", color = Color(0xFFFFD34E), style = MaterialTheme.typography.labelMedium)
                }
                Text("${page + 1} / 3", color = Color.White.copy(alpha = .55f))
            }

            val range = when (page) {
                0 -> 1..6
                1 -> 7..12
                else -> 13..18
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                range.forEach { index ->
                    when (index) {
                        10 -> InteractiveStaticPanel(staticCleared) { staticCleared = true }
                        16 -> MemoryFragmentPanel()
                        else -> StoryPanel(index)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (page > 0) {
                    Button(onClick = { page-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                }
                Button(
                    onClick = {
                        when {
                            page == 1 && !staticCleared -> Unit
                            page < 2 -> page++
                            else -> onComplete()
                        }
                    },
                    enabled = page != 1 || staticCleared,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            page == 1 && !staticCleared -> "Clear the static"
                            page == 2 -> "Return to familiar"
                            else -> "Continue"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryPanel(index: Int) {
    val context = LocalContext.current
    val resId = remember(index) {
        context.resources.getIdentifier(
            "story_ep0_${index.toString().padStart(2, '0')}",
            "drawable",
            context.packageName,
        )
    }
    SafeStoryboardImage(resId, index)
}

@Composable
private fun InteractiveStaticPanel(cleared: Boolean, onClear: () -> Unit) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val resId = remember {
        context.resources.getIdentifier("story_ep0_10", "drawable", context.packageName)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10131A))
            .pointerInput(cleared) {
                if (!cleared) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        dragDistance = (dragDistance + abs(amount.x) + abs(amount.y)).coerceAtMost(1800f)
                        if (dragDistance >= 1200f) onClear()
                    }
                }
            },
    ) {
        SafeStoryboardImage(resId, 10, Modifier.fillMaxSize())
        if (!cleared) {
            Canvas(Modifier.fillMaxSize()) {
                val remaining = (1f - dragDistance / 1800f).coerceIn(.08f, 1f)
                repeat(28) { i ->
                    val y = size.height * i / 28f
                    drawLine(
                        Color.White.copy(alpha = .42f * remaining),
                        androidx.compose.ui.geometry.Offset(0f, y),
                        androidx.compose.ui.geometry.Offset(size.width, y + if (i % 2 == 0) 32f else -20f),
                        strokeWidth = if (i % 4 == 0) 5f else 2f,
                    )
                }
            }
            Text(
                "DRAG TO CLEAR THE STATIC",
                color = Color.White,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

@Composable
private fun MemoryFragmentPanel() {
    Box(
        Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF11131B)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(40) { i ->
                val x = (i * 71f) % size.width
                val y = (i * 109f) % size.height
                drawCircle(Color(0xFFFFD34E).copy(alpha = .13f), if (i % 5 == 0) 4f else 2f, androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SIGNAL FRAGMENT 00", color = Color(0xFFFFD34E), fontWeight = FontWeight.Black)
            Text("RECOVERED", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SafeStoryboardImage(resId: Int, index: Int, modifier: Modifier = Modifier.fillMaxWidth().height(220.dp)) {
    val context = LocalContext.current
    val bitmap = remember(resId) {
        if (resId == 0) null else runCatching {
            context.resources.openRawResource(resId).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF10131A)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Episode 0 panel $index",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text("ART SIGNAL LOST · PANEL $index", color = Color.White.copy(alpha = .5f))
        }
    }
}
