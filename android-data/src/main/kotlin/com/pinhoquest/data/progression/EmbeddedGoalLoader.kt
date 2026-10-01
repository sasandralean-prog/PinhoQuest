package com.pinhoquest.data.progression

import com.pinhoquest.domain.progression.GoalDefinition
import com.pinhoquest.domain.progression.GoalId
import com.pinhoquest.domain.progression.GoalRule
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

data class EmbeddedGoalSet(
    val version: Int,
    val goals: List<GoalDefinition>,
)

object EmbeddedGoalLoader {
    private const val RESOURCE_PATH = "goals/v1_goals.json"

    fun loadV1(): EmbeddedGoalSet {
        val raw = requireNotNull(
            EmbeddedGoalLoader::class.java.classLoader?.getResourceAsStream(RESOURCE_PATH),
        ) { "Missing embedded goal resource: $RESOURCE_PATH" }
            .bufferedReader()
            .use { it.readText() }
        val root = Json.parseToJsonElement(raw).jsonObject
        val version = root.requiredInt("version")
        val goals = root.requiredArray("goals").map { element ->
            parseGoal(element.jsonObject)
        }
        require(goals.map { it.id }.distinct().size == goals.size) { "goal ids must be unique" }
        return EmbeddedGoalSet(version, goals)
    }

    private fun parseGoal(obj: JsonObject): GoalDefinition {
        val target = obj.requiredInt("target")
        val rule = when (obj.requiredString("ruleType")) {
            "COMPLETION_COUNT" -> GoalRule.CompletionCount(target)
            "CATEGORY_COMPLETION" -> GoalRule.CategoryCompletion(
                category = QuestCategory.valueOf(obj.requiredString("category")),
                target = target,
            )
            "DIFFICULTY_COMPLETION" -> GoalRule.DifficultyCompletion(
                difficulty = QuestDifficulty.valueOf(obj.requiredString("difficulty")),
                target = target,
            )
            "BONUS_OBJECTIVE_COUNT" -> GoalRule.BonusObjectiveCount(target)
            "LIFETIME_XP_MILESTONE" -> GoalRule.LifetimeXpMilestone(target)
            else -> error("Unsupported goal rule type")
        }
        return GoalDefinition(
            id = GoalId(obj.requiredString("id")),
            title = obj.requiredString("title"),
            rule = rule,
        )
    }
}

private fun JsonObject.requiredString(key: String): String =
    requireNotNull(this[key]?.jsonPrimitive?.contentOrNull) { "Missing string: $key" }

private fun JsonObject.requiredInt(key: String): Int =
    requireNotNull(this[key]?.jsonPrimitive?.intOrNull) { "Missing int: $key" }

private fun JsonObject.requiredArray(key: String) =
    requireNotNull(this[key]) { "Missing array: $key" }.jsonArray
