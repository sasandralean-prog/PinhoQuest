package com.pinhoquest.data.research

import com.pinhoquest.core.research.ResearchOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class WikipediaGameDiscoveryAdapterTest {
    @Test
    fun admitsOnlySearchResultsWithVideoGameEvidenceAndProvenance() = runBlocking {
        val responses = ArrayDeque(
            listOf(
                ResearchHttpResponse(
                    200,
                    """{"query":{"search":[{"title":"Example Game"},{"title":"Example Tree"}]}}""",
                ),
                ResearchHttpResponse(
                    200,
                    """{"title":"Example Game","description":"2025 video game","extract":"Example Game is a video game.","content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Example_Game"}}}""",
                ),
                ResearchHttpResponse(
                    200,
                    """{"title":"Example Tree","description":"tree","extract":"A species of tree."}""",
                ),
            ),
        )
        val adapter = WikipediaGameDiscoveryAdapter(
            transport = object : ResearchHttpTransport {
                override suspend fun get(url: String): ResearchHttpResponse = responses.removeFirst()
            },
            clock = Clock.fixed(Instant.ofEpochMilli(1234L), ZoneOffset.UTC),
        )

        val outcome = adapter.research("video game")

        assertTrue(outcome is ResearchOutcome.Success)
        val games = (outcome as ResearchOutcome.Success).items
        assertEquals(listOf("Example Game"), games.map { it.canonicalName })
        assertEquals("https://en.wikipedia.org/wiki/Example_Game", games.single().provenance.single().sourceUri)
        assertEquals(1234L, games.single().provenance.single().researchedAtEpochMillis)
    }

    @Test
    fun emptySearchResultsAreUnavailableRatherThanInvented() = runBlocking {
        val adapter = WikipediaGameDiscoveryAdapter(
            transport = object : ResearchHttpTransport {
                override suspend fun get(url: String) =
                    ResearchHttpResponse(200, """{"query":{"search":[]}}""")
            },
        )

        assertTrue(adapter.research("video game") is ResearchOutcome.Unavailable)
    }
}
