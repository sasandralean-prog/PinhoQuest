package com.pinhoquest.core.inference.micro

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FunctionGemmaResponseSanitizer(
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    fun extract(raw: String): MicroQuestText {
        require(raw.isNotBlank()) { "empty model response" }
        val candidates = extractCandidates(raw)
        val objectNode = candidates.asSequence()
            .mapNotNull { candidate ->
                runCatching { json.parseToJsonElement(candidate) as? JsonObject }.getOrNull()
            }
            .firstOrNull { it.keys == setOf("title", "description", "objectives") }
            ?: throw IllegalArgumentException("micro quest payload not found")

        val title = stringField(objectNode, "title")
        val description = stringField(objectNode, "description")
        val objectives = arrayField(objectNode, "objectives")
        require(title.length <= MicroQuestContract.MAX_TITLE)
        require(description.length in 12..MicroQuestContract.MAX_DESCRIPTION)
        require(objectives.isNotEmpty() && objectives.size <= MicroQuestContract.MAX_OBJECTIVES)
        require(objectives.all { it.length <= MicroQuestContract.MAX_OBJECTIVE })
        return MicroQuestText(title, description, objectives)
    }

    private fun extractCandidates(raw: String): List<String> {
        val normalized = raw
            .replace("<|im_start|>model", "")
            .replace("<|im_start|>assistant", "")
            .replace("<|im_end|>", "")
            .replace("<start_of_turn>model", "")
            .replace("<end_of_turn>", "")
            .replace("<escape>", "")
            .trim()
        val candidates = mutableListOf<String>()
        var start = -1
        var depth = 0
        var inString = false
        var escaped = false
        normalized.forEachIndexed { index, char ->
            if (escaped) { escaped = false; return@forEachIndexed }
            if (inString && char == '\\') { escaped = true; return@forEachIndexed }
            if (char == '"') { inString = !inString; return@forEachIndexed }
            if (inString) return@forEachIndexed
            when (char) {
                '{' -> { if (depth == 0) start = index; depth++ }
                '}' -> {
                    if (depth > 0) depth--
                    if (depth == 0 && start >= 0) {
                        candidates += normalized.substring(start, index + 1)
                        start = -1
                    }
                }
            }
        }
        return candidates
    }
    private fun stringField(obj: JsonObject, key: String): String {
        val primitive = obj[key] as? JsonPrimitive
            ?: throw IllegalArgumentException("$key must be string")
        require(primitive.isString) { "$key must be string" }
        return primitive.content.trim().also { require(it.isNotBlank()) }
    }

    private fun arrayField(obj: JsonObject, key: String): List<String> {
        val array = obj[key] as? JsonArray
            ?: throw IllegalArgumentException("$key must be array")
        return array.map {
            val primitive = it as? JsonPrimitive
                ?: throw IllegalArgumentException("$key must contain strings")
            require(primitive.isString) { "$key must contain strings" }
            primitive.content.trim().also { text -> require(text.isNotBlank()) }
        }
    }
}
