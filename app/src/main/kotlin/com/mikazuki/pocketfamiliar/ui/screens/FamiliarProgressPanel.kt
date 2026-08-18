package com.mikazuki.pocketfamiliar.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikazuki.pocketfamiliar.model.FamiliarThemeCatalog

@Composable
fun FamiliarProgressPanel(vm: HomeViewModel) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val hasActivityPermission by vm.hasActivityPermission.collectAsStateWithLifecycle()
    val achievementCount by vm.achievementCount.collectAsStateWithLifecycle()
    val giftMessage by vm.giftMessage.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { vm.refreshPermissionsAndProgress() },
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Bond & Play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Level ${progress.level}  •  Bond ${progress.bondXp} XP  •  Play Lv ${progress.playLevel}  •  ${progress.charms} Charms",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Steps ${progress.lifetimeSteps}  •  Best juggle x${progress.bestJuggleCombo}  •  Achievements $achievementCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) {
                    Text("Enable step rewards")
                }
            } else {
                Text(
                    "Walking earns Bond XP and Charms. Preferred activities give a bonus, but every familiar can level through any activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Gifts", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.giftCatalog, key = { it.id }) { gift ->
                    OutlinedButton(
                        onClick = { vm.redeemGift(gift) },
                        modifier = Modifier.widthIn(min = 118.dp),
                    ) {
                        Column {
                            Text(gift.displayName)
                            Text("${gift.costCharms} Charms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            giftMessage?.let { message ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = vm::clearGiftMessage) { Text("OK") }
                }
            }

            Text("Theme Rewards", style = MaterialTheme.typography.titleSmall)
            Text(
                "Unlock screen atmospheres, frames, and auras through Bond, Familiar levels, and achievements.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.themeCatalog, key = { it.id }) { theme ->
                    val unlocked = FamiliarThemeCatalog.isUnlocked(theme, progress, achievementCount)
                    val selected = settings.selectedThemeId == theme.id
                    OutlinedButton(
                        onClick = { vm.selectTheme(theme) },
                        enabled = unlocked,
                        modifier = Modifier.widthIn(min = 148.dp),
                    ) {
                        Column {
                            Text(if (selected) "✓ ${theme.displayName}" else theme.displayName)
                            Text(
                                if (unlocked) theme.category.name.lowercase().replaceFirstChar { it.uppercase() }
                                else "Unlock: ${FamiliarThemeCatalog.unlockLabel(theme)}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
