package com.pinhoquest.core.inference.micro

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FunctionGemmaToolCallExtractor(
    private val json: Json = Json { ignoreUnknownKeys = false },
) {
    fun extract(raw: String): MicroQuestText {
        require(raw.isNotBlank()) { "empty function-call response" }
        val marker = "<start_function_call>"
        val endMarker = "<end_function_call>"
        val start = raw.indexOf(marker)
        require(start >= 0) { "function-call start marker not found" }
        val end = raw.indexOf(endMarker, start + marker.length)
        require(end >= 0) { "function-call end marker not found" }
        val body = raw.substring(start + marker.length, end).trim()
        val prefix = "call:" + MicroQuestToolContract.NAME
        require(body.startsWith(prefix)) { "unexpected function name" }
        val arguments = parseObject(body.substring(prefix.length).trim())
        require(arguments.keys == MicroQuestToolContract.REQUIRED_ARGUMENTS.toSet()) {
            "unexpected function arguments"
        }
        val title = stringField(arguments, "title")
        val description = stringField(arguments, "description")
        val objectives = stringArrayField(arguments, "objectives")
        require(title.length in MicroQuestToolContract.MIN_TITLE_LENGTH..MicroQuestToolContract.MAX_TITLE_LENGTH)
        require(description.length in MicroQuestToolContract.MIN_DESCRIPTION_LENGTH..MicroQuestToolContract.MAX_DESCRIPTION_LENGTH)
        require(objectives.size in MicroQuestToolContract.MIN_OBJECTIVES..MicroQuestToolContract.MAX_OBJECTIVES)
        require(objectives.all { it.length in MicroQuestToolContract.MIN_OBJECTIVE_LENGTH..MicroQuestToolContract.MAX_OBJECTIVE_LENGTH })
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