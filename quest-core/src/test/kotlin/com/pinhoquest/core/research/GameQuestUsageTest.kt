package com.pinhoquest.core.research

import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameQuestUsageTest {
    private val game = GameIdentityKey("minecraft")
    private val variantA = GameQuestVariant(GameQuestActivity.EXPLORATION, QuestCategory.GAMING, QuestEnvironment.ANYWHERE, "explore a cave")
    private val variantB = variantA.copy(activity = GameQuestActivity.CONSTRUCTION, objectivePattern = "build a shelter")

    @Test fun sameInputsProduceSameSha256UsageId() {
        val cycle = GameCandidateCycleId("cycle-1")
        assertEquals(GameQuestUsageId.create(cycle, game, variantA), GameQuestUsageId.create(cycle, game, variantA))
        assertEquals(64, GameQuestUsageId.create(cycle, game, variantA).value.length)
    }

    @Test fun semanticVariantChangesUsageIdWithoutUsingRawQuestText() {
        val cycle = GameCandidateCycleId("cycle-1")
        assertNotEquals(GameQuestUsageId.create(cycle, game, variantA), GameQuestUsageId.create(cycle, game, variantB))
    }

    @Test fun sameGameCanReturnOnAnotherCandidateCycle() {
        val usageA = GameQuestUsageId.create(GameCandidateCycleId("cycle-1"), game, variantA)
        val usageB = GameQuestUsageId.create(GameCandidateCycleId("cycle-2"), game, variantA)
        assertNotEquals(usageA, usageB)
    }

    @Test fun identityKeyNormalizationMatchesCatalogIdentity() {
        assertEquals("minecraft", GameIdentityKey(GameDiscovery.normalize(" Minecraft ")).value)
        assertTrue(GameQuestUsageId.create(GameCandidateCycleId("cycle-1"), game, variantA).value.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun variantFingerprintIgnoresWhitespaceOnlyDifferences() {
        val equivalent = variantA.copy(objectivePattern = "  explore   a cave ")
        assertEquals(variantA.canonicalFingerprint, equivalent.canonicalFingerprint)
        assertEquals(
            GameQuestUsageId.create(GameCandidateCycleId("cycle-1"), game, variantA),
            GameQuestUsageId.create(GameCandidateCycleId("cycle-1"), game, equivalent),
        )
    }

    @Test fun variantFingerprintCapturesStructuredSemanticAxes() {
        val differentEnvironment = variantA.copy(environment = QuestEnvironment.OUTDOOR)
        assertNotEquals(variantA.canonicalFingerprint, differentEnvironment.canonicalFingerprint)
    }
}
