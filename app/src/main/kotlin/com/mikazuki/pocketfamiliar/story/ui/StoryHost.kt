package com.mikazuki.pocketfamiliar.story.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikazuki.pocketfamiliar.story.data.StoryCatalog
import com.mikazuki.pocketfamiliar.ui.screens.HomeScreen
import com.mikazuki.pocketfamiliar.ui.screens.HomeViewModel

/** Root story surface. Story playback is isolated from the overlay service. */
@Composable
fun PocketFamiliarStoryHost(vm: HomeViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val storyProgress by vm.storyProgress.collectAsStateWithLifecycle()
    val storyMessage by vm.storyMessage.collectAsStateWithLifecycle()
    val hasOverlayPermission by vm.hasOverlayPermission.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var storyActive by rememberSaveable { mutableStateOf(false) }
    val episode = remember(settings.selectedPetId) { StoryCatalog.signalEpisode(settings.selectedPetId) }

    LaunchedEffect(storyMessage) {
        val message = storyMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.clearStoryMessage()
    }

    if (storyActive) {
        StoryboardStoryPlayer(
            episode = episode,
            selectedFamiliarId = settings.selectedPetId,
            onComplete = {
                vm.completeStoryEpisode(episode)
                if (hasOverlayPermission && settings.selectedPetId == episode.focusFamiliarId) {
                    runCatching { vm.debugState("SPECIAL") }
                }
                storyActive = false
            },
            onExit = { storyActive = false },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(vm)
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp))
        ExtendedFloatingActionButton(
            onClick = { storyActive = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            icon = { Icon(Icons.Default.AutoStories, contentDescription = null) },
            text = { Text(if (storyProgress.hasCompleted(episode.id)) "Replay · The Signal" else "Story · The Signal") },
        )
    }
}
