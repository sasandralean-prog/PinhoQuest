package com.pinhoquest.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.data.db.PinhoQuestDatabase
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCustomTagPersistenceTest {
    @Test
    fun persistsTenCustomTagsWithoutCountingSystemTagsTowardLimit() = withDatabase { database ->
        val repository = RoomTagRepository(database.tagDao())
        val profile = ProfileId("profile-persistence")

        repository.upsert(
            profile,
            Tag(id = TagId("system-nature"), label = "Natureza", source = TagSource.SYSTEM),
        )
        repeat(10) { index ->
            assertTrue(
                repository.createCustomTag(profile, "Tema $index") is CustomTagWriteResult.Saved,
            )
        }

        assertEquals(10, database.tagDao().countUserTags(profile.value))
        assertEquals(11, repository.list(profile).size)
        assertEquals(
            CustomTagWriteResult.LimitReached,
            repository.createCustomTag(profile, "Tema extra"),
        )
    }

    @Test
    fun enforcesCaseInsensitiveUniquenessAndAllowsRename() = withDatabase { database ->
        val repository = RoomTagRepository(database.tagDao())
        val profile = ProfileId("profile-unique")
        repository.upsert(
            profile,
            Tag(id = TagId("system-music"), label = "Música", source = TagSource.SYSTEM),
        )

        assertEquals(
            CustomTagWriteResult.DuplicateLabel,
            repository.createCustomTag(profile, "mÚsIcA"),
        )
        val saved = repository.createCustomTag(profile, "Trilhas") as CustomTagWriteResult.Saved
        val renamed = repository.renameCustomTag(profile, saved.tag.id, "Fotografia")
        assertTrue(renamed is CustomTagWriteResult.Saved)
        assertEquals("Fotografia", repository.list(profile).single { it.id == saved.tag.id }.label)
    }

    @Test
    fun deletionOnlyRemovesUserCreatedTags() = withDatabase { database ->
        val repository = RoomTagRepository(database.tagDao())
        val profile = ProfileId("profile-delete")
        repository.upsert(
            profile,
            Tag(id = TagId("system-music"), label = "Música", source = TagSource.SYSTEM),
        )
        val saved = repository.createCustomTag(profile, "Jardinagem") as CustomTagWriteResult.Saved

        assertTrue(repository.deleteCustomTag(profile, saved.tag.id))
        assertEquals(listOf("Música"), repository.list(profile).map { it.label })
    }

    private fun withDatabase(block: suspend (PinhoQuestDatabase) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, PinhoQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking { block(database) }
        } finally {
            database.close()
        }
    }
}
