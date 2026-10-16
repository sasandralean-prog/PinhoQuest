package com.pinhoquest.data.repository

import com.pinhoquest.data.db.dao.TagDao
import com.pinhoquest.data.db.entity.TagEntity
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTagRepositoryTest {
    private val dao = FakeTagDao()
    private val repository = RoomTagRepository(dao)
    private val profile = ProfileId("profile-1")

    @Test
    fun createTrimsLabelsAndPersistsUserTag() = runBlocking {
        val result = repository.createCustomTag(profile, "  Fotografia analógica  ")

        assertTrue(result is CustomTagWriteResult.Saved)
        val saved = (result as CustomTagWriteResult.Saved).tag
        assertEquals("Fotografia analógica", saved.label)
        assertEquals(TagSource.USER, saved.source)
        assertEquals(saved, repository.list(profile).single())
    }

    @Test
    fun rejectsBlankAndMoreThanTwentyFourCharacters() = runBlocking {
        assertEquals(CustomTagWriteResult.InvalidLabel, repository.createCustomTag(profile, "  "))
        assertEquals(
            CustomTagWriteResult.InvalidLabel,
            repository.createCustomTag(profile, "x".repeat(25)),
        )
        assertTrue(repository.list(profile).isEmpty())
    }

    @Test
    fun acceptsTenCustomTagsAndRejectsEleventhWithoutTouchingSystemTags() = runBlocking {
        repository.upsert(
            profile,
            com.pinhoquest.domain.tag.Tag(
                id = TagId("system-music"),
                label = "Música",
                source = TagSource.SYSTEM,
            ),
        )

        repeat(10) { index ->
            assertTrue(
                "index=$index",
                repository.createCustomTag(profile, "Tema $index") is CustomTagWriteResult.Saved,
            )
        }

        assertEquals(CustomTagWriteResult.LimitReached, repository.createCustomTag(profile, "Tema extra"))
        assertEquals(11, repository.list(profile).size)
        assertEquals(10, dao.countUserTags(profile.value))
    }

    @Test
    fun labelUniquenessIsCaseInsensitiveAcrossSystemAndUserTags() = runBlocking {
        repository.upsert(
            profile,
            com.pinhoquest.domain.tag.Tag(
                id = TagId("system-nature"),
                label = "Natureza",
                source = TagSource.SYSTEM,
            ),
        )

        assertEquals(
            CustomTagWriteResult.DuplicateLabel,
            repository.createCustomTag(profile, "nAtUrEzA"),
        )
    }

    @Test
    fun renamePreservesIdAndEnabledStateAndRejectsDuplicates() = runBlocking {
        val saved = repository.createCustomTag(profile, "Trilhas") as CustomTagWriteResult.Saved
        repository.upsert(profile, saved.tag.copy(enabled = false))

        val rename = repository.renameCustomTag(profile, saved.tag.id, "Fotografia")
        assertTrue(rename is CustomTagWriteResult.Saved)
        val updated = (rename as CustomTagWriteResult.Saved).tag
        assertEquals(saved.tag.id, updated.id)
        assertEquals("Fotografia", updated.label)
        assertFalse(updated.enabled)

        repository.createCustomTag(profile, "Leitura")
        assertEquals(
            CustomTagWriteResult.DuplicateLabel,
            repository.renameCustomTag(profile, saved.tag.id, "LEITURA"),
        )
    }

    @Test
    fun deleteOnlyRemovesUserCreatedTags() = runBlocking {
        repository.upsert(
            profile,
            com.pinhoquest.domain.tag.Tag(
                id = TagId("system-music"),
                label = "Música",
                source = TagSource.SYSTEM,
            ),
        )
        val userTag = (repository.createCustomTag(profile, "Jardinagem") as CustomTagWriteResult.Saved).tag

        assertTrue(repository.deleteCustomTag(profile, userTag.id))
        assertFalse(repository.deleteCustomTag(profile, userTag.id))
        assertEquals(listOf("Música"), repository.list(profile).map { it.label })
    }

    private class FakeTagDao : TagDao {
        private val rows = linkedMapOf<Pair<String, String>, TagEntity>()

        override suspend fun upsert(entity: TagEntity) {
            rows[entity.profileId to entity.tagId] = entity
        }

        override suspend fun listByProfile(profileId: String): List<TagEntity> =
            rows.values
                .filter { it.profileId == profileId }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.tagId })

        override suspend fun countUserTags(profileId: String): Int =
            rows.values.count { it.profileId == profileId && it.source == TagSource.USER.name }

        override suspend fun countTagsWithLabel(profileId: String, label: String): Int =
            rows.values.count { it.profileId == profileId && it.label.equals(label, ignoreCase = true) }

        override suspend fun countTagsWithLabelExcluding(
            profileId: String,
            label: String,
            excludingTagId: String,
        ): Int =
            rows.values.count {
                it.profileId == profileId &&
                    it.tagId != excludingTagId &&
                    it.label.equals(label, ignoreCase = true)
            }

        override suspend fun deleteUserTag(profileId: String, tagId: String): Int {
            val key = profileId to tagId
            val entity = rows[key] ?: return 0
            if (entity.source != TagSource.USER.name) return 0
            rows.remove(key)
            return 1
        }
    }
}
