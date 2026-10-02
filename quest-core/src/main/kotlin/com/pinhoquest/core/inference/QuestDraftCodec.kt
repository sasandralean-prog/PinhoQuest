package com.pinhoquest.core.inference

import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestDraft
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestObjective
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class QuestDraftCodec(
    private val json: Json = Json { ignoreUnknownKeys = false }
) {
    fun decode(raw: String, plan: QuestGenerationPlan): QuestDraft {
        val payload = extractJsonObject(raw)
        val payloadObject = runCatching { json.parseToJsonElement(payload).jsonObject }
            .getOrElse { throw IllegalArgumentException("malformed quest payload", it) }
        require(payloadObject.keys == QuestOutputSchema.KEYS) { "quest payload fields do not match contract" }

        val title = requiredString(payloadObject, "title")
        val description = requiredString(payloadObject, "description")
        val objectives = requiredStrings(payloadObject, "objectives", optional = false)
        val bonusObjectives = requiredStrings(payloadObject, "bonusObjectives", optional = true)
        val minutes = payloadObject["estimatedMinutes"]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("estimatedMinutes must be an integer")
        require(minutes > 0) { "estimatedMinutes must be positive" }
        val minMinutes = plan.filters.minMinutes
        val maxMinutes = plan.filters.maxMinutes
        require(minMinutes == null || minutes >= minMinutes) {
            "estimatedMinutes is below the requested minimum"
        }
        require(maxMinutes == null || minutes <= maxMinutes) {
            "estimatedMinutes is above the requested maximum"
        }
        val difficultyName = requiredString(payloadObject, "estimatedDifficulty")
        val difficulty = runCatching { QuestDifficulty.valueOf(difficultyName) }
            .getOrElse { throw IllegalArgumentException("unsupported estimatedDifficulty", it) }
        require(plan.filters.desiredDifficulty == null || difficulty == plan.filters.desiredDifficulty) {
            "estimatedDifficulty does not match the requested difficulty"
        }
        require(objectives.isNotEmpty()) { "objectives must not be empty" }

        return QuestDraft(
            title = title,
            description = description,
            objectives = objectives.mapIndexed { index, text ->
                QuestObjective(ObjectiveId("main-${index + 1}"), text)
            } + bonusObjectives.mapIndexed { index, text ->
                QuestObjective(ObjectiveId("bonus-${index + 1}"), text, optional = true)
            },
            category = plan.selectedCategory,
            environment = plan.selectedEnvironment,
            estimatedDuration = EstimatedDuration(minutes, minutes),
            difficulty = difficulty,
        )
    }

    private fun requiredString(jsonObject: JsonObject, key: String): String {
        val primitive = jsonObject[key] as? JsonPrimitive
            ?: throw IllegalArgumentException("$key must be a string")
        require(primitive.isString) { "$key must be a string" }
        val value = primitive.content.trim()
        require(value.isNotEmpty()) { "$key must be a non-blank string" }
        return value
    }

    private fun requiredStrings(jsonObject: JsonObject, key: String, optional: Boolean): List<String> {
        val element = jsonObject[key] ?: throw IllegalArgumentException("$key is required")
        val array = element as? JsonArray ?: throw IllegalArgumentException("$key must be an array")
        val values = array.map { item ->
            val primitive = item as? JsonPrimitive
                ?: throw IllegalArgumentException("$key must contain only strings")
            require(primitive.isString) { "$key must contain only strings" }
            primitive.content.trim().also {
                require(it.isNotEmpty()) { "$key must not contain blank values" }
            }
        }
        if (!optional) return values
        return values
    }
    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        require(start >= 0) { "quest payload does not contain a JSON object" }
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until raw.length) {
            val char = raw[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (inString && char == '\\') {
                escaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            if (inString) continue
            when (char) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, index + 1)
                }
            }
        }
        throw IllegalArgumentException("quest payload JSON object is incomplete")
    }

    private companion object {
        object QuestOutputSchema {
            val KEYS = linkedSetOf(
                "title", "description", "objectives", "bonusObjectives",
                "estimatedMinutes", "estimatedDifficulty",
            )
        }
    }
}