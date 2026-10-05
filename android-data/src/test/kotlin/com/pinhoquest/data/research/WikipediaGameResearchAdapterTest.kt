package com.pinhoquest.data.research

import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.ResearchOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class WikipediaGameResearchAdapterTest {
    private val clock = Clock.fixed(Instant.ofEpochMilli(1234L), ZoneOffset.UTC)

    @Test
    fun successfulVideoGameSummaryBecomesGovernedDiscoveryWithProvenance() = runBlocking {
        val adapter = adapter(200, """{"title":"Example Quest","description":"2024 video game","extract":"Example Quest is a video game about exploration.","content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Example_Quest"}}}""")

        val outcome = adapter.research("Example Quest")
        assertTrue(outcome is ResearchOutcome.Success)
        val items = (outcome as ResearchOutcome.Success).items
        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("Example Quest", item.canonicalName)
        assertEquals(GameAvailability.UNKNOWN, item.availability)
        assertEquals("https://en.wikipedia.org/wiki/Example_Quest", item.provenance.single().sourceUri)
        assertEquals(1234L, item.provenance.single().researchedAtEpochMillis)
    }

    @Test
    fun nonGamePageDoesNotBecomeAFalseGameFact() = runBlocking {
        val adapter = adapter(200, """{"title":"Example Tree","description":"tree","extract":"Example Tree is a tree."}""")
        assertTrue(adapter.research("Example Tree") is ResearchOutcome.Unavailable)
    }

    @Test
    fun statusCodesRemainSemantic() = runBlocking {
        assertTrue(adapter(404, "").research("missing") is ResearchOutcome.Unavailable)
        assertTrue(adapter(429, "").research("limited") is ResearchOutcome.RateLimited)
        assertTrue(adapter(503, "").research("down") is ResearchOutcome.TechnicalFailure)
        assertTrue(adapter(200, "not json").research("broken") is ResearchOutcome.InvalidResponse)
    }

    private fun adapter(status: Int, body: String) = WikipediaGameResearchAdapter(
        transport = object : ResearchHttpTransport {
            override suspend fun get(url: String) = ResearchHttpResponse(status, body)
        },
        clock = clock,
    )
}
