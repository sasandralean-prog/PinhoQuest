package com.pinhoquest.core.inference.micro

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FunctionGemmaToolCallExtractor(
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    companion object {
        const val TOOL_NAME = "compose_quest_text"
    }

    fun extract(raw: String): MicroQuestText {
        require(raw.isNotBlank()) { "empty function-call response" }
        val marker = "<start_function_call>"
        val endMarker = "<end_function_call>"
        val start = raw.indexOf(marker)
        require(start >= 0) { "function-call start marker not found" }
        val end = raw.indexOf(endMarker, start + marker.length)
        require(end >= 0) { "function-call end marker not found" }
        val body = raw.substring(start + marker.length, end).trim()
        val prefix = "call:$TOOL_NAME"
        require(body.startsWith(prefix)) { "unexpected function name" }
        val arguments = parseObject(body.substring(prefix.length).trim())
        require(arguments.keys == setOf("title", "description", "objectives")) {
            "unexpected function arguments"
        }
        val title = stringField(arguments, "title")
        val description = stringField(arguments, "description")
        val objectives = stringArrayField(arguments, "objectives")
        require(title.length <= MicroQuestContract.MAX_TITLE)
        require(description.length in 12..MicroQuestContract.MAX_DESCRIPTION)
        require(objectives.size in 1..MicroQuestContract.MAX_OBJECTIVES)
        require(objectives.all { it.length <= MicroQuestContract.MAX_OBJECTIVE })
        return MicroQuestText(title, description, objectives)
    }

    private fun parseObject(raw: String): JsonObject {
        val objectStart = raw.indexOf('{')
        val objectEnd = raw.lastIndexOf('}')
        require(objectStart >= 0 && objectEnd > objectStart)
        return json.parseToJsonElement(raw.substring(objectStart, objectEnd + 1)) as? JsonObject
            ?: error("function arguments are not an object")
    }

    private fun stringField(obj: JsonObject, key: String): String {
        val primitive = obj[key] as? JsonPrimitive ?: throw IllegalArgumentException("$key must be string")
        require(primitive.isString) { "$key must be string" }
        return primitive.content.trim().also { require(it.isNotBlank()) }
    }

    private fun stringArrayField(obj: JsonObject, key: String): List<String> {
        val array = obj[key] as? JsonArray ?: throw IllegalArgumentException("$key must be array")
        return array.map {
            val primitive = it as? JsonPrimitive ?: throw IllegalArgumentException("$key must contain strings")
            require(primitive.isString) { "$key must contain strings" }
            primitive.content.trim().also { text -> require(text.isNotBlank()) }
        }
    }
}