package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSessionFilters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestEngineTest {
    @Test
    fun gameWithoutCandidateDoesNotFallBackToNormal() = runTest {
        var composeCalls = 0
        val composer = ComposerPort {
            composeCalls += 1
            error("composer must not be called")
        }
        val engine = QuestEngine(
            planner = QuestPlanner(),
            composer = composer,
            validator = QuestValidator(),
        )

        val result = engine.generate(
            QuestRequest(QuestMode.GAME),
            QuestContext(),
        )

        assertEquals(0, composeCalls)
        assertEquals(
            QuestGenerationResult.Unavailable(GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED),
            result,
        )
    }

    @Test
    fun randomLowersAffinityWeightButPreservesHardFilters() {
        val filters = QuestSessionFilters(
            minMinutes = 15,
            maxMinutes = 30,
            environments = setOf(QuestEnvironment.WINDOWS),
            categories = setOf(QuestCategory.CODING),
        )
        val context = QuestContext(
            categoryAffinities = mapOf(QuestCategory.CODING to 1.0),
        )
        val planner = QuestPlanner()

        val normal = planner.plan(QuestRequest(QuestMode.NORMAL, filters), context)
        val random = planner.plan(QuestRequest(QuestMode.RANDOM, filters), context)

        assertTrue(random.affinityWeight < normal.affinityWeight)
        assertEquals(filters, random.filters)
        assertEquals(QuestCategory.CODING, random.selectedCategory)
        assertEquals(QuestEnvironment.WINDOWS, random.selectedEnvironment)
    }

    @Test
    fun proceduralComposerProducesAValidatedQuest() = runTest {
        val engine = QuestEngine(
            planner = QuestPlanner(),
            composer = ProceduralComposer(),
            validator = QuestValidator(),
        )

        val result = engine.generate(
            QuestRequest(QuestMode.NORMAL),
            QuestContext(
                categoryAffinities = mapOf(QuestCategory.CREATIVE to 0.9),
            ),
        )

        assertTrue(result is QuestGenerationResult.Success)
    }
}
