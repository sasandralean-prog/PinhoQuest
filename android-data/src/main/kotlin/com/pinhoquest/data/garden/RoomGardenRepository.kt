package com.pinhoquest.data.garden

import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.FlowerDefinitionEntity
import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.EstimateConfidence
import com.pinhoquest.domain.garden.FlowerAcquisition
import com.pinhoquest.domain.garden.FlowerDefinition
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.XpLedgerSnapshot
import com.pinhoquest.domain.progression.XpTransaction
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.progression.XpTransactionType
import org.json.JSONArray

data class GardenFlowerSnapshot(
    val definition: FlowerDefinition,
    val discoveryState: FlowerDiscoveryState,
    val acquisition: FlowerAcquisition?,
    val questTitle: String?,
)

data class GardenSnapshot(
    val flowers: List<GardenFlowerSnapshot>,
    val xpLedger: XpLedgerSnapshot,
)

class RoomGardenRepository(
    private val database: PinhoQuestDatabase,
) {
    suspend fun snapshot(profileId: ProfileId): GardenSnapshot {
        val garden = database.gardenDao()
        val progression = database.progressionDao()
        val questRepository = RoomQuestRepository(database.questDao())
        val pack = garden.latestCatalogPack(profileId.value)
        val entries = pack?.let { garden.catalogEntries(it.packId) }.orEmpty()
        val definitions = garden.flowerDefinitions(entries.map { it.flowerId })
            .associateBy { it.flowerId }
        val discoveries = garden.discoveries(profileId.value).associateBy { it.flowerId }
        val acquisitions = garden.acquisitions(profileId.value).associateBy { it.flowerId }

        val flowers = entries.mapNotNull { entry ->
            val definition = definitions[entry.flowerId] ?: return@mapNotNull null
            val acquisitionEntity = acquisitions[entry.flowerId]
            val acquisition = acquisitionEntity?.let {
                FlowerAcquisition(
                    profileId = ProfileId(it.profileId),
                    flowerId = FlowerId(it.flowerId),
                    completionId = CompletionId(it.completionId),
                    acquiredAtEpochMillis = it.acquiredAtEpochMillis,
                    xpAward = it.xpAward,
                )
            }
            val questTitle = acquisitionEntity?.let {
                progression.completionById(it.completionId)?.let { completion ->
                    questRepository.get(QuestId(completion.questId))?.title
                }
            }
            GardenFlowerSnapshot(
                definition = definition.toDomain(),
                discoveryState = discoveries[entry.flowerId]
                    ?.state
                    ?.let(FlowerDiscoveryState::valueOf)
                    ?: FlowerDiscoveryState.HIDDEN,
                acquisition = acquisition,
                questTitle = questTitle,
            )
        }

        val ledger = XpLedgerSnapshot(
            progression.xpTransactions(profileId.value).map { row ->
                XpTransaction(
                    id = XpTransactionId(row.transactionId),
                    profileId = ProfileId(row.profileId),
                    amount = row.amount,
                    type = XpTransactionType.valueOf(row.type),
                    completionId = row.completionId?.let(::CompletionId),
                    flowerId = row.flowerId,
                    createdAtEpochMillis = row.createdAtEpochMillis,
                )
            },
        )
        return GardenSnapshot(flowers = flowers, xpLedger = ledger)
    }

    private fun FlowerDefinitionEntity.toDomain(): FlowerDefinition {
        val estimate = if (confidence != null && countingBasis != null) {
            GlobalPopulationEstimate(
                estimatedIndividuals = estimatedIndividuals,
                lowerBound = lowerBound,
                upperBound = upperBound,
                estimateDateEpochMillis = estimateDateEpochMillis,
                evidenceSourceUris = decodeArray(evidenceSourceUrisJson),
                confidence = EstimateConfidence.valueOf(confidence),
                countingBasis = CountingBasis.valueOf(countingBasis),
            )
        } else {
            null
        }
        return FlowerDefinition(
            id = FlowerId(flowerId),
            commonName = commonName,
            scientificName = scientificName,
            description = description,
            rarity = FlowerRarity.valueOf(rarity),
            rarityScaleVersion = rarityScaleVersion,
            populationEstimate = estimate,
        )
    }

    private fun decodeArray(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        val json = JSONArray(raw)
        return buildSet {
            for (index in 0 until json.length()) {
                add(json.getString(index))
            }
        }
    }
}
