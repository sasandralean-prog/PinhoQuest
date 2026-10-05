package com.pinhoquest.data.research

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.core.research.GameAvailability
import com.pinhoquest.core.research.GameCatalogSnapshot
import com.pinhoquest.core.research.GameDiscovery
import com.pinhoquest.core.research.ResearchProvenance
import com.pinhoquest.data.db.PinhoQuestDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomGameDiscoveryCatalogStoreTest {
    private lateinit var database: PinhoQuestDatabase

    @Before
    fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            PinhoQuestDatabase::class.java,
        ).build()
    }

    @After
    fun after() {
        database.close()
    }

    @Test
    fun normalizedSnapshotRoundTripPreservesFactsAndProvenance() = runBlocking {
        val store = RoomGameDiscoveryCatalogStore(database)
        val item = GameDiscovery(
            canonicalName = "Example",
            platforms = setOf("Android", "PC"),
            genres = setOf("RPG"),
            availability = GameAvailability.UNKNOWN,
            provenance = setOf(ResearchProvenance("https://example.test", 100L)),
        )
        val snapshot = GameCatalogSnapshot(100L, 1_100L, listOf(item))

        store.write(snapshot)
        val restored = store.read()

        assertNotNull(restored)
        assertEquals(snapshot, restored)
    }

    @Test
    fun emptySuccessfulSnapshotIsPersistedAsSemanticNoNewOptions() = runBlocking {
        val store = RoomGameDiscoveryCatalogStore(database)
        store.write(GameCatalogSnapshot(100L, 1_100L, emptyList()))

        val restored = store.read()

        assertNotNull(restored)
        assertEquals(emptyList<GameDiscovery>(), restored?.items)
    }
}
