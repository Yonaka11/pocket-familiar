package com.mikazuki.pocketfamiliar.ui.screens

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.BatteryState
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetSettingsRepository(application)
    private val batteryMonitor = BatteryMonitor(application)

    val settings: StateFlow<PetSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetSettings())

    val batteryState: StateFlow<BatteryState> = batteryMonitor.batteryState

    private val _hasOverlayPermission = MutableStateFlow(checkOverlayPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    init {
        batteryMonitor.register()
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.unregister()
    }

    /**
     * Re-evaluate the overlay permission status.
     * Call this from the UI on every [Lifecycle.Event.ON_RESUME] so the UI
     * stays in sync after the user returns from the System Settings page.
     */
    fun refreshOverlayPermission() {
        _hasOverlayPermission.value = checkOverlayPermission()
    }

    private fun checkOverlayPermission(): Boolean =
        Settings.canDrawOverlays(getApplication())

    fun startPet() {
        val context = getApplication<Application>()
        val intent = Intent(context, PetOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopPet() {
        getApplication<Application>().stopService(
            Intent(getApplication(), PetOverlayService::class.java)
        )
    }

    fun setPetSize(value: Float) = viewModelScope.launch { repository.setPetSize(value) }

    fun setMovementSpeed(value: Float) = viewModelScope.launch { repository.setMovementSpeed(value) }

    fun setSleepEnabled(value: Boolean) = viewModelScope.launch { repository.setSleepEnabled(value) }

    fun setAutoStartOnBoot(value: Boolean) = viewModelScope.launch { repository.setAutoStartOnBoot(value) }
}
