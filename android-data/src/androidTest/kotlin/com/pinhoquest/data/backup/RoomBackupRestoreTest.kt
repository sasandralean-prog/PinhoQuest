package com.pinhoquest.data.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.settings.AppPreferencesStore
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.domain.backup.BackupReadResult
import com.pinhoquest.domain.backup.BackupSnapshotBuildResult
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.profile.UserProfile
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestObjective
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomBackupRestoreTest {
    private lateinit var context: Context
    private lateinit var db: PinhoQuestDatabase
    private lateinit var preferencesScope: CoroutineScope
    private lateinit var preferencesStore: AppPreferencesStore
    private lateinit var dbName: String
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbName = "pinho-quest-backup-test-${UUID.randomUUID()}.db"
        preferencesScope = CoroutineScope(Job() + Dispatchers.IO)
        preferencesFile = File(
            context.filesDir,
            "backup-test-${UUID.randomUUID()}.preferences_pb",
        )
        val dataStore = PreferenceDataStoreFactory.create(
            scope = preferencesScope,
            produceFile = { preferencesFile },
        )
        preferencesStore = AppPreferencesStore(dataStore)
        db = Room.databaseBuilder(
            context,
            PinhoQuestDatabase::class.java,
            dbName,
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
        preferencesScope.cancel()
        context.deleteDatabase(dbName)
        preferencesFile.delete()
    }

    @Test
    fun snapshotCapturesRoomAndPreferencesAndCodecRoundTrips() = runBlocking {
        seedState(
            gardenName = "Jardim Original",
            questTitle = "Quest Original",
            theme = ThemePreference.DARK,
            fontScale = 1.25f,
        )

        val result = RoomBackupSnapshotBuilder(
            database = db,
            preferencesStore = preferencesStore,
            clock = Clock.fixed(
                Instant.ofEpochMilli(10_000L),
                ZoneOffset.UTC,
            ),
        ).build("0.1.0")

        assertTrue(result is BackupSnapshotBuildResult.Ready)
        val snapshot = (result as BackupSnapshotBuildResult.Ready).snapshot
        assertEquals("Jardim Original", snapshot.profile.gardenOwnerName)
        assertEquals("Quest Original", snapshot.quests.single().title)
        assertEquals(ThemePreference.DARK.name, snapshot.preferences.theme)
        assertEquals(1.25f, snapshot.preferences.fontScale)
        assertEquals(10_000L, snapshot.manifest.createdAt)
        assertEquals(4, snapshot.manifest.schemaVersion)

        val codec = com.pinhoquest.core.backup.BackupCodec()
        val decoded = codec.decode(codec.encode(snapshot))
        assertTrue(decoded is BackupReadResult.Valid)
        val decodedSnapshot = (decoded as BackupReadResult.Valid).snapshot
        assertEquals(
            snapshot.copy(manifest = snapshot.manifest.copy(integrityHash = decodedSnapshot.manifest.integrityHash)),
            decodedSnapshot,
        )
    }

    @Test
    fun restoreReplacesRoomStateAndPreferencesFromValidatedSnapshot() = runBlocking {
        seedState(
            gardenName = "Jardim Original",
            questTitle = "Quest Original",
            theme = ThemePreference.DARK,
            fontScale = 1.25f,
        )
        val builder = RoomBackupSnapshotBuilder(db, preferencesStore)
        val built = builder.build("0.1.0")
        assertTrue(built is BackupSnapshotBuildResult.Ready)
        val snapshot = (built as BackupSnapshotBuildResult.Ready).snapshot
        val codec = com.pinhoquest.core.backup.BackupCodec()
        val bytes = codec.encode(snapshot)

        seedState(
            gardenName = "Jardim Modificado",
            questTitle = "Quest Modificada",
            theme = ThemePreference.LIGHT,
            fontScale = 0.9f,
        )
        assertEquals(
            "Jardim Modificado",
            RoomProfileRepository(db.profileDao()).current()!!.gardenOwnerName.value,
        )

        val result = RoomRestoreService(db, preferencesStore).restore(codec.decode(bytes))

        assertTrue(result is RoomRestoreResult.Restored)
        assertEquals(
            "Jardim Original",
            RoomProfileRepository(db.profileDao()).current()!!.gardenOwnerName.value,
        )
        assertEquals(
            "Quest Original",
            RoomQuestRepository(db.questDao()).get(QuestId("quest-1"))!!.title,
        )
        val restoredPreferences = preferencesStore.preferences.first()
        assertEquals(ThemePreference.DARK, restoredPreferences.theme)
        assertEquals(1.25f, restoredPreferences.fontScale)
    }

    @Test
    fun invalidBackupLeavesCurrentStateUntouched() = runBlocking {
        seedState(
            gardenName = "Jardim Atual",
            questTitle = "Quest Atual",
            theme = ThemePreference.LIGHT,
            fontScale = 1.0f,
        )

        val result = RoomRestoreService(db, preferencesStore).restore(
            BackupReadResult.InvalidBackup("tampered"),
        )

        assertTrue(result is RoomRestoreResult.Invalid)
        assertEquals(
            "Jardim Atual",
            RoomProfileRepository(db.profileDao()).current()!!.gardenOwnerName.value,
        )
        assertEquals(
            "Quest Atual",
            RoomQuestRepository(db.questDao()).get(QuestId("quest-1"))!!.title,
        )
        val preferences = preferencesStore.preferences.first()
        assertEquals(ThemePreference.LIGHT, preferences.theme)
        assertEquals(1.0f, preferences.fontScale)
    }

    private suspend fun seedState(
        gardenName: String,
        questTitle: String,
        theme: ThemePreference,
        fontScale: Float,
    ) {
        RoomProfileRepository(db.profileDao()).upsert(
            UserProfile(
                id = ProfileId("profile-1"),
                gardenOwnerName = GardenOwnerName.create(gardenName).getOrThrow(),
                createdAtEpochMillis = 100L,
            ),
        )
        RoomQuestRepository(db.questDao()).upsert(
            Quest(
                id = QuestId("quest-1"),
                title = questTitle,
                description = "Uma quest de teste.",
                objectives = listOf(
                    QuestObjective(ObjectiveId("objective-1"), "Concluir o teste."),
                ),
                category = QuestCategory.CODING,
                environment = QuestEnvironment.ANDROID,
                estimatedDuration = EstimatedDuration(15, 30),
                difficulty = QuestDifficulty.EASY,
            ),
        )
        preferencesStore.setTheme(theme)
        preferencesStore.setFontScale(fontScale)
    }
}
