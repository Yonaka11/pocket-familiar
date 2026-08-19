package com.mikazuki.pocketfamiliar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mikazuki.pocketfamiliar.model.FamiliarThemeCatalog
import com.mikazuki.pocketfamiliar.model.PetSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pet_settings")

/**
 * Single source of truth for user-controlled settings, backed by DataStore.
 *
 * All writes are suspending; reads are exposed as [Flow] for reactive updates.
 */
class PetSettingsRepository(private val context: Context) {

    private object Keys {
        val PET_SIZE = floatPreferencesKey("pet_size")
        val MOVEMENT_SPEED = floatPreferencesKey("movement_speed")
        val SLEEP_ENABLED = booleanPreferencesKey("sleep_enabled")
        val AUTO_START = booleanPreferencesKey("auto_start_on_boot")
        val SELECTED_PET_ID = stringPreferencesKey("selected_pet_id")
        val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        val DEBUG_THEME_IDS = stringSetPreferencesKey("debug_theme_ids")
    }

    val settingsFlow: Flow<PetSettings> = context.dataStore.data.map { prefs ->
        PetSettings(
            petSize = prefs[Keys.PET_SIZE] ?: 1.0f,
            movementSpeed = prefs[Keys.MOVEMENT_SPEED] ?: 80f,
            sleepEnabled = prefs[Keys.SLEEP_ENABLED] ?: true,
            autoStartOnBoot = prefs[Keys.AUTO_START] ?: false,
            selectedPetId = prefs[Keys.SELECTED_PET_ID] ?: "default",
            selectedThemeId = prefs[Keys.SELECTED_THEME_ID] ?: FamiliarThemeCatalog.DEFAULT_THEME_ID,
            debugThemeIds = prefs[Keys.DEBUG_THEME_IDS]?.toSet() ?: emptySet(),
        )
    }

    suspend fun setPetSize(value: Float) = context.dataStore.edit { it[Keys.PET_SIZE] = value }

    suspend fun setMovementSpeed(value: Float) = context.dataStore.edit { it[Keys.MOVEMENT_SPEED] = value }

    suspend fun setSleepEnabled(value: Boolean) = context.dataStore.edit { it[Keys.SLEEP_ENABLED] = value }

    suspend fun setAutoStartOnBoot(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_START] = value }

    suspend fun setSelectedPetId(value: String) = context.dataStore.edit { it[Keys.SELECTED_PET_ID] = value }

    suspend fun setSelectedThemeId(value: String) = context.dataStore.edit { it[Keys.SELECTED_THEME_ID] = value }

    suspend fun setDebugThemeEnabled(themeId: String, enabled: Boolean) = context.dataStore.edit { prefs ->
        val current = prefs[Keys.DEBUG_THEME_IDS]?.toMutableSet() ?: mutableSetOf()
        if (enabled) current += themeId else current -= themeId
        prefs[Keys.DEBUG_THEME_IDS] = current
    }

    suspend fun clearDebugThemes() = context.dataStore.edit { prefs ->
        prefs[Keys.DEBUG_THEME_IDS] = emptySet()
    }
}
