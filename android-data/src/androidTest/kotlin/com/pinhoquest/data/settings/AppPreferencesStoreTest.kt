package com.pinhoquest.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPreferencesStoreTest {
    @Test
    fun themeAndFontScaleRoundTrip() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        val file = File(context.filesDir, "prefs-${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        val store = AppPreferencesStore(dataStore)

        store.setTheme(ThemePreference.DARK)
        store.setFontScale(1.25f)

        val snapshot = store.preferences.first()
        assertEquals(ThemePreference.DARK, snapshot.theme)
        assertEquals(1.25f, snapshot.fontScale)

        scope.cancel()
        file.delete()
        Unit
    }
}
