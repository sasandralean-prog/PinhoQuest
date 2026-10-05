package com.pinhoquest.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    suspend fun setFontScale(fontScale: Float) {
        require(fontScale > 0f) { "Font scale must be positive" }
        dataStore.edit { it[FONT_SCALE_KEY] = fontScale }
    }

    suspend fun write(preferences: AppPreferences) {
        require(preferences.fontScale > 0f) { "Font scale must be positive" }
        dataStore.edit {
            it[THEME_KEY] = preferences.theme.name
            it[FONT_SCALE_KEY] = preferences.fontScale
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
    }
}
