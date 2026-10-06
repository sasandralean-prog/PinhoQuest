package com.pinhoquest.core.profile

import java.util.UUID
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProfileIdFactoryTest {
    @Test
    fun randomProfileIdsAreDistinctAndPortable() {
        val first = RandomProfileIdFactory.newId()
        val second = RandomProfileIdFactory.newId()

        assertNotEquals(first, second)
        UUID.fromString(first.value)
        UUID.fromString(second.value)
    }
}
