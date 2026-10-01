package com.pinhoquest.core.garden

import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.EstimateConfidence
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowerRarityScaleTest {
    private val scale = FlowerRarityScale(
        version = 1,
        referenceMinPopulation = 1L,
        referenceMaxPopulation = 100_000_000L,
        requiredCountingBasis = CountingBasis.INDIVIDUALS,
    )

    @Test
    fun fourEqualLogBandsAreClassifiedFromCommonToRarest() {
        assertEquals(FlowerRarity.COMMON, scale.classify(evidence(10_000_000L)))
        assertEquals(FlowerRarity.UNCOMMON, scale.classify(evidence(100_000L)))
        assertEquals(FlowerRarity.RARE, scale.classify(evidence(1_000L)))
        assertEquals(FlowerRarity.RAREST, scale.classify(evidence(10L)))
    }

    @Test
    fun exactQuarterBoundariesPromoteToNextRarerBand() {
        assertEquals(FlowerRarity.UNCOMMON, scale.classify(evidence(1_000_000L)))
        assertEquals(FlowerRarity.RARE, scale.classify(evidence(10_000L)))
        assertEquals(FlowerRarity.RAREST, scale.classify(evidence(100L)))
    }

    @Test
    fun nonPositiveOrMissingPopulationIsUnknown() {
        assertEquals(FlowerRarity.UNKNOWN, scale.classify(evidence(0L)))
        assertEquals(FlowerRarity.UNKNOWN, scale.classify(evidence(-1L)))
        assertEquals(
            FlowerRarity.UNKNOWN,
            scale.classify(
                GlobalPopulationEstimate(
                    estimatedIndividuals = null,
                    lowerBound = null,
                    upperBound = null,
                    estimateDateEpochMillis = 1L,
                    evidenceSourceUris = setOf("https://example.test/source"),
                    confidence = EstimateConfidence.MEDIUM,
                    countingBasis = CountingBasis.INDIVIDUALS,
                ),
            ),
        )
    }

    @Test
    fun missingProvenanceOrDateIsUnknown() {
        assertEquals(
            FlowerRarity.UNKNOWN,
            scale.classify(evidence(1_000L).copy(evidenceSourceUris = emptySet())),
        )
        assertEquals(
            FlowerRarity.UNKNOWN,
            scale.classify(evidence(1_000L).copy(estimateDateEpochMillis = null)),
        )
    }

    @Test
    fun incompatibleCountingBasisIsRejectedAsUnknown() {
        assertEquals(
            FlowerRarity.UNKNOWN,
            scale.classify(evidence(1_000L).copy(countingBasis = CountingBasis.MATURE_INDIVIDUALS)),
        )
    }

    @Test
    fun valuesOutsideReferenceRangeClampToScaleEnds() {
        assertEquals(FlowerRarity.COMMON, scale.classify(evidence(1_000_000_000L)))
        assertEquals(FlowerRarity.RAREST, scale.classify(evidence(1L)))
    }

    private fun evidence(population: Long) = GlobalPopulationEstimate(
        estimatedIndividuals = population,
        lowerBound = null,
        upperBound = null,
        estimateDateEpochMillis = 1L,
        evidenceSourceUris = setOf("https://example.test/source"),
        confidence = EstimateConfidence.MEDIUM,
        countingBasis = CountingBasis.INDIVIDUALS,
    )
}
