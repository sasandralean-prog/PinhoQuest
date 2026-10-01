package com.pinhoquest.core.progression

import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import com.pinhoquest.domain.profile.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelPolicyTest {
    @Test
    fun levelDependsOnlyOnLifetimeXpNotSpendableBalance() {
        val profile = ProfileId("p1")
        val beforeSpend = XpLedgerSnapshot(
            listOf(
                tx("q1", profile, 100, XpTransactionType.QUEST_REWARD),
                tx("q2", profile, 100, XpTransactionType.QUEST_REWARD),
            ),
        )
        val afterSpend = XpLedgerSnapshot(
            beforeSpend.transactions + tx("research", profile, -175, XpTransactionType.FLOWER_RESEARCH),
        )

        assertEquals(3, LevelPolicyV1.levelFor(beforeSpend.lifetimeXp()))
        assertEquals(3, LevelPolicyV1.levelFor(afterSpend.lifetimeXp()))
        assertEquals(25, afterSpend.spendableXp())
    }

    @Test
    fun levelThresholdsAreStableAndMonotonic() {
        assertEquals(1, LevelPolicyV1.levelFor(0))
        assertEquals(1, LevelPolicyV1.levelFor(99))
        assertEquals(2, LevelPolicyV1.levelFor(100))
        assertEquals(3, LevelPolicyV1.levelFor(200))
        assertEquals(4, LevelPolicyV1.levelFor(400))
    }

    private fun tx(
        id: String,
        profileId: ProfileId,
        amount: Int,
        type: XpTransactionType,
    ) = XpTransaction(
        id = XpTransactionId(id),
        profileId = profileId,
        amount = amount,
        type = type,
        completionId = null,
        flowerId = null,
        createdAtEpochMillis = 1L,
    )
}
