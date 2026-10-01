package com.pinhoquest.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.data.repository.RoomProfileRepository
import com.pinhoquest.data.repository.RoomQuestRepository
import com.pinhoquest.data.repository.RoomQuestSessionRepository
import com.pinhoquest.data.repository.RoomTagRepository
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
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.domain.quest.QuestSessionId
import com.pinhoquest.domain.quest.QuestState
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinhoQuestDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "pinho-quest-foundation-test.db"

    @Before
    fun cleanBefore() {
        context.deleteDatabase(dbName)
    }

    @After
    fun cleanAfter() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun profileTagAndQuestRoundTrip() = runBlocking {
        val db = openDatabase()
        val profile = UserProfile(
            id = ProfileId("profile-1"),
            gardenOwnerName = GardenOwnerName.create("Rafa").getOrThrow(),
            createdAtEpochMillis = 100L,
        )
        val tag = Tag(
            id = TagId("tag-coding"),
            label = "Programação",
            source = TagSource.USER,
            affinity = 0.9,
        )
        val quest = sampleQuest()

        RoomProfileRepository(db.profileDao()).upsert(profile)
        RoomTagRepository(db.tagDao()).upsert(profile.id, tag)
        RoomQuestRepository(db.questDao()).upsert(quest)

        assertEquals(profile, RoomProfileRepository(db.profileDao()).get(profile.id))
        assertEquals(listOf(tag), RoomTagRepository(db.tagDao()).list(profile.id))
        assertEquals(quest, RoomQuestRepository(db.questDao()).get(quest.id))
        db.close()
    }

    @Test
    fun sessionRepositoryFindsCanonicalSessionByQuestId() = runBlocking {
        val db = openDatabase()
        val repository: com.pinhoquest.core.session.QuestSessionRepository =
            RoomQuestSessionRepository(db.questSessionDao())
        val session = QuestSession(
            id = QuestSessionId("session-by-quest"),
            questId = QuestId("quest-by-id"),
            state = QuestState.ACCEPTED,
            createdAtEpochMillis = 150L,
            updatedAtEpochMillis = 150L,
        )

        repository.upsert(session)

        assertEquals(session, repository.getByQuestId(session.questId))
        db.close()
    }

    @Test
    fun activeSessionRestoresAfterDatabaseReopen() = runBlocking {
        var db = openDatabase()
        val session = QuestSession(
            id = QuestSessionId("session-1"),
            questId = QuestId("quest-1"),
            state = QuestState.ACTIVE,
            createdAtEpochMillis = 200L,
            updatedAtEpochMillis = 300L,
        )
        RoomQuestSessionRepository(db.questSessionDao()).upsert(session)
        db.close()

        db = openDatabase()
        val restored = RoomQuestSessionRepository(db.questSessionDao()).active()

        assertNotNull(restored)
        assertEquals(session, restored)
        db.close()
    }

    private fun openDatabase(): PinhoQuestDatabase =
        Room.databaseBuilder(context, PinhoQuestDatabase::class.java, dbName).build()

    private fun sampleQuest() = Quest(
        id = QuestId("quest-1"),
        title = "Frankenstein Digital",
        description = "Descubra algo pequeno sobre um arquivo.",
        objectives = listOf(
            QuestObjective(ObjectiveId("objective-1"), "Investigue o formato real"),
        ),
        category = QuestCategory.CODING,
        environment = QuestEnvironment.WINDOWS,
        estimatedDuration = EstimatedDuration(15, 30),
        difficulty = QuestDifficulty.MEDIUM,
    )
}
