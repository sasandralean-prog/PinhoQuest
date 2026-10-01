package com.pinhoquest.core.garden

import com.pinhoquest.domain.garden.FlowerDiscovery
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType

data class FlowerInvestigationSnapshot(
    val discovery: FlowerDiscovery,
    val ledger: XpLedgerSnapshot,
)

interface FlowerInvestigationStore {
    suspend fun read(
        profileId: ProfileId,
        flowerId: FlowerId,
    ): FlowerInvestigationSnapshot?

    suspend fun advance(
        profileId: ProfileId,
        flowerId: FlowerId,
        expectedState: FlowerDiscoveryState,
        nextState: FlowerDiscoveryState,
        transaction: XpTransaction,
    ): Boolean
}

sealed interface InvestigationResult {
    data class Advanced(
        val discovery: FlowerDiscovery,
        val xpSpent: Int,
    ) : InvestigationResult

    data class InsufficientXp(
        val required: Int,
        val available: Int,
    ) : InvestigationResult

    data object AlreadyRevealed : InvestigationResult
    data object AlreadyCollected : InvestigationResult
    data object NotFound : InvestigationResult
    data object Conflict : InvestigationResult
}

class FlowerInvestigationService(
    private val store: FlowerInvestigationStore,
    private val transactionIdFactory: () -> XpTransactionId,
    private val nowEpochMillis: () -> Long,
    private val costPolicy: FlowerInvestigationCostPolicy = FlowerInvestigationCostPolicyV1,
) {
    suspend fun investigate(
        profileId: ProfileId,
        flowerId: FlowerId,
    ): InvestigationResult {
        val snapshot = store.read(profileId, flowerId) ?: return InvestigationResult.NotFound
        val current = snapshot.discovery.state
        val next = when (current) {
            FlowerDiscoveryState.HIDDEN -> FlowerDiscoveryState.HINTED
            FlowerDiscoveryState.HINTED -> FlowerDiscoveryState.REVEALED
            FlowerDiscoveryState.REVEALED -> return InvestigationResult.AlreadyRevealed
            FlowerDiscoveryState.COLLECTED -> return InvestigationResult.AlreadyCollected
        }

        val cost = costPolicy.costFor(current)
        val available = snapshot.ledger.spendableXp()
        if (available < cost) {
            return InvestigationResult.InsufficientXp(required = cost, available = available)
        }

        val transaction = XpTransaction(
            id = transactionIdFactory(),
            profileId = profileId,
            amount = -cost,
            type = XpTransactionType.FLOWER_RESEARCH,
            completionId = null,
            flowerId = flowerId.value,
            createdAtEpochMillis = nowEpochMillis(),
        )

        val committed = store.advance(
            profileId = profileId,
            flowerId = flowerId,
            expectedState = current,
            nextState = next,
            transaction = transaction,
        )
        if (!committed) return InvestigationResult.Conflict

        return InvestigationResult.Advanced(
            discovery = snapshot.discovery.copy(state = next),
            xpSpent = cost,
        )
    }
}

fun interface FlowerInvestigationCostPolicy {
    fun costFor(state: FlowerDiscoveryState): Int
}

object FlowerInvestigationCostPolicyV1 : FlowerInvestigationCostPolicy {
    const val VERSION = 1
    private const val COST_PER_STEP = 75

    override fun costFor(state: FlowerDiscoveryState): Int = when (state) {
        FlowerDiscoveryState.HIDDEN,
        FlowerDiscoveryState.HINTED,
        -> COST_PER_STEP
        FlowerDiscoveryState.REVEALED,
        FlowerDiscoveryState.COLLECTED,
        -> 0
    }
}
