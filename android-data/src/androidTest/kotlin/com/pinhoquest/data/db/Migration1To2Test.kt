package com.pinhoquest.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.data.db.entity.FlowerAcquisitionEntity
import com.pinhoquest.data.db.entity.RewardOpportunityEntity
import com.pinhoquest.data.db.migration.MIGRATION_1_2
import com.pinhoquest.data.db.migration.MIGRATION_2_3
import com.pinhoquest.data.db.migration.MIGRATION_3_4
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.repository.RoomQuestSessionRepository
import com.pinhoquest.data.repository.RoomTagRepository
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.quest.QuestId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "pinho-quest-migration-1-2.db"

    @Before
    fun before() {
        context.deleteDatabase(dbName)
    }

    @After
    fun after() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrationPreservesFoundationDataAndCreatesMetadata() = runBlocking {
        createVersion1Fixture()

        val db = Room.databaseBuilder(context, PinhoQuestDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

        val profile = RoomProfileRepository(db.profileDao()).get(ProfileId("profile-1"))
        val tags = RoomTagRepository(db.tagDao()).list(ProfileId("profile-1"))
        val quest = RoomQuestRepository(db.questDao()).get(QuestId("quest-1"))
        val session = RoomQuestSessionRepository(db.questSessionDao()).active()
        val metadata = db.progressionDao().datasetMetadata("profile-1")

        assertNotNull(profile)
        assertEquals("Rafa", profile?.gardenOwnerName?.value)
        assertEquals(1, tags.size)
        assertNotNull(quest)
        assertNotNull(session)
        assertEquals(0L, metadata?.datasetRevision)
        assertEquals(2, metadata?.schemaVersion)
        db.close()
    }

    @Test
    fun duplicateFlowerAcquisitionForSameProfileAndFlowerIsRejected() = runBlocking {
        val db = freshV2()
        val dao = db.gardenDao()
        dao.insertAcquisition(
            FlowerAcquisitionEntity(
                profileId = "p1",
                flowerId = "f1",
                completionId = "c1",
                acquiredAtEpochMillis = 1L,
                xpAward = 50,
            ),
        )

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.insertAcquisition(
                    FlowerAcquisitionEntity(
                        profileId = "p1",
                        flowerId = "f1",
                        completionId = "c2",
                        acquiredAtEpochMillis = 2L,
                        xpAward = 25,
                    ),
                )
            }
        }
        db.close()
    }

    @Test
    fun oneCompletionCannotCreateTwoRewardOpportunities() = runBlocking {
        val db = freshV2()
        val dao = db.gardenDao()
        dao.insertRewardOpportunity(
            RewardOpportunityEntity(
                opportunityId = "r1",
                profileId = "p1",
                completionId = "c1",
                state = "CREATED",
                resolvedFlowerId = null,
                createdAtEpochMillis = 1L,
                resolvedAtEpochMillis = null,
            ),
        )

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.insertRewardOpportunity(
                    RewardOpportunityEntity(
                        opportunityId = "r2",
                        profileId = "p1",
                        completionId = "c1",
                        state = "CREATED",
                        resolvedFlowerId = null,
                        createdAtEpochMillis = 2L,
                        resolvedAtEpochMillis = null,
                    ),
                )
            }
        }
        db.close()
    }

    private fun freshV2(): PinhoQuestDatabase =
        Room.inMemoryDatabaseBuilder(context, PinhoQuestDatabase::class.java).build()

    private fun createVersion1Fixture() {
        val file = context.getDatabasePath(dbName)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS profiles (" +
                "profileId TEXT NOT NULL PRIMARY KEY, " +
                "gardenOwnerName TEXT NOT NULL, " +
                "createdAtEpochMillis INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS tags (" +
                "profileId TEXT NOT NULL, tagId TEXT NOT NULL, label TEXT NOT NULL, " +
                "source TEXT NOT NULL, affinity REAL NOT NULL, enabled INTEGER NOT NULL, " +
                "PRIMARY KEY(profileId, tagId))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_profileId ON tags(profileId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS quests (" +
                "questId TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, " +
                "category TEXT NOT NULL, environment TEXT NOT NULL, minMinutes INTEGER NOT NULL, " +
                "maxMinutes INTEGER NOT NULL, difficulty TEXT NOT NULL, state TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS quest_objectives (" +
                "questId TEXT NOT NULL, objectiveId TEXT NOT NULL, text TEXT NOT NULL, optional INTEGER NOT NULL, " +
                "PRIMARY KEY(questId, objectiveId))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS quest_sessions (" +
                "sessionId TEXT NOT NULL PRIMARY KEY, questId TEXT NOT NULL, state TEXT NOT NULL, " +
                "createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_sessions_state ON quest_sessions(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_sessions_questId ON quest_sessions(questId)")

        db.execSQL("INSERT INTO profiles VALUES ('profile-1','Rafa',100)")
        db.execSQL("INSERT INTO tags VALUES ('profile-1','coding','Programação','USER',0.9,1)")
        db.execSQL(
            "INSERT INTO quests VALUES (" +
                "'quest-1','Q','D','CODING','WINDOWS',15,30,'MEDIUM','ACTIVE')",
        )
        db.execSQL("INSERT INTO quest_objectives VALUES ('quest-1','o1','Faça algo',0)")
        db.execSQL("INSERT INTO quest_sessions VALUES ('session-1','quest-1','ACTIVE',200,300)")
        db.version = 1
        db.close()
    }
}
