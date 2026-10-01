package com.pinhoquest.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
    fun freshInstallOnboardsRunsQuestAbandonsAndRestoresProfile() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithText("Programação").performScrollTo().performClick()
        composeRule.onNodeWithText("Criar meu jardim").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("O que vamos inventar hoje?")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Tags").performClick()
        composeRule.onNodeWithText("Programação").assertIsDisplayed()
        composeRule.onNodeWithText("Quests").performClick()
        composeRule.onNodeWithText("SORTEAR QUEST").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Frankenstein Digital")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("COMEÇAR QUEST").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Em andamento 🌱")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Preciso parar").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("SORTEAR QUEST")
                .fetchSemanticsNodes().isNotEmpty()
        }

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.onAllNodesWithText("Seu nome").assertCountEquals(0)
        composeRule.onNodeWithText("Oi, Rafa 🌱").assertIsDisplayed()
        composeRule.onNodeWithText("SORTEAR QUEST").assertIsDisplayed()
        scenario.close()

        assertTrue(context.getDatabasePath("pinho-quest.db").exists())
    }
}
