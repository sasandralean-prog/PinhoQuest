package com.pinhoquest.core.quest

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import com.pinhoquest.domain.quest.QuestSessionFilters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestPlannerTest {
    @Test
    fun twentyRandomPlansWithEmptyHistoryReachMultipleCategories() {
        var choice = 0
        val planner = QuestPlanner(QuestChoiceSource { bound ->
            val selected = choice % bound
            choice += 1
            selected
        })

        val categories = (0 until 20).map {
            planner.plan(QuestRequest(QuestMode.RANDOM), QuestContext()).selectedCategory
        }

        assertTrue(categories.toSet().size > 1)
        assertTrue(QuestCategory.CODING in categories)
        assertTrue(QuestCategory.CREATIVE in categories)
        assertTrue(categories.none { it == QuestCategory.GAMING })
    }

    @Test
    fun randomPrefersCategoriesMissingFromRecentHistory() {
        val planner = QuestPlanner(QuestChoiceSource { 0 })
        val plan = planner.plan(
            QuestRequest(QuestMode.RANDOM),
            QuestContext(
                recentCategories = listOf(QuestCategory.CODING, QuestCategory.CREATIVE),
            ),
        )

        assertEquals(QuestCategory.EXPLORATION, plan.selectedCategory)
    }

    @Test
    fun randomCanRotateWhenEveryCandidateIsInRecentHistory() {
        var choice = 0
        val planner = QuestPlanner(QuestChoiceSource { bound ->
            val selected = choice % bound
            choice += 1
            selected
        })
        val allNonGameCategories = listOf(
            QuestCategory.CODING,
            QuestCategory.CREATIVE,
            QuestCategory.EXPLORATION,
            QuestCategory.LEARNING,
            QuestCategory.RANDOM,
        )

        val selected = (0 until 10).map {
            planner.plan(
                QuestRequest(QuestMode.RANDOM),
                QuestContext(recentCategories = allNonGameCategories),
            ).selectedCategory
        }

        assertTrue(selected.toSet().size > 1)
        assertTrue(selected.all { it in allNonGameCategories })
    }

    @Test
    fun explicitCategoryFilterRemainsHardConstraint() {
        val planner = QuestPlanner(QuestChoiceSource { bound -> bound - 1 })
        val allowed = setOf(QuestCategory.LEARNING, QuestCategory.RANDOM)

        val plan = planner.plan(
            QuestRequest(
                mode = QuestMode.RANDOM,
                filters = QuestSessionFilters(categories = allowed),
            ),
            QuestContext(),
        )

        assertTrue(plan.selectedCategory in allowed)
    }

    @Test
    fun proceduralComposerHasThreeDistinctCodingAngles() = runTest {
        val titles = (0..2).map { variant ->
            val planner = QuestPlanner(QuestChoiceSource { variant.coerceAtMost(it - 1) })
            val plan = planner.plan(
                QuestRequest(QuestMode.NORMAL),
                QuestContext(categoryAffinities = mapOf(QuestCategory.CODING to 1.0)),
            )
            ProceduralComposer().compose(plan).title
        }

        assertEquals(3, titles.toSet().size)
        assertNotEquals(titles[0], titles[1])
        assertNotEquals(titles[1], titles[2])
    }
}
