package com.pinhoquest.domain.progression

import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestId

@JvmInline value class CompletionId(val value: String)
@JvmInline value class XpTransactionId(val value: String)

enum class XpTransactionType {
    QUEST_REWARD,
    FLOWER_RESEARCH,
    ADJUSTMENT,
}

data class XpTransaction(
    val id: XpTransactionId,
    val profileId: ProfileId,
    val amount: Int,
    val type: XpTransactionType,
    val completionId: CompletionId?,
    val flowerId: String?,
    val createdAtEpochMillis: Long,
)

data class XpLedgerSnapshot(
    val transactions: List<XpTransaction>,
) {
    fun lifetimeXp(): Int =
        transactions.sumOf { tx ->
            when {
                tx.type == XpTransactionType.QUEST_REWARD && tx.amount > 0 -> tx.amount
                tx.type == XpTransactionType.ADJUSTMENT && tx.amount > 0 -> tx.amount
                else -> 0
            }
        }

    fun spendableXp(): Int = transactions.sumOf(XpTransaction::amount)
}

data class QuestCompletionFacts(
    val completionId: CompletionId,
    val questId: QuestId,
    val category: QuestCategory,
    val difficulty: QuestDifficulty,
    val mainObjectiveIds: Set<ObjectiveId>,
    val bonusObjectiveIds: Set<ObjectiveId>,
    val completedObjectiveIds: Set<ObjectiveId>,
) {
    val completedMainObjectiveCount: Int
        get() = mainObjectiveIds.count(completedObjectiveIds::contains)

    val completedBonusObjectiveCount: Int
        get() = bonusObjectiveIds.count(completedObjectiveIds::contains)
}
