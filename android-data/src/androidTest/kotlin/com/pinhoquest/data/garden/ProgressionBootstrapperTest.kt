package com.pinhoquest.data.garden

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.core.garden.FlowerInvestigationService
import com.pinhoquest.core.garden.InvestigationResult
import com.pinhoquest.data.bootstrap.ProgressionBootstrapper
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.db.entity.ProfileEntity
import com.pinhoquest.data.db.entity.XpTransactionEntity
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.XpTransactionId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressionBootstrapperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun seedIsIdempotentAndCreatesCatalogGoalsDiscoveriesAndMetadata() = runBlocking {
        val db = database()
        val profileId = ProfileId("profile-seed")
        db.profileDao().upsert(ProfileEntity(profileId.value, "Rafa", 1L))

        val bootstrapper = ProgressionBootstrapper(db)
        bootstrapper.ensureSeeded(profileId)
        bootstrapper.ensureSeeded(profileId)

        assertEquals(1, db.gardenDao().catalogPacks(profileId.value).size)
        val pack = db.gardenDao().latestCatalogPack(profileId.value)!!
        assertEquals(24, db.gardenDao().catalogEntries(pack.packId).size)
        assertEquals(24, db.gardenDao().discoveries(profileId.value).size)
        assertTrue(db.progressionDao().goalDefinitions().isNotEmpty())
        assertEquals(0L, db.progressionDao().datasetMetadata(profileId.value)?.datasetRevision)
        db.close()
    }

    @Test
    fun investigationSpendsBalanceButKeepsLifetimeXp() = runBlocking {
        val db = database()
        val profileId = ProfileId("profile-investigate")
        db.profileDao().upsert(ProfileEntity(profileId.value, "Rafa", 1L))
        ProgressionBootstrapper(db).ensureSeeded(profileId)
        db.progressionDao().insertXpTransaction(
            XpTransactionEntity(
                transactionId = "earned-150",
                profileId = profileId.value,
                amount = 150,
                type = "QUEST_REWARD",
                completionId = null,
                flowerId = null,
                createdAtEpochMillis = 2L,
            ),
        )
        val flowerId = FlowerId(
            db.gardenDao().catalogEntries(
                db.gardenDao().latestCatalogPack(profileId.value)!!.packId,
            ).first().flowerId,
        )
        var ids = 0
        val service = FlowerInvestigationService(
            store = RoomFlowerInvestigationStore(db),
            transactionIdFactory = { XpTransactionId("research-" + (++ids)) },
            nowEpochMillis = { 10L + ids },
        )

        val first = service.investigate(profileId, flowerId)
        val second = service.investigate(profileId, flowerId)
        val snapshot = RoomGardenRepository(db).snapshot(profileId)

        assertTrue(first is InvestigationResult.Advanced)
        assertTrue(second is InvestigationResult.Advanced)
        assertEquals(FlowerDiscoveryState.REVEALED, snapshot.flowers.first { it.definition.id == flowerId }.discoveryState)
        assertEquals(150, snapshot.xpLedger.lifetimeXp())
        assertEquals(0, snapshot.xpLedger.spendableXp())
        db.close()
    }

    private fun database(): PinhoQuestDatabase =
        Room.inMemoryDatabaseBuilder(context, PinhoQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
