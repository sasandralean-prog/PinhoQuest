package com.pinhoquest.data.bootstrap

import androidx.room.withTransaction
import com.pinhoquest.data.catalog.EmbeddedCatalogLoader
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.CatalogEntryEntity
import com.pinhoquest.data.db.entity.CatalogPackEntity
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.FlowerDefinitionEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.db.entity.GoalDefinitionEntity
import com.pinhoquest.data.db.entity.RarityScaleEntity
import com.pinhoquest.data.progression.EmbeddedGoalLoader
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.GoalRule
import org.json.JSONArray

class ProgressionBootstrapper(
    private val database: PinhoQuestDatabase,
) {
    suspend fun ensureSeeded(profileId: ProfileId) {
        database.withTransaction {
            val garden = database.gardenDao()
            val progression = database.progressionDao()

            if (progression.goalDefinitions().isEmpty()) {
                val goals = EmbeddedGoalLoader.loadV1()
                progression.upsertGoalDefinitions(
                    goals.goals.map { goal ->
                        val rule = goal.rule
                        GoalDefinitionEntity(
                            goalId = goal.id.value,
                            title = goal.title,
                            ruleType = when (rule) {
                                is GoalRule.CompletionCount -> "COMPLETION_COUNT"
                                is GoalRule.CategoryCompletion -> "CATEGORY_COMPLETION"
                                is GoalRule.DifficultyCompletion -> "DIFFICULTY_COMPLETION"
                                is GoalRule.BonusObjectiveCount -> "BONUS_OBJECTIVE_COUNT"
                                is GoalRule.LifetimeXpMilestone -> "LIFETIME_XP_MILESTONE"
                            },
                            target = rule.target,
                            category = (rule as? GoalRule.CategoryCompletion)?.category?.name,
                            difficulty = (rule as? GoalRule.DifficultyCompletion)?.difficulty?.name,
                            version = goals.version,
                        )
                    },
                )
            }

            if (garden.latestCatalogPack(profileId.value) == null) {
                val loaded = EmbeddedCatalogLoader.loadCollectionI()
                check(loaded.validation.isValid) {
                    "Embedded Collection I invalid: " + loaded.validation.errors.joinToString()
                }
                val scale = loaded.scale
                garden.upsertRarityScale(
                    RarityScaleEntity(
                        scaleVersion = scale.version,
                        referenceMinPopulation = scale.referenceMinPopulation,
                        referenceMaxPopulation = scale.referenceMaxPopulation,
                        countingBasis = scale.requiredCountingBasis.name,
                        methodology = "Versioned absolute mature-individual log10 scale",
                    ),
                )
                val pack = loaded.pack
                garden.insertCatalogPack(
                    CatalogPackEntity(
                        packId = pack.id.value,
                        profileId = profileId.value,
                        collectionIndex = 1,
                        version = pack.version,
                        generatedAtEpochMillis = pack.generatedAtEpochMillis,
                        rarityScaleVersion = scale.version,
                        sourceUrisJson = jsonArray(pack.sourceUris),
                        integrityHash = pack.integrityHash,
                    ),
                )
                garden.upsertFlowerDefinitions(
                    pack.entries.map { entry ->
                        val flower = entry.flower
                        val estimate = flower.populationEstimate
                        FlowerDefinitionEntity(
                            flowerId = flower.id.value,
                            commonName = flower.commonName,
                            scientificName = flower.scientificName,
                            description = flower.description,
                            rarity = flower.rarity.name,
                            rarityScaleVersion = flower.rarityScaleVersion,
                            estimatedIndividuals = estimate?.estimatedIndividuals,
                            lowerBound = estimate?.lowerBound,
                            upperBound = estimate?.upperBound,
                            estimateDateEpochMillis = estimate?.estimateDateEpochMillis,
                            evidenceSourceUrisJson = jsonArray(estimate?.evidenceSourceUris.orEmpty()),
                            confidence = estimate?.confidence?.name,
                            countingBasis = estimate?.countingBasis?.name,
                        )
                    },
                )
                garden.upsertCatalogEntries(
                    pack.entries.mapIndexed { index, entry ->
                        CatalogEntryEntity(
                            packId = pack.id.value,
                            flowerId = entry.flower.id.value,
                            slotIndex = index,
                            eligible = entry.eligible,
                        )
                    },
                )
                pack.entries.forEach { entry ->
                    garden.upsertDiscovery(
                        FlowerDiscoveryEntity(
                            profileId = profileId.value,
                            flowerId = entry.flower.id.value,
                            state = FlowerDiscoveryState.HIDDEN.name,
                        ),
                    )
                }
            }

            if (progression.datasetMetadata(profileId.value) == null) {
                progression.upsertDatasetMetadata(
                    DatasetMetadataEntity(
                        profileId = profileId.value,
                        datasetRevision = 0,
                        schemaVersion = 2,
                        lastModifiedAtEpochMillis = 0,
                    ),
                )
            }
        }
    }

    private fun jsonArray(values: Set<String>): String =
        JSONArray(values.sorted()).toString()
}
