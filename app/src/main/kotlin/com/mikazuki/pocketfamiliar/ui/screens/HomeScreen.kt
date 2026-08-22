package com.mikazuki.pocketfamiliar.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.model.BatteryMood
import com.mikazuki.pocketfamiliar.model.PetRegistry

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val battery by vm.batteryState.collectAsStateWithLifecycle()
    val hasPermission by vm.hasOverlayPermission.collectAsStateWithLifecycle()
    val supportsForms = settings.selectedPetId in setOf("emi", "kaelani", "mira")
    val runtimeProfile = PetRegistry.getRuntimeProfile(settings.selectedPetId, supportsForms && settings.useFamiliarForm)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshPermissionsAndProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Choose Your Familiar", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PetRegistry.all.forEach { profile ->
                val isSelected = settings.selectedPetId == profile.id
                OutlinedCard(
                    modifier = Modifier.width(154.dp).clickable { vm.setSelectedPetId(profile.id) },
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(profile.previewResId),
                                contentDescription = profile.displayName,
                                modifier = Modifier.size(60.dp),
                            )
                        }
                        Text(profile.displayName, style = MaterialTheme.typography.labelLarge)
                        Text(profile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Character Form", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(runtimeProfile.previewResId),
                            contentDescription = runtimeProfile.displayName,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (settings.useFamiliarForm && supportsForms) "Familiar form" else "Attendant form", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (supportsForms) "Manual selection only. It never changes based on time of day."
                            else "This character currently has one runtime form.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = supportsForms && settings.useFamiliarForm,
                        onCheckedChange = vm::setUseFamiliarForm,
                        enabled = supportsForms,
                    )
                }
            }
        }

        FamiliarProgressPanel(vm)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(imageVector = if (hasPermission) Icons.Default.Check else Icons.Default.Error, contentDescription = null)
                Text(
                    text = if (hasPermission) stringResource(R.string.label_overlay_permission_granted)
                    else stringResource(R.string.label_overlay_permission_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!hasPermission) {
                    FilledTonalButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    }) { Text(stringResource(R.string.btn_grant_permission)) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::startPet, enabled = hasPermission, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.btn_start_pet))
            }
            OutlinedButton(onClick = vm::stopPet, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.btn_stop_pet))
            }
        }

        HorizontalDivider()

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pet Settings", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                SettingSlider(
                    label = stringResource(R.string.label_pet_size),
                    value = settings.petSize,
                    valueRange = 0.5f..2.0f,
                    formatLabel = { "×${"%.1f".format(it)}" },
                    onValueChangeFinished = vm::setPetSize,
                )
                SettingSlider(
                    label = stringResource(R.string.label_movement_speed),
                    value = settings.movementSpeed,
                    valueRange = 30f..200f,
                    formatLabel = { "${it.toInt()} px/s" },
                    onValueChangeFinished = vm::setMovementSpeed,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingToggle(stringResource(R.string.label_sleep_behavior), settings.sleepEnabled, vm::setSleepEnabled)
                SettingToggle(stringResource(R.string.label_auto_start), settings.autoStartOnBoot, vm::setAutoStartOnBoot)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Character Lab", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Force any runtime state immediately to check frame slicing, scale, familiar-form motion and reactions without waiting for autonomous behavior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("IDLE", "WALK", "RUN", "JUMP", "FALL", "HELD", "SLEEP", "HAPPY", "SPECIAL", "GROOM", "EAT").forEach { state ->
                        OutlinedButton(onClick = { vm.debugState(state) }, enabled = hasPermission) {
                            Text(state.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (battery.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = batteryIconTint(battery.mood),
                )
                Column {
                    Text(stringResource(R.string.label_battery), style = MaterialTheme.typography.labelLarge)
                    val chargingLabel = if (battery.isCharging) " · Charging" else ""
                    Text(
                        "${battery.levelPercent}% · ${batteryMoodLabel(battery.mood)}$chargingLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Beta Target", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("✓ Stable overlay and decoder fallbacks", style = MaterialTheme.typography.bodySmall)
                Text("✓ EMK 4×4 attendant animation mapping", style = MaterialTheme.typography.bodySmall)
                Text("✓ Manual, animated familiar forms", style = MaterialTheme.typography.bodySmall)
                Text("✓ Character-specific autonomous behavior and touch reactions", style = MaterialTheme.typography.bodySmall)
                Text("✓ Persistent Bond / Play XP, Charms, gifts, themes and achievements", style = MaterialTheme.typography.bodySmall)
                Text("Next gate: device soak test with zero crashes and clean sprite cropping.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formatLabel: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderPos by rememberSaveable { mutableFloatStateOf(value) }
    val displayText = remember(sliderPos) { formatLabel(sliderPos) }
    val prevValue = remember { mutableFloatStateOf(value) }
    if (prevValue.floatValue != value) {
        prevValue.floatValue = value
        sliderPos = value
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(displayText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = sliderPos,
            onValueChange = { sliderPos = it },
            valueRange = valueRange,
            onValueChangeFinished = { onValueChangeFinished(sliderPos) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun batteryMoodLabel(mood: BatteryMood) = when (mood) {
    BatteryMood.HAPPY -> "Happy"
    BatteryMood.NORMAL -> "Normal"
    BatteryMood.TIRED -> "Tired"
    BatteryMood.SLEEPY -> "Sleepy"
    BatteryMood.CHARGING -> "Charging"
}

@Composable
private fun batteryIconTint(mood: BatteryMood) = when (mood) {
    BatteryMood.HAPPY -> MaterialTheme.colorScheme.primary
    BatteryMood.CHARGING -> MaterialTheme.colorScheme.tertiary
    BatteryMood.TIRED, BatteryMood.SLEEPY -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}
