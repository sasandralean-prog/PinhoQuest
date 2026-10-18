package com.pinhoquest.flora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowerSpriteIdentityTest {
    @Test
    fun flowerHashIsStableForCaseAndWhitespaceVariants() {
        assertEquals(
            FlowerSpriteIdentity.flowerSha256("Rosa   Canina"),
            FlowerSpriteIdentity.flowerSha256("  rosa canina "),
        )
    }

    @Test
    fun flowerHashChangesWhenScientificNameChanges() {
        assertNotEquals(
            FlowerSpriteIdentity.flowerSha256("Rosa canina"),
            FlowerSpriteIdentity.flowerSha256("Rosa gallica"),
        )
    }

    @Test
    fun hashesAreSha256Hex() {
        val hash = FlowerSpriteIdentity.flowerSha256("Rosa canina")
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun collectionHashIgnoresOrderingAndDuplicateMembers() {
        val rosa = FlowerSpriteIdentity.flowerSha256("Rosa canina")
        val tulipa = FlowerSpriteIdentity.flowerSha256("Tulipa gesneriana")
        val orchid = FlowerSpriteIdentity.flowerSha256("Phalaenopsis amabilis")

        assertEquals(
            FlowerSpriteIdentity.collectionSha256(listOf(rosa, tulipa, orchid)),
            FlowerSpriteIdentity.collectionSha256(listOf(orchid, rosa, tulipa, rosa)),
        )
    }

    @Test
    fun collectionHashChangesWhenMemberSetChanges() {
        val rosa = FlowerSpriteIdentity.flowerSha256("Rosa canina")
        val tulipa = FlowerSpriteIdentity.flowerSha256("Tulipa gesneriana")

        assertNotEquals(
            FlowerSpriteIdentity.collectionSha256(listOf(rosa)),
            FlowerSpriteIdentity.collectionSha256(listOf(rosa, tulipa)),
        )
    }

    @Test
    fun scientificNameSetProducesSameCollectionHashRegardlessOfOrder() {
        assertEquals(
            FlowerSpriteIdentity.collectionShaFromScientificNames(
                listOf("Rosa canina", "Tulipa gesneriana"),
            ),
            FlowerSpriteIdentity.collectionShaFromScientificNames(
                listOf("Tulipa gesneriana", "Rosa canina"),
            ),
        )
    }
}
