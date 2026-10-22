package com.pinhoquest.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val fontScale: Float = 1.0f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.18f,
)

class AppPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<AppPreferences> = dataStore.data.map { values ->
        AppPreferences(
            theme = values[THEME_KEY]
                ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
                ?: ThemePreference.SYSTEM,
            fontScale = values[FONT_SCALE_KEY] ?: 1.0f,
            musicEnabled = values[MUSIC_ENABLED_KEY] ?: true,
            musicVolume = (values[MUSIC_VOLUME_KEY] ?: 0.18f).coerceIn(0f, 0.35f),
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setFontScale(fontScale: Float) {
        require(fontScale.isFinite() && fontScale > 0f) { "Font scale must be positive and finite" }
        dataStore.edit { it[FONT_SCALE_KEY] = fontScale }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { it[MUSIC_ENABLED_KEY] = enabled }
    }

    suspend fun setMusicVolume(volume: Float) {
        require(volume.isFinite() && volume in 0f..0.35f) { "Music volume must be between 0 and 0.35" }
        dataStore.edit { it[MUSIC_VOLUME_KEY] = volume }
    }

    suspend fun write(preferences: AppPreferences) {
        require(preferences.fontScale.isFinite() && preferences.fontScale > 0f) {
            "Font scale must be positive and finite"
        }
        require(preferences.musicVolume.isFinite() && preferences.musicVolume in 0f..0.35f) {
            "Music volume must be between 0 and 0.35"
        }
        dataStore.edit {
            it[THEME_KEY] = preferences.theme.name
            it[FONT_SCALE_KEY] = preferences.fontScale
            it[MUSIC_ENABLED_KEY] = preferences.musicEnabled
            it[MUSIC_VOLUME_KEY] = preferences.musicVolume
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        val MUSIC_ENABLED_KEY = booleanPreferencesKey("music_enabled")
        val MUSIC_VOLUME_KEY = floatPreferencesKey("music_volume")
    }
}
