package com.pinhoquest.core.reward

import com.pinhoquest.domain.garden.CatalogEntry
import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.CatalogPackId
import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.EstimateConfidence
import com.pinhoquest.domain.garden.FlowerDefinition
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.garden.FlowerInventory
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.reward.RewardOpportunity
import com.pinhoquest.domain.reward.RewardOpportunityId
import com.pinhoquest.domain.reward.RewardResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardEngineTest {
    private val opportunity = RewardOpportunity(
        id = RewardOpportunityId("r1"),
        profileId = ProfileId("p1"),
        completionId = CompletionId("c1"),
        createdAtEpochMillis = 1L,
    )

    @Test
    fun collectedFlowersAreExcludedFromEligiblePool() {
        val engine = RewardEngine(UniformIndexSource { 0 })
        val catalog = pack("a", "b", "c")

        val result = engine.resolve(
            opportunity,
            catalog,
            FlowerInventory(setOf(FlowerId("a"), FlowerId("b"))),
        )

        assertEquals(FlowerId("c"), (result as RewardResolution.Awarded).flowerId)
    }

    @Test
    fun everyEligibleFlowerCanBeSelectedByInjectedUniformIndex() {
        val catalog = pack("a", "b", "c")
        val inventory = FlowerInventory(emptySet())

        val selected = (0..2).map { index ->
            val engine = RewardEngine(UniformIndexSource { bound ->
                assertEquals(3, bound)
                index
            })
            (engine.resolve(opportunity, catalog, inventory) as RewardResolution.Awarded).flowerId.value
        }

        assertEquals(listOf("a", "b", "c"), selected)
    }

    @Test
    fun exhaustedCatalogDefersInsteadOfLosingReward() {
        val catalog = pack("a", "b")
        val result = RewardEngine(UniformIndexSource { 0 }).resolve(
            opportunity,
            catalog,
            FlowerInventory(setOf(FlowerId("a"), FlowerId("b"))),
        )

        assertTrue(result is RewardResolution.Deferred)
    }

    @Test
    fun entriesMarkedIneligibleNeverEnterDropPool() {
        val catalog = CatalogPack(
            id = CatalogPackId("p"),
            version = 1,
            generatedAtEpochMillis = 1L,
            entries = listOf(
                CatalogEntry(flower("a"), eligible = false),
                CatalogEntry(flower("b"), eligible = true),
            ),
            sourceUris = emptySet(),
            integrityHash = "hash",
        )

        val result = RewardEngine(UniformIndexSource { 0 }).resolve(
            opportunity,
            catalog,
            FlowerInventory(emptySet()),
        )

        assertEquals(FlowerId("b"), (result as RewardResolution.Awarded).flowerId)
    }

    private fun pack(vararg ids: String) = CatalogPack(
        id = CatalogPackId("p"),
        version = 1,
        generatedAtEpochMillis = 1L,
        entries = ids.map { CatalogEntry(flower(it), eligible = true) },
        sourceUris = emptySet(),
        integrityHash = "hash",
    )

    private fun flower(id: String) = FlowerDefinition(
        id = FlowerId(id),
        commonName = id,
        scientificName = "Species " + id,
        description = "",
        rarity = FlowerRarity.COMMON,
        rarityScaleVersion = 1,
        populationEstimate = GlobalPopulationEstimate(
            estimatedIndividuals = 1_000_000L,
            lowerBound = null,
            upperBound = null,
            estimateDateEpochMillis = 1L,
            evidenceSourceUris = setOf("https://example.test/" + id),
            confidence = EstimateConfidence.MEDIUM,
            countingBasis = CountingBasis.INDIVIDUALS,
        ),
    )
}
