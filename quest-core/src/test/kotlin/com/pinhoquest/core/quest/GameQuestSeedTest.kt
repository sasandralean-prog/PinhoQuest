package com.pinhoquest.core.quest

import com.pinhoquest.core.research.GameCandidateCycleId
import com.pinhoquest.core.research.GameDiscovery
import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.GameQuestContext
import com.pinhoquest.core.research.GameIdentityKey
import com.pinhoquest.core.research.ResearchProvenance
import com.pinhoquest.core.research.GameDiscovery.Companion.normalize
import org.junit.Assert.assertEquals
import org.junit.Test

class GameQuestSeedTest {
    @Test fun fromContextCarriesCanonicalGameIdentityAndCycle() {
        val discovery = GameDiscovery(
            canonicalName = "Minecraft",
            platforms = setOf("Android", "PC"),
            genres = setOf("Sandbox"),
            availability = GameAvailability.FREE_TO_PLAY,
            provenance = setOf(ResearchProvenance("https://example.test/minecraft", 1_000L)),
        )
        val context = GameQuestContext(GameCandidateCycleId("cycle-7"), discovery)
        val seed = GameQuestSeed.from(context)

        assertEquals("minecraft", seed.gameIdentity.value)
        assertEquals("Minecraft", seed.title)
        assertEquals("cycle-7", seed.cycleId)
        assertEquals(com.pinhoquest.domain.quest.QuestEnvironment.ANDROID, seed.environment)
    }
}
