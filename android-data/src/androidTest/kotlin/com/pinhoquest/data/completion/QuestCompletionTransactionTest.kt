package com.pinhoquest.data.completion

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.core.completion.CompletionResult
import com.pinhoquest.core.completion.QuestCompletionService
import com.pinhoquest.core.reward.RewardEngine
import com.pinhoquest.core.reward.UniformIndexSource
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.CatalogEntryEntity
import com.pinhoquest.data.db.entity.CatalogPackEntity
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.FlowerDefinitionEntity
import com.pinhoquest.data.db.entity.FlowerDiscoveryEntity
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.repository.RoomQuestSessionRepository
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.profile.UserProfile
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestObjective
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import com.pinhoquest.domain.reward.RewardOpportunityId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestCompletionTransactionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun failureAfterWritesRollsBackCompletionXpRewardAcquisitionAndState() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PinhoQuestDatabase::class.java).build()
        seed(db)

        val store = RoomQuestCompletionStore(
            database = db,
            failureInjector = CompletionFailureInjector {
                throw IllegalStateException("poison-before-commit")
            },
        )
        val service = QuestCompletionService(
            store = store,
            rewardEngine = RewardEngine(UniformIndexSource { 0 }),
            completionIdFactory = { CompletionId("completion-1") },
            xpTransactionIdFactory = { XpTransactionId("xp-1") },
            rewardOpportunityIdFactory = { RewardOpportunityId("reward-1") },
            nowEpochMillis = { 1_000L },
        )

        val result = service.complete(
            sessionId = QuestSessionId("session-1"),
            completedObjectives = setOf(ObjectiveId("main")),
        )

        assertTrue(result is CompletionResult.TechnicalFailure)
        assertNull(db.progressionDao().completionBySession("session-1"))
        assertTrue(db.progressionDao().xpTransactions("profile-1").isEmpty())
        assertTrue(db.gardenDao().acquisitions("profile-1").isEmpty())
        assertNull(db.gardenDao().rewardOpportunityByCompletion("completion-1"))
        assertEquals(0L, db.progressionDao().datasetMetadata("profile-1")?.datasetRevision)
        assertEquals(
            QuestState.ACTIVE,
            RoomQuestSessionRepository(db.questSessionDao()).get(QuestSessionId("session-1"))?.state,
        )
        assertEquals(
            QuestState.ACTIVE,
            RoomQuestRepository(db.questDao()).get(QuestId("quest-1"))?.state,
        )
        db.close()
    }

    @Test
    fun successfulRetryReturnsSameCompletionWithoutDuplicateXpOrFlower() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, PinhoQuestDatabase::class.java).build()
        seed(db)
        val service = QuestCompletionService(
            store = RoomQuestCompletionStore(db),
            rewardEngine = RewardEngine(UniformIndexSource { 0 }),
            completionIdFactory = { CompletionId("completion-1") },
            xpTransactionIdFactory = { XpTransactionId("xp-1") },
            rewardOpportunityIdFactory = { RewardOpportunityId("reward-1") },
            nowEpochMillis = { 1_000L },
        )

        val first = service.complete(QuestSessionId("session-1"), setOf(ObjectiveId("main")))
        val second = service.complete(QuestSessionId("session-1"), setOf(ObjectiveId("main")))

        assertTrue(first is CompletionResult.Success)
        assertTrue(second is CompletionResult.Success)
        assertEquals(1, db.progressionDao().xpTransactions("profile-1").size)
        assertEquals(1, db.gardenDao().acquisitions("profile-1").size)
        assertEquals(1L, db.progressionDao().datasetMetadata("profile-1")?.datasetRevision)
        db.close()
    }

    private suspend fun seed(db: PinhoQuestDatabase) {
        val profile = UserProfile(
            id = ProfileId("profile-1"),
            gardenOwnerName = GardenOwnerName.create("Rafa").getOrThrow(),
            createdAtEpochMillis = 1L,
        )
        RoomProfileRepository(db.profileDao()).upsert(profile)

        val quest = Quest(
            id = QuestId("quest-1"),
            title = "Quest",
            description = "Desc",
            objectives = listOf(QuestObjective(ObjectiveId("main"), "Faça", optional = false)),
            category = QuestCategory.CODING,
            environment = QuestEnvironment.WINDOWS,
            estimatedDuration = EstimatedDuration(15, 30),
            difficulty = QuestDifficulty.MEDIUM,
            state = QuestState.ACTIVE,
        )
        RoomQuestRepository(db.questDao()).upsert(quest)
        RoomQuestSessionRepository(db.questSessionDao()).upsert(
            QuestSession(
                id = QuestSessionId("session-1"),
                questId = quest.id,
                state = QuestState.ACTIVE,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
            ),
        )
        db.progressionDao().upsertDatasetMetadata(
            DatasetMetadataEntity(
                profileId = "profile-1",
                datasetRevision = 0L,
                schemaVersion = 2,
                lastModifiedAtEpochMillis = 1L,
            ),
        )
        db.gardenDao().insertCatalogPack(
            CatalogPackEntity(
                packId = "pack-1",
                profileId = "profile-1",
                collectionIndex = 1,
                version = 1,
                generatedAtEpochMillis = 1L,
                rarityScaleVersion = 1,
                sourceUrisJson = "[]",
                integrityHash = "hash",
            ),
        )
        db.gardenDao().upsertFlowerDefinitions(
            listOf(
                FlowerDefinitionEntity(
                    flowerId = "flower-1",
                    commonName = "Flor",
                    scientificName = "Species flower",
                    description = "",
                    rarity = "UNKNOWN",
                    rarityScaleVersion = null,
                    estimatedIndividuals = null,
                    lowerBound = null,
                    upperBound = null,
                    estimateDateEpochMillis = null,
                    evidenceSourceUrisJson = "[]",
                    confidence = null,
                    countingBasis = null,
                ),
            ),
        )
        db.gardenDao().upsertCatalogEntries(
            listOf(CatalogEntryEntity("pack-1", "flower-1", 0, true)),
        )
        db.gardenDao().upsertDiscovery(
            FlowerDiscoveryEntity("profile-1", "flower-1", "HIDDEN"),
        )
    }
}
