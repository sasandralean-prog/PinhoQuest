package com.pinhoquest.core.research

enum class ResearchStatus {
    SUCCESS,
    UNAVAILABLE,
    RATE_LIMITED,
    INVALID_RESPONSE,
    UNSUPPORTED_SOURCE,
    TECHNICAL_FAILURE,
}

sealed interface ResearchOutcome<out T> {
    data class Success<T>(val items: List<T>) : ResearchOutcome<T>
    data object Unavailable : ResearchOutcome<Nothing>
    data object RateLimited : ResearchOutcome<Nothing>
    data object InvalidResponse : ResearchOutcome<Nothing>
    data object UnsupportedSource : ResearchOutcome<Nothing>
    data object TechnicalFailure : ResearchOutcome<Nothing>
}

data class ResearchProvenance(
    val sourceUri: String,
    val researchedAtEpochMillis: Long,
) {
    init {
        require(sourceUri.isNotBlank()) { "sourceUri must not be blank" }
        require(researchedAtEpochMillis > 0L) { "researchedAtEpochMillis must be positive" }
    }
}

enum class GameAvailability {
    FREE,
    FREE_TO_PLAY,
    UNKNOWN,
}

data class GameDiscovery(
    val canonicalName: String,
    val platforms: Set<String>,
    val genres: Set<String>,
    val availability: GameAvailability,
    val provenance: Set<ResearchProvenance>,
) {
    init {
        require(canonicalName.isNotBlank()) { "canonicalName must not be blank" }
        require(provenance.isNotEmpty()) { "provenance must not be empty" }
    }

    val identityKey: String
        get() = normalize(canonicalName)

    companion object {
        fun normalize(value: String): String = value
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
    }
}

interface GameResearchPort {
    suspend fun research(query: String, platform: String? = null): ResearchOutcome<GameDiscovery>
}
