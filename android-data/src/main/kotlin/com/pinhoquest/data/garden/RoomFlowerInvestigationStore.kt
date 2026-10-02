package com.pinhoquest.data.garden

import androidx.room.withTransaction
import com.pinhoquest.core.garden.FlowerInvestigationSnapshot
import com.pinhoquest.core.garden.FlowerInvestigationStore
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.db.entity.XpTransactionEntity
import com.pinhoquest.domain.garden.FlowerDiscovery
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType

class RoomFlowerInvestigationStore(
    private val database: PinhoQuestDatabase,
) : FlowerInvestigationStore {
    override suspend fun read(
        profileId: ProfileId,
        flowerId: FlowerId,
    ): FlowerInvestigationSnapshot? {
        val discovery = database.gardenDao().discovery(profileId.value, flowerId.value)
            ?: return null
        val ledger = XpLedgerSnapshot(
            database.progressionDao().xpTransactions(profileId.value).map { it.toDomain() },
        )
        return FlowerInvestigationSnapshot(
            discovery = FlowerDiscovery(
                profileId = profileId,
                flowerId = flowerId,
                state = FlowerDiscoveryState.valueOf(discovery.state),
            ),
            ledger = ledger,
        )
    }

    override suspend fun advance(
        profileId: ProfileId,
        flowerId: FlowerId,
        expectedState: FlowerDiscoveryState,
        nextState: FlowerDiscoveryState,
        transaction: XpTransaction,
    ): Boolean = database.withTransaction {
        val garden = database.gardenDao()
        val progression = database.progressionDao()
        val current = garden.discovery(profileId.value, flowerId.value)
            ?: return@withTransaction false
        if (current.state != expectedState.name) return@withTransaction false

        val available = progression.xpTransactions(profileId.value).sumOf { it.amount }
        if (available + transaction.amount < 0) return@withTransaction false

        progression.insertXpTransaction(transaction.toEntity())
        garden.upsertDiscovery(
            FlowerDiscoveryEntity(
                profileId = profileId.value,
                flowerId = flowerId.value,
                state = nextState.name,
            ),
        )
        val metadata = progression.datasetMetadata(profileId.value)
            ?: DatasetMetadataEntity(
                profileId = profileId.value,
                datasetRevision = 0,
                schemaVersion = 2,
                lastModifiedAtEpochMillis = 0,
            )
        progression.upsertDatasetMetadata(
            metadata.copy(
                datasetRevision = metadata.datasetRevision + 1,
                lastModifiedAtEpochMillis = transaction.createdAtEpochMillis,
            ),
        )
        true
    }

    private fun XpTransactionEntity.toDomain() = XpTransaction(
        id = XpTransactionId(transactionId),
        profileId = ProfileId(profileId),
        amount = amount,
        type = XpTransactionType.valueOf(type),
        completionId = completionId?.let(::CompletionId),
        flowerId = flowerId,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private fun XpTransaction.toEntity() = XpTransactionEntity(
        transactionId = id.value,
        profileId = profileId.value,
        amount = amount,
        type = type.name,
        completionId = completionId?.value,
        flowerId = flowerId,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
