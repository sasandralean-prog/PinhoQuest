package com.pinhoquest.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchitectureSmokeTest {
    @Test
    fun domainModuleLoads() {
        assertEquals("pinho-quest-domain", DomainModuleMarker.id)
    }
}
