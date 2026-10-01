package com.pinhoquest.data.catalog

import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.FlowerRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedCatalogLoaderTest {
    @Test
    fun collectionIHas24UniqueRealSpeciesAndValidatedClassifiedEvidence() {
        val loaded = EmbeddedCatalogLoader.loadCollectionI()

        assertEquals(24, loaded.pack.entries.size)
        assertEquals(
            24,
            loaded.pack.entries.map { it.flower.scientificName }.distinct().size,
        )
        assertTrue(loaded.validation.isValid)

        val classified = loaded.pack.entries
            .map { it.flower }
            .filter { it.rarity != FlowerRarity.UNKNOWN }

        assertTrue(classified.size >= 10)
        assertEquals(
            setOf(
                FlowerRarity.COMMON,
                FlowerRarity.UNCOMMON,
                FlowerRarity.RARE,
                FlowerRarity.RAREST,
            ),
            classified.map { it.rarity }.toSet(),
        )
        classified.forEach { flower ->
            val estimate = requireNotNull(flower.populationEstimate)
            assertEquals(CountingBasis.MATURE_INDIVIDUALS, estimate.countingBasis)
            assertTrue(requireNotNull(estimate.estimatedIndividuals) > 0L)
            assertTrue(requireNotNull(estimate.estimateDateEpochMillis) > 0L)
            assertTrue(estimate.evidenceSourceUris.isNotEmpty())
            assertEquals(1, flower.rarityScaleVersion)
        }

        val ambiguousCounts = setOf(
            "Didymocarpus phuquocensis",
            "Keetia susu",
        )
        loaded.pack.entries
            .map { it.flower }
            .filter { it.scientificName in ambiguousCounts }
            .forEach { flower ->
                assertEquals(FlowerRarity.UNKNOWN, flower.rarity)
                assertEquals(null, flower.rarityScaleVersion)
            }
    }

    @Test
    fun collectionIHasStableIntegrityHashAndNoDuplicateIds() {
        val first = EmbeddedCatalogLoader.loadCollectionI()
        val second = EmbeddedCatalogLoader.loadCollectionI()

        assertEquals(first.pack.integrityHash, second.pack.integrityHash)
        assertEquals(
            24,
            first.pack.entries.map { it.flower.id }.distinct().size,
        )
        assertTrue(first.pack.integrityHash.matches(Regex("[0-9a-f]{64}")))
    }
}
