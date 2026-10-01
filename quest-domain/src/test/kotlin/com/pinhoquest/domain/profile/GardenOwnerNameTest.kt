package com.pinhoquest.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenOwnerNameTest {
    @Test
    fun trimsValidName() {
        val result = GardenOwnerName.create("  Rafa  ")

        assertEquals("Rafa", result.getOrThrow().value)
    }

    @Test
    fun rejectsBlankName() {
        assertTrue(GardenOwnerName.create("   ").isFailure)
    }

    @Test
    fun acceptsTwentyCharacters() {
        val raw = "12345678901234567890"

        assertEquals(raw, GardenOwnerName.create(raw).getOrThrow().value)
    }

    @Test
    fun rejectsTwentyOneCharacters() {
        val raw = "123456789012345678901"

        assertTrue(GardenOwnerName.create(raw).isFailure)
    }
}
