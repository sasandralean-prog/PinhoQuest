package com.pinhoquest.data.research

import androidx.room.withTransaction
import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.GameCatalogSnapshot
import com.pinhoquest.core.research.GameDiscovery
import com.pinhoquest.core.research.GameDiscoveryCatalogStore
import com.pinhoquest.core.research.GameQuestUsage
import com.pinhoquest.core.research.GameQuestUsageId
import com.pinhoquest.core.research.GameCandidateCycleId
import com.pinhoquest.core.research.GameIdentityKey
import com.pinhoquest.core.research.GameQuestUsageStore
import com.pinhoquest.core.research.ResearchProvenance
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.GameCatalogSnapshotEntity
import com.pinhoquest.data.db.entity.GameDiscoveryEntity
import com.pinhoquest.data.db.entity.GameQuestUsageEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val SNAPSHOT_ID = "default"

@Serializable
private data class PersistedProvenance(
    val sourceUri: String,
    val researchedAtEpochMillis: Long,
)

class RoomGameDiscoveryCatalogStore(
    private val database: PinhoQuestDatabase,
    private val json: Json = Json,
) : GameDiscoveryCatalogStore, GameQuestUsageStore {
    override suspend fun read(): GameCatalogSnapshot? {
        val metadata = database.researchCatalogDao().snapshot(SNAPSHOT_ID) ?: return null
        val items = database.researchCatalogDao().discoveries().map { it.toDomain() }
        return GameCatalogSnapshot(
            researchedAtEpochMillis = metadata.researchedAtEpochMillis,
            expiresAtEpochMillis = metadata.expiresAtEpochMillis,
            items = items,
            cycleId = GameCandidateCycleId(metadata.cycleId),
        )
    }

    override suspend fun write(snapshot: GameCatalogSnapshot) {
        database.withTransaction {
            val dao = database.researchCatalogDao()
            dao.clearDiscoveries()
            dao.clearSnapshots()
            dao.upsertDiscoveries(snapshot.items.map { it.toEntity() })
            dao.upsertSnapshot(
                GameCatalogSnapshotEntity(
                    snapshotId = SNAPSHOT_ID,
                    cycleId = snapshot.cycleId.value,
                    researchedAtEpochMillis = snapshot.researchedAtEpochMillis,
                    expiresAtEpochMillis = snapshot.expiresAtEpochMillis,
                ),
            )
        }
    }

    override suspend fun usedIdentityKeys(cycleId: GameCandidateCycleId): Set<GameIdentityKey> =
        database.researchCatalogDao()
            .usedGameIdentityKeys(cycleId.value)
            .map(::GameIdentityKey)
            .toSet()

    override suspend fun record(usage: GameQuestUsage) {
        database.researchCatalogDao().upsertUsage(
            GameQuestUsageEntity(
                usageId = usage.usageId.value,
                cycleId = usage.cycleId.value,
                gameIdentityKey = usage.gameIdentity.value,
                variantFingerprint = usage.variantFingerprint,
                usedAtEpochMillis = usage.usedAtEpochMillis,
            ),
        )
    }

    override suspend fun clearCycle(cycleId: GameCandidateCycleId) {
        database.researchCatalogDao().clearUsageCycle(cycleId.value)
    }

    private fun GameDiscovery.toEntity() = GameDiscoveryEntity(
        identityKey = identityKey,
        canonicalName = canonicalName,
        platformsJson = json.encodeToString(platforms.toList()),
        genresJson = json.encodeToString(genres.toList()),
        availability = availability.name,
        provenanceJson = json.encodeToString(
            provenance.map { PersistedProvenance(it.sourceUri, it.researchedAtEpochMillis) },
        ),
    )

    private fun GameDiscoveryEntity.toDomain() = GameDiscovery(
        canonicalName = canonicalName,
        platforms = json.decodeFromString<List<String>>(platformsJson).toSet(),
        genres = json.decodeFromString<List<String>>(genresJson).toSet(),
        availability = GameAvailability.valueOf(availability),
        provenance = json.decodeFromString<List<PersistedProvenance>>(provenanceJson)
            .map { ResearchProvenance(it.sourceUri, it.researchedAtEpochMillis) }
            .toSet(),
    )
}
