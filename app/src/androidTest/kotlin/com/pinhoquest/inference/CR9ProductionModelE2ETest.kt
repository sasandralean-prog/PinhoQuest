package com.pinhoquest.inference

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pinhoquest.PinhoQuestApplication
import com.pinhoquest.core.inference.micro.AdmissionAwareMicroQuestComposer
import com.pinhoquest.core.inference.micro.MicroQuestComposer
import com.pinhoquest.core.inference.micro.MicroQuestOrigin
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSessionFilters
import com.pinhoquest.data.model.AndroidModelStore
import com.pinhoquest.domain.model.ModelManifest
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CR9ProductionModelE2ETest {
    companion object {
        private const val CR74_SHA256 = "e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb"
        private const val CR74_BYTES = 284692656L
    }
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPersistentStateButKeepInstalledModel() {
        context.deleteDatabase("pinho-quest.db")
        File(context.filesDir, "datastore/app.preferences_pb").delete()
    }

    @Test
    fun productiveUiGeneratesWithCanonicalCr74Model() {
        val active = AndroidModelStore(File(context.filesDir, "models")).active()
        assertNotNull("CR-7.4 model must be active before the productive E2E", active)
        assertEquals(
            "e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb",
            active!!.manifest.sha256,
        )

        val graph = (context.applicationContext as PinhoQuestApplication).graph
        val field = graph.javaClass.getDeclaredField("productionComposer").apply {
            isAccessible = true
        }
        assertTrue(
            "Production graph must use the admission-aware local composer when CR-7.4 is active",
            field.get(graph) is AdmissionAwareMicroQuestComposer,
        )

        ActivityScenario.launch(com.pinhoquest.MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Seu nome")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Seu nome").performTextInput("Rafa")
            composeRule.onNodeWithText("Programação").performScrollTo().performClick()
            composeRule.onNodeWithText("Criar meu jardim").performScrollTo().performClick()

            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("O que vamos inventar hoje?")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("SORTEAR QUEST").performClick()

            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText("COMEÇAR QUEST")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("COMEÇAR QUEST").assertIsDisplayed()

            val localField = field.get(graph).javaClass.getDeclaredField("local").apply {
                isAccessible = true
            }
            val localComposer = localField.get(field.get(graph)) as MicroQuestComposer
            val planner = QuestPlanner()
            val plan = planner.plan(
                QuestRequest(QuestMode.NORMAL, QuestSessionFilters()),
                QuestContext(),
            )
            val rendered = kotlinx.coroutines.runBlocking { localComposer.composeWithDetails(plan) }
            assertEquals(MicroQuestOrigin.LOCAL_MODEL, rendered.origin)
            assertTrue(rendered.draft.title.isNotBlank())
            assertTrue(rendered.draft.description.isNotBlank())
            assertTrue(rendered.draft.objectives.isNotEmpty())
        }
    }
}
