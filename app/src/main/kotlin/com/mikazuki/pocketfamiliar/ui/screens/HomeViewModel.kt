package com.mikazuki.pocketfamiliar.ui.screens

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mikazuki.pocketfamiliar.data.FamiliarGameplayRepository
import com.mikazuki.pocketfamiliar.data.FamiliarProgressRepository
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import com.mikazuki.pocketfamiliar.model.BatteryState
import com.mikazuki.pocketfamiliar.model.FamiliarGift
import com.mikazuki.pocketfamiliar.model.FamiliarGiftCatalog
import com.mikazuki.pocketfamiliar.model.FamiliarProgress
import com.mikazuki.pocketfamiliar.model.FamiliarReward
import com.mikazuki.pocketfamiliar.model.FamiliarRewardEngine
import com.mikazuki.pocketfamiliar.model.FamiliarTheme
import com.mikazuki.pocketfamiliar.model.FamiliarThemeCatalog
import com.mikazuki.pocketfamiliar.model.PetRegistry
import com.mikazuki.pocketfamiliar.model.PetSettings
import com.mikazuki.pocketfamiliar.service.ACTION_DEBUG_STATE
import com.mikazuki.pocketfamiliar.service.EXTRA_DEBUG_STATE
import com.mikazuki.pocketfamiliar.service.PetOverlayService
import com.mikazuki.pocketfamiliar.story.data.StoryProgressRepository
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import com.mikazuki.pocketfamiliar.story.model.StoryProgress
import com.mikazuki.pocketfamiliar.util.BatteryMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PetSettingsRepository(application)
    private val progressRepository = FamiliarProgressRepository(application)
    private val gameplayRepository = FamiliarGameplayRepository(application)
    private val storyRepository = StoryProgressRepository(application)
    private val batteryMonitor = BatteryMonitor(application)

    val settings: StateFlow<PetSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetSettings())

    val storyProgress: StateFlow<StoryProgress> = storyRepository.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoryProgress())

    val batteryState: StateFlow<BatteryState> = batteryMonitor.batteryState
    val giftCatalog: List<FamiliarGift> = FamiliarGiftCatalog.all
    val themeCatalog: List<FamiliarTheme> = FamiliarThemeCatalog.all

    private val _hasOverlayPermission = MutableStateFlow(checkOverlayPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasActivityPermission = MutableStateFlow(checkActivityPermission())
    val hasActivityPermission: StateFlow<Boolean> = _hasActivityPermission.asStateFlow()

    private val _progress = MutableStateFlow(progressRepository.load(PetSettings().selectedPetId))
    val progress: StateFlow<FamiliarProgress> = _progress.asStateFlow()

    private val _achievementCount = MutableStateFlow(0)
    val achievementCount: StateFlow<Int> = _achievementCount.asStateFlow()

    private val _giftMessage = MutableStateFlow<String?>(null)
    val giftMessage: StateFlow<String?> = _giftMessage.asStateFlow()

    private val _storyMessage = MutableStateFlow<String?>(null)
    val storyMessage: StateFlow<String?> = _storyMessage.asStateFlow()

    init {
        batteryMonitor.register()
        viewModelScope.launch {
            repository.settingsFlow.collect { value -> refreshProgress(value.selectedPetId) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.unregister()
    }

    fun refreshPermissionsAndProgress() {
        _hasOverlayPermission.value = checkOverlayPermission()
        _hasActivityPermission.value = checkActivityPermission()
        refreshProgress(settings.value.selectedPetId)
    }

    fun refreshOverlayPermission() = refreshPermissionsAndProgress()

    private fun checkOverlayPermission(): Boolean = Settings.canDrawOverlays(getApplication())

    private fun checkActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            getApplication<Application>().checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun startPet() = sendServiceIntent(Intent(getApplication(), PetOverlayService::class.java))

    fun stopPet() {
        getApplication<Application>().stopService(Intent(getApplication(), PetOverlayService::class.java))
    }

    fun debugState(stateName: String) {
        val intent = Intent(getApplication(), PetOverlayService::class.java).apply {
            action = ACTION_DEBUG_STATE
            putExtra(EXTRA_DEBUG_STATE, stateName)
        }
        sendServiceIntent(intent)
    }

    private fun sendServiceIntent(intent: Intent) {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    fun completeStoryEpisode(episode: StoryEpisode) {
        viewModelScope.launch {
            val firstCompletion = storyRepository.completeEpisode(episode.id, episode.memoryRewardId)
            if (firstCompletion) {
                val id = settings.value.selectedPetId
                _progress.value = progressRepository.addReward(
                    id,
                    FamiliarReward(bondXp = 25, charms = 10),
                )
                _storyMessage.value = "${episode.memoryRewardTitle} recovered · +25 Bond XP · +10 Charms"
            } else {
                _storyMessage.value = "${episode.title} replay complete."
            }
        }
    }

    fun redeemGift(gift: FamiliarGift) {
        val id = settings.value.selectedPetId
        val profile = PetRegistry.getById(id)
        val current = progressRepository.load(id)
        if (current.charms < gift.costCharms) {
            _giftMessage.value = "Need ${gift.costCharms - current.charms} more Charms for ${gift.displayName}."
            return
        }

        progressRepository.addReward(id, FamiliarReward(charms = -gift.costCharms))
        val reward = FamiliarRewardEngine.rewardForGift(gift, profile.preferences)
        _progress.value = progressRepository.addReward(id, reward)
        gameplayRepository.addGift(id)
        _giftMessage.value = if (reward.preferenceBonusApplied) {
            "${profile.displayName} loves ${gift.displayName}! Bonus Bond XP."
        } else {
            "${profile.displayName} enjoyed ${gift.displayName}."
        }
    }

    fun selectTheme(theme: FamiliarTheme) {
        if (!FamiliarThemeCatalog.isUnlocked(theme, _progress.value, _achievementCount.value)) return
        viewModelScope.launch { repository.setSelectedThemeId(theme.id) }
    }

    fun setDebugThemeEnabled(themeId: String, enabled: Boolean) = viewModelScope.launch {
        repository.setDebugThemeEnabled(themeId, enabled)
    }

    fun clearDebugThemes() = viewModelScope.launch { repository.clearDebugThemes() }
    fun clearGiftMessage() { _giftMessage.value = null }
    fun clearStoryMessage() { _storyMessage.value = null }

    private fun refreshProgress(id: String) {
        _progress.value = progressRepository.load(id)
        _achievementCount.value = gameplayRepository.achievementIds(id).size
    }

    fun setPetSize(value: Float) = viewModelScope.launch { repository.setPetSize(value) }
    fun setMovementSpeed(value: Float) = viewModelScope.launch { repository.setMovementSpeed(value) }
    fun setSleepEnabled(value: Boolean) = viewModelScope.launch { repository.setSleepEnabled(value) }
    fun setAutoStartOnBoot(value: Boolean) = viewModelScope.launch { repository.setAutoStartOnBoot(value) }
    fun setSelectedPetId(id: String) = viewModelScope.launch { repository.setSelectedPetId(id) }
    fun setUseFamiliarForm(value: Boolean) = viewModelScope.launch { repository.setUseFamiliarForm(value) }
}
