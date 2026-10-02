package com.pinhoquest.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.MainActivity
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionActivityE2ETest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPersistentState() {
        context.deleteDatabase("pinho-quest.db")
        File(context.filesDir, "datastore/app.preferences_pb").delete()
    }

    @Test
    fun freshInstallCompletesQuestAwardsXpFlowerAndRestoresGarden() {
        var scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Seu nome")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Seu nome").performTextInput("Rafa")
        composeRule.onNodeWithText("Programação").performScrollTo().performClick()
        composeRule.onNodeWithText("Criar meu jardim").performScrollTo().performClick()
        waitFor("O que vamos inventar hoje?")

        composeRule.onNodeWithText("SORTEAR QUEST").performClick()
        waitFor("Frankenstein Digital")
        composeRule.onNodeWithText("COMEÇAR QUEST").performScrollTo().performClick()
        waitFor("Em andamento 🌱")
        composeRule.onNodeWithText("CONCLUIR QUEST").performScrollTo().performClick()

        waitFor("🌱 Quest concluída!")
        composeRule.onNodeWithText("+25 XP").assertIsDisplayed()
        composeRule.onNodeWithText("🌷 Você encontrou uma nova flor!").assertIsDisplayed()
        composeRule.onNodeWithText("Ver no jardim").performClick()

        waitFor("🌷 Jardim de Rafa")
        composeRule.onNodeWithText("25 XP").assertIsDisplayed()
        composeRule.onNodeWithText("Nível 1").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 24 flores").assertIsDisplayed()

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitFor("Oi, Rafa 🌱")
        composeRule.onNodeWithText("Jardim").performClick()
        waitFor("🌷 Jardim de Rafa")
        composeRule.onNodeWithText("25 XP").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 24 flores").assertIsDisplayed()
        scenario.close()
    }

    private fun waitFor(text: String) {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
