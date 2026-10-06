package com.pinhoquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationActivityE2ETest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPersistentState() {
        context.deleteDatabase("pinho-quest.db")
        File(context.filesDir, "datastore/app.preferences_pb").delete()
    }

    @Test
    fun freshInstallOnboardsRunsQuestAndRestoresProfile() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Começar")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Começar").performClick()
        composeRule.onNodeWithContentDescription("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithContentDescription("Confirmar nome").performClick()
        composeRule.onNodeWithContentDescription("Programação").performClick()
        composeRule.onNodeWithContentDescription("Continuar").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Sortear quest")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Sortear quest").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Começar quest")
                .fetchSemanticsNodes().isNotEmpty()
        }

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Sortear quest")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Perfil").performClick()
        composeRule.onNodeWithContentDescription("Programação").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Abrir configurações").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Início").performClick()
        composeRule.onNodeWithContentDescription("Sortear quest").assertIsDisplayed()

        scenario.close()
        assertTrue(context.getDatabasePath("pinho-quest.db").exists())
    }
}
