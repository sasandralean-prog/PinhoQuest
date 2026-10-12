package com.pinhoquest.data.research

import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.GameDiscovery
import com.pinhoquest.core.research.GameResearchPort
import com.pinhoquest.core.research.ResearchOutcome
import com.pinhoquest.core.research.ResearchProvenance
import java.time.Clock
import java.net.URLEncoder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Discovers a bounded set of candidate game pages, then admits only pages whose
 * own summary contains explicit video-game evidence. Search-result titles alone
 * are never treated as factual game identity.
 */
class WikipediaGameDiscoveryAdapter(
    private val transport: ResearchHttpTransport,
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val maxResults: Int = 8,
) : GameResearchPort {
    init {
        require(maxResults in 1..10)
    }

    override suspend fun research(query: String, platform: String?): ResearchOutcome<GameDiscovery> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return ResearchOutcome.Unavailable

        val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search" +
            "&srsearch=${encode(normalizedQuery)}&srnamespace=0&srlimit=$maxResults&format=json"
        val searchResponse = try {
            transport.get(searchUrl)
        } catch (_: Exception) {
            return ResearchOutcome.TechnicalFailure
        }
        if (searchResponse.statusCode != 200) return outcomeForStatus(searchResponse.statusCode)

        val searchResults = try {
            json.decodeFromString<WikipediaSearchResponse>(searchResponse.body)
                .query?.search.orEmpty()
        } catch (_: Exception) {
            return ResearchOutcome.InvalidResponse
        }
        if (searchResults.isEmpty()) return ResearchOutcome.Unavailable

        val discoveries = mutableListOf<GameDiscovery>()
        for (result in searchResults.take(maxResults)) {
            val title = result.title.trim()
            if (title.isEmpty() || isGenericCatalogPage(title)) continue

            val summaryResponse = try {
                transport.get("https://en.wikipedia.org/api/rest_v1/page/summary/${encode(title)}")
            } catch (_: Exception) {
                continue
            }
            if (summaryResponse.statusCode != 200) continue

            val summary = try {
                json.decodeFromString<WikipediaGameSummary>(summaryResponse.body)
            } catch (_: Exception) {
                continue
            }
            val canonicalName = summary.title?.trim().orEmpty().ifBlank { title }
            val evidence = listOf(summary.description.orEmpty(), summary.extract.orEmpty())
                .joinToString(" ")
                .lowercase()
            if (!evidence.contains("video game") && !evidence.contains("video games")) continue

            val sourceUri = summary.contentUrls?.desktop?.page
                ?.takeIf { it.isNotBlank() }
                ?: "https://en.wikipedia.org/wiki/${encode(canonicalName)}"
            discoveries += GameDiscovery(
                canonicalName = canonicalName,
                platforms = platform?.let(::setOf).orEmpty(),
                genres = emptySet(),
                availability = GameAvailability.UNKNOWN,
                provenance = setOf(
                    ResearchProvenance(
                        sourceUri = sourceUri,
                        researchedAtEpochMillis = clock.millis(),
                    ),
                ),
            )
        }

        return if (discoveries.isEmpty()) ResearchOutcome.Unavailable
        else ResearchOutcome.Success(discoveries.distinctBy { it.identityKey })
    }

    private fun outcomeForStatus(statusCode: Int): ResearchOutcome<GameDiscovery> = when (statusCode) {
        404 -> ResearchOutcome.Unavailable
        429 -> ResearchOutcome.RateLimited
        in 400..499 -> ResearchOutcome.UnsupportedSource
        in 500..599 -> ResearchOutcome.TechnicalFailure
        else -> ResearchOutcome.InvalidResponse
    }

    private fun isGenericCatalogPage(title: String): Boolean {
        val normalized = title.trim().lowercase()
        return normalized.startsWith("list of ") ||
            normalized in setOf("video game", "video games", "history of video games", "video game industry")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    @Serializable
    private data class WikipediaSearchResponse(val query: WikipediaSearchQuery? = null)

    @Serializable
    private data class WikipediaSearchQuery(val search: List<WikipediaSearchResult> = emptyList())

    @Serializable
    private data class WikipediaSearchResult(val title: String = "")

    @Serializable
    private data class WikipediaGameSummary(
        val title: String? = null,
        val description: String? = null,
        val extract: String? = null,
        @SerialName("content_urls") val contentUrls: WikipediaGameContentUrls? = null,
    )

    @Serializable
    private data class WikipediaGameContentUrls(val desktop: WikipediaGameDesktopUrl? = null)

    @Serializable
    private data class WikipediaGameDesktopUrl(val page: String? = null)
}
