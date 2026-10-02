package com.pinhoquest.core.inference.prompt

class CompactPromptSerializer(
    private val limits: PromptContractLimits = PromptContractLimits(),
) {
    fun serialize(envelope: BoundedPromptEnvelope): String {
        val json = buildJson(envelope)
        val prompt = PREAMBLE + "\n" + json
        require(prompt.length <= limits.maxPromptCharacters) {
            "prompt exceeds deterministic character budget"
        }
        return prompt
    }

    private fun buildJson(envelope: BoundedPromptEnvelope): String = buildString {
        append("{\"v\":")
        append(envelope.schemaVersion)
        append(",\"task\":\"")
        append(escape(envelope.task))
        append("\",\"category\":\"")
        append(envelope.category.name)
        append("\",\"environment\":\"")
        append(envelope.environment.name)
        append("\",\"time\":\"")
        append(formatTime(envelope.minMinutes, envelope.maxMinutes))
        append("\"" )
        if (envelope.difficulty != null) {
            append(",\"difficulty\":\"")
            append(envelope.difficulty.name)
            append("\"" )
        }
        append(",\"tags\":[")
        envelope.selectedTags.joinTo(this, ",") {
            "\"${escape(it.label)}\""
        }
        append("]")
        append(",\"research\":[")
        envelope.researchHints.joinTo(this, ",") { serializeResearch(it) }
        append("],\"output\":[")
        QuestPromptContract.OUTPUT_FIELDS.joinTo(this, ",") { "\"$it\"" }
        append("]}")
    }

    private fun serializeResearch(hint: PromptResearchHint): String = when (hint) {
        is PromptResearchHint.Game -> buildString {
            append("{\"k\":\"GAME\",\"name\":\"")
            append(escape(hint.canonicalName))
            append("\"" )
            hint.platform?.let {
                append(",\"platform\":\"")
                append(escape(it))
                append("\"" )
            }
            hint.genre?.let {
                append(",\"genre\":\"")
                append(escape(it))
                append("\"" )
            }
            append(",\"availability\":\"")
            append(hint.availability.name)
            append("\"}")
        }
        is PromptResearchHint.Flower -> buildString {
            append("{\"k\":\"FLOWER\",\"name\":\"")
            append(escape(hint.canonicalName))
            append("\"" )
            hint.commonName?.let { append(",\"commonName\":\"" + escape(it) + "\"" ) }
            hint.region?.let { append(",\"region\":\"" + escape(it) + "\"" ) }
            append("}")
        }
    }
    private fun formatTime(min: Int?, max: Int?): String =
        when {
            min != null && max != null -> "$min-$max"
            min != null -> "$min+"
            max != null -> "0-$max"
            else -> "ANY"
        }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private companion object {
        const val PREAMBLE =
            "Crie uma quest usando somente os fatos fornecidos. Nao invente fatos, XP ou recompensas. " +
                "Responda somente com JSON valido usando os campos pedidos."
    }
}