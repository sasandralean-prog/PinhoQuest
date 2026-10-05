package com.pinhoquest.data.research

import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.GameDiscovery
import com.pinhoquest.core.research.GameResearchPort
import com.pinhoquest.core.research.ResearchOutcome
import com.pinhoquest.core.research.ResearchProvenance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.time.Clock

class WikipediaGameResearchAdapter(
    private val transport: ResearchHttpTransport,
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GameResearchPort {
    override suspend fun research(query: String, platform: String?): ResearchOutcome<GameDiscovery> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return ResearchOutcome.Unavailable

        val encoded = encode(normalizedQuery)
        val response = try {
            transport.get("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
        } catch (_: Exception) {
            return ResearchOutcome.TechnicalFailure
        }

        return when (response.statusCode) {
            200 -> parseGameSummary(response.body, normalizedQuery)
            404 -> ResearchOutcome.Unavailable
            429 -> ResearchOutcome.RateLimited
            in 400..499 -> ResearchOutcome.UnsupportedSource
            in 500..599 -> ResearchOutcome.TechnicalFailure
            else -> ResearchOutcome.InvalidResponse
        }
    }

    private fun parseGameSummary(body: String, query: String): ResearchOutcome<GameDiscovery> {
        val summary = try {
            json.decodeFromString<WikipediaSummary>(body)
        } catch (_: Exception) {
            return ResearchOutcome.InvalidResponse
        }

        val title = summary.title?.trim().orEmpty()
        val extract = summary.extract?.trim().orEmpty()
        val description = summary.description?.trim().orEmpty()
        if (title.isEmpty() || extract.isEmpty()) return ResearchOutcome.InvalidResponse

        val gameEvidence = listOf(title, description, extract).joinToString(" ").lowercase()
        if (!gameEvidence.contains("video game") && !gameEvidence.contains("video games")) {
            return ResearchOutcome.Unavailable
        }

        val sourceUri = summary.contentUrls?.desktop?.page
            ?.takeIf { it.isNotBlank() }
            ?: "https://en.wikipedia.org/wiki/\${encode(title)}"

        return ResearchOutcome.Success(
            listOf(
                GameDiscovery(
                    canonicalName = title.ifBlank { query },
                    platforms = emptySet(),
                    genres = emptySet(),
                    availability = GameAvailability.UNKNOWN,
                    provenance = setOf(
                        ResearchProvenance(
                            sourceUri = sourceUri,
                            researchedAtEpochMillis = clock.millis(),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

@Serializable
private data class WikipediaSummary(
    val title: String? = null,
    val description: String? = null,
    val extract: String? = null,
    @SerialName("content_urls") val contentUrls: WikipediaContentUrls? = null,
)

@Serializable
private data class WikipediaContentUrls(
    val desktop: WikipediaDesktopUrl? = null,
)

@Serializable
private data class WikipediaDesktopUrl(
    val page: String? = null,
)
