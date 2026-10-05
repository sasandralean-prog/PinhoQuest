package com.pinhoquest.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppPreferencesStoreTest {
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var store: AppPreferencesStore

    @Before
    fun setUp() {
        scope = CoroutineScope(Job() + Dispatchers.IO)
        file = File.createTempFile("pinhoquest-preferences-${UUID.randomUUID()}", ".preferences_pb")
        store = AppPreferencesStore(
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            ),
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun defaultsAreSystemThemeAndNormalText() = runBlocking {
        val preferences = store.preferences.first()
        assertEquals(ThemePreference.SYSTEM, preferences.theme)
        assertEquals(1.0f, preferences.fontScale)
    }

    @Test
    fun themeAndFontScalePersistTogether() = runBlocking {
        store.write(AppPreferences(ThemePreference.DARK, 1.15f))
        val preferences = store.preferences.first()
        assertEquals(ThemePreference.DARK, preferences.theme)
        assertEquals(1.15f, preferences.fontScale)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroFontScaleIsRejected() = runBlocking {
        store.setFontScale(0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeFontScaleIsRejected() = runBlocking {
        store.write(AppPreferences(ThemePreference.LIGHT, -1f))
    }
}
