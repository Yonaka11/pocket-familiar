package com.mikazuki.pocketfamiliar.story.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import kotlin.math.abs

/**
 * Episode 0 presentation pass based on the approved three-page storyboard.
 * It deliberately uses crash-safe bitmap decoding rather than painterResource:
 * a missing/corrupt panel falls back to halftone art instead of killing the app.
 */
@Composable
fun StoryboardStoryPlayer(
    episode: StoryEpisode,
    selectedFamiliarId: String,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    var page by remember(episode.id) { mutableIntStateOf(0) }
    var staticCleared by remember { mutableStateOf(false) }
    val cast = storyCast(selectedFamiliarId)

    BackHandler {
        if (page > 0) page-- else onExit()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF07090D))) {
        HalftoneBackdrop(cast.accent)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("POCKET FAMILIAR", color = Color.White, fontWeight = FontWeight.Black)
                    Text("EPISODE 0 · THE SIGNAL", color = cast.accent, style = MaterialTheme.typography.labelMedium)
                }
                Text("${page + 1} / 3", color = Color.White.copy(alpha = .55f))
            }

            when (page) {
                0 -> OpeningPage(cast)
                1 -> ContactPage(cast, staticCleared) { staticCleared = true }
                else -> AftermathPage(cast)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    Text(if (page == 2) "Return to familiar" else if (page == 1 && !staticCleared) "Clear the static" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun OpeningPage(cast: StoryCast) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MangaPanel(Modifier.weight(.9f), cast.accent) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SafeStoryImage(cast.avatarRes, Modifier.weight(1f))
                Column(Modifier.weight(1.5f)) {
                    Text("Pocket Familiar · ACTIVE", color = cast.accent, fontWeight = FontWeight.Bold)
                    Text("Your familiar stops mid-motion.", color = Color.White.copy(alpha=.75f))
                }
            }
        }
        MangaPanel(Modifier.weight(.8f), cast.accent) {
            SpeechBubble("...Wait.")
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
        }
        MangaPanel(Modifier.weight(1.15f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble(cast.warning)
        }
        MangaPanel(Modifier.weight(.7f), cast.accent) {
            Text("THE INTERFACE TEARS OPEN", color = cast.accent, fontWeight = FontWeight.Black)
            Canvas(Modifier.fillMaxSize()) {
                repeat(16) { i ->
                    val y = size.height * i / 16f
                    drawLine(cast.accent.copy(alpha=.35f), androidx.compose.ui.geometry.Offset(0f,y), androidx.compose.ui.geometry.Offset(size.width,y-30f), strokeWidth=if(i%3==0) 6f else 2f)
                }
            }
        }
        MangaPanel(Modifier.weight(.95f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble("Did you see that?")
        }
    }
}

@Composable
private fun ContactPage(cast: StoryCast, cleared: Boolean, onClear: () -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MangaPanel(Modifier.weight(.72f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble("No...")
        }
        MangaPanel(Modifier.weight(1.15f), Color(0xFF9B82E8)) {
            SafeStoryImage(R.drawable.seraphi_launcher_foreground, Modifier.fillMaxSize())
            Text("SIGNAL // FRAGMENTED", color = Color.White.copy(alpha=.75f), modifier = Modifier.align(Alignment.BottomStart))
        }
        MangaPanel(Modifier.weight(.8f), Color(0xFFB8A6FF)) {
            SafeStoryImage(R.drawable.seraphi_launcher_foreground, Modifier.fillMaxSize())
            SpeechBubble("Find me.")
        }
        MangaPanel(
            Modifier.weight(1f).pointerInput(cleared) {
                if (!cleared) detectDragGestures { change, amount ->
                    change.consume()
                    drag = (drag + abs(amount.x) + abs(amount.y)).coerceAtMost(1800f)
                    if (drag >= 1400f) onClear()
                }
            },
            cast.accent,
        ) {
            Text(if (cleared) "SIGNAL STABLE" else "DRAG ACROSS THE STATIC", color = Color.White, fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxSize()) {
                val a = if (cleared) .08f else (.48f * (1f - drag / 1800f)).coerceAtLeast(.10f)
                repeat(28) { i ->
                    val y = i * size.height / 28f
                    drawLine(Color.White.copy(alpha=a), androidx.compose.ui.geometry.Offset(0f,y), androidx.compose.ui.geometry.Offset(size.width,y + if(i%2==0) 42f else -24f), strokeWidth=if(i%4==0) 5f else 2f)
                }
            }
        }
        MangaPanel(Modifier.weight(.8f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble("Hold on!")
        }
    }
}

@Composable
private fun AftermathPage(cast: StoryCast) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MangaPanel(Modifier.weight(1f), Color(0xFF9B82E8)) {
            SafeStoryImage(R.drawable.seraphi_launcher_foreground, Modifier.fillMaxSize())
            Text("The static clears. The figure breaks into light.", color = Color.White, modifier = Modifier.align(Alignment.BottomStart))
        }
        MangaPanel(Modifier.weight(.9f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble(cast.aftermath)
        }
        MangaPanel(Modifier.weight(.72f), cast.accent) {
            Text("SIGNAL FRAGMENT 00", color = cast.accent, fontWeight = FontWeight.Black)
            Text("RECOVERED", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        }
        MangaPanel(Modifier.weight(.9f), cast.accent) {
            SafeStoryImage(cast.avatarRes, Modifier.fillMaxSize())
            SpeechBubble("Did you hear that too?")
        }
        MangaPanel(Modifier.weight(1.05f), Color(0xFF9B82E8)) {
            SafeStoryImage(R.drawable.seraphi_launcher_foreground, Modifier.fillMaxSize())
            Text("Everything looks normal... until it isn't.", color = Color.White, fontStyle = FontStyle.Italic, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun MangaPanel(modifier: Modifier, accent: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10131A))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 16f
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    if (((x + y) / step).toInt() % 3 == 0) drawCircle(accent.copy(alpha=.08f), 1.8f, androidx.compose.ui.geometry.Offset(x,y))
                    x += step
                }
                y += step
            }
        }
        content()
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(.58f)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(text, color = Color.Black, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SafeStoryImage(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(resId) {
        runCatching { BitmapFactory.decodeResource(context.resources, resId)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier.background(Color(0xFF171923)), contentAlignment = Alignment.Center) {
            Text("ART SIGNAL LOST", color = Color.White.copy(alpha=.45f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun HalftoneBackdrop(accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF07090D))
        repeat(50) { i ->
            val x = (i * 79f) % size.width
            val y = (i * 131f) % size.height
            drawCircle(accent.copy(alpha=.08f), if (i % 5 == 0) 4f else 2f, androidx.compose.ui.geometry.Offset(x,y))
        }
    }
}

private data class StoryCast(
    val name: String,
    val avatarRes: Int,
    val accent: Color,
    val warning: String,
    val aftermath: String,
)

private fun storyCast(id: String): StoryCast = when (id) {
    "kaelani" -> StoryCast("Kaelani", R.drawable.kaelani_avatar, Color(0xFF39C6A6), "Something is blooming where nothing should be.", "I remember that light... but not where I saw it.")
    "mira" -> StoryCast("Mira", R.drawable.mira_avatar, Color(0xFFC46B8C), "No. That line wasn't there before.", "I know that pattern. I don't remember learning it.")
    else -> StoryCast("Emi", R.drawable.emi_avatar, Color(0xFFFFD400), "Don't touch that.", "She was here...")
}
