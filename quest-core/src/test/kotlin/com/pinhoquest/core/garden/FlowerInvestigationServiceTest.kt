package com.pinhoquest.core.garden

import com.pinhoquest.domain.garden.FlowerDiscovery
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowerInvestigationServiceTest {
    private val profileId = ProfileId("p1")
    private val flowerId = FlowerId("f1")

    @Test
    fun hiddenAdvancesToHintedAndHintedToRevealedWhileSpendingXp() = runTest {
        val store = FakeStore(
            discovery = FlowerDiscovery(profileId, flowerId, FlowerDiscoveryState.HIDDEN),
            ledger = ledger(200),
        )
        var ids = 0
        val service = FlowerInvestigationService(
            store = store,
            transactionIdFactory = { XpTransactionId("tx-" + (++ids)) },
            nowEpochMillis = { 10L },
        )

        val first = service.investigate(profileId, flowerId)
        val second = service.investigate(profileId, flowerId)

        assertTrue(first is InvestigationResult.Advanced)
        assertTrue(second is InvestigationResult.Advanced)
        assertEquals(FlowerDiscoveryState.REVEALED, store.discovery.state)
        assertEquals(50, store.ledger.spendableXp())
        assertEquals(200, store.ledger.lifetimeXp())
    }

    @Test
    fun insufficientSpendableXpDoesNotMutateDiscoveryOrLedger() = runTest {
        val initial = FlowerDiscovery(profileId, flowerId, FlowerDiscoveryState.HIDDEN)
        val store = FakeStore(initial, ledger(50))
        val service = FlowerInvestigationService(
            store = store,
            transactionIdFactory = { XpTransactionId("tx") },
            nowEpochMillis = { 10L },
        )

        val result = service.investigate(profileId, flowerId)

        assertTrue(result is InvestigationResult.InsufficientXp)
        assertEquals(initial, store.discovery)
        assertEquals(50, store.ledger.spendableXp())
    }

    @Test
    fun revealedFlowerDoesNotSpendMoreXp() = runTest {
        val initial = FlowerDiscovery(profileId, flowerId, FlowerDiscoveryState.REVEALED)
        val store = FakeStore(initial, ledger(100))
        val service = FlowerInvestigationService(
            store = store,
            transactionIdFactory = { XpTransactionId("tx") },
            nowEpochMillis = { 10L },
        )

        val result = service.investigate(profileId, flowerId)

        assertTrue(result is InvestigationResult.AlreadyRevealed)
        assertEquals(100, store.ledger.spendableXp())
    }

    private fun ledger(amount: Int): XpLedgerSnapshot = XpLedgerSnapshot(
        listOf(
            XpTransaction(
                id = XpTransactionId("earn"),
                profileId = profileId,
                amount = amount,
                type = XpTransactionType.QUEST_REWARD,
                completionId = null,
                flowerId = null,
                createdAtEpochMillis = 1L,
            ),
        ),
    )

    private class FakeStore(
        var discovery: FlowerDiscovery,
        var ledger: XpLedgerSnapshot,
    ) : FlowerInvestigationStore {
        override suspend fun read(
            profileId: ProfileId,
            flowerId: FlowerId,
        ): FlowerInvestigationSnapshot = FlowerInvestigationSnapshot(discovery, ledger)

        override suspend fun advance(
            profileId: ProfileId,
            flowerId: FlowerId,
            expectedState: FlowerDiscoveryState,
            nextState: FlowerDiscoveryState,
            transaction: XpTransaction,
        ): Boolean {
            if (discovery.profileId != profileId || discovery.flowerId != flowerId) return false
            if (discovery.state != expectedState) return false
            discovery = discovery.copy(state = nextState)
            ledger = XpLedgerSnapshot(ledger.transactions + transaction)
            return true
        }
    }
}
