package com.pinhoquest.core.progression

import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.QuestCompletionFacts
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.profile.ProfileId
import org.junit.Assert.assertEquals
import org.junit.Test

class XpPolicyTest {
    @Test
    fun earnedXpIncreasesLifetimeAndSpendableWhileResearchOnlySpendsBalance() {
        val profileId = ProfileId("p1")
        val ledger = XpLedgerSnapshot(
            transactions = listOf(
                tx("earn-1", profileId, 50, XpTransactionType.QUEST_REWARD),
                tx("spend-1", profileId, -25, XpTransactionType.FLOWER_RESEARCH),
                tx("earn-2", profileId, 10, XpTransactionType.QUEST_REWARD),
            ),
        )

        assertEquals(60, ledger.lifetimeXp())
        assertEquals(35, ledger.spendableXp())
    }

    @Test
    fun optionalObjectiveBonusIsBoundedByPolicy() {
        val facts = completion(
            difficulty = QuestDifficulty.MEDIUM,
            bonusObjectiveCount = 8,
            completedBonusCount = 8,
        )

        assertEquals(60, XpPolicyV1.awardFor(facts))
    }

    @Test
    fun incompleteMainObjectivesDoNotIncreaseAward() {
        val facts = completion(
            difficulty = QuestDifficulty.EASY,
            completedMainCount = 1,
            mainObjectiveCount = 2,
        )

        assertEquals(25, XpPolicyV1.awardFor(facts))
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

    private fun completion(
        difficulty: QuestDifficulty,
        mainObjectiveCount: Int = 1,
        completedMainCount: Int = mainObjectiveCount,
        bonusObjectiveCount: Int = 0,
        completedBonusCount: Int = bonusObjectiveCount,
    ): QuestCompletionFacts {
        val main = (1..mainObjectiveCount).map { ObjectiveId("m-$it") }.toSet()
        val bonus = (1..bonusObjectiveCount).map { ObjectiveId("b-$it") }.toSet()
        val completed = main.take(completedMainCount).toSet() + bonus.take(completedBonusCount)
        return QuestCompletionFacts(
            completionId = CompletionId("completion"),
            questId = QuestId("quest"),
            category = QuestCategory.CODING,
            difficulty = difficulty,
            mainObjectiveIds = main,
            bonusObjectiveIds = bonus,
            completedObjectiveIds = completed,
        )
    }
}
