package com.pinhoquest.data.repository

import com.pinhoquest.data.db.dao.TagDao
import com.pinhoquest.data.db.entity.TagEntity
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import java.util.UUID

sealed interface CustomTagWriteResult {
    data class Saved(val tag: Tag) : CustomTagWriteResult
    data object InvalidLabel : CustomTagWriteResult
    data object LimitReached : CustomTagWriteResult
    data object DuplicateLabel : CustomTagWriteResult
    data object NotFound : CustomTagWriteResult
}

class RoomTagRepository(
    private val dao: TagDao,
) {
    suspend fun upsert(profileId: ProfileId, tag: Tag) {
        dao.upsert(
            TagEntity(
                profileId = profileId.value,
                tagId = tag.id.value,
                label = tag.label,
                source = tag.source.name,
                affinity = tag.affinity,
                enabled = tag.enabled,
            ),
        )
    }

    suspend fun list(profileId: ProfileId): List<Tag> =
        dao.listByProfile(profileId.value).map { entity ->
            Tag(
                id = TagId(entity.tagId),
                label = entity.label,
                source = TagSource.valueOf(entity.source),
                affinity = entity.affinity,
                enabled = entity.enabled,
            )
        }

    /**
     * User tags are persisted in the canonical Room tags table. The cap applies
     * only to USER tags, never to built-in SYSTEM or explicitly confirmed DERIVED
     * tags. IDs remain stable when a tag is renamed.
     */
    suspend fun createCustomTag(profileId: ProfileId, rawLabel: String): CustomTagWriteResult {
        val label = normalizeCustomLabel(rawLabel) ?: return CustomTagWriteResult.InvalidLabel
        if (dao.countUserTags(profileId.value) >= MAX_CUSTOM_TAGS) {
            return CustomTagWriteResult.LimitReached
        }
        if (dao.countTagsWithLabel(profileId.value, label) > 0) {
            return CustomTagWriteResult.DuplicateLabel
        }
        val tag = Tag(
            id = TagId("user-${UUID.randomUUID()}"),
            label = label,
            source = TagSource.USER,
        )
        upsert(profileId, tag)
        return CustomTagWriteResult.Saved(tag)
    }

    suspend fun renameCustomTag(
        profileId: ProfileId,
        tagId: TagId,
        rawLabel: String,
    ): CustomTagWriteResult {
        val current = dao.listByProfile(profileId.value)
            .firstOrNull { it.tagId == tagId.value && it.source == TagSource.USER.name }
            ?: return CustomTagWriteResult.NotFound
        val label = normalizeCustomLabel(rawLabel) ?: return CustomTagWriteResult.InvalidLabel
        if (dao.countTagsWithLabelExcluding(profileId.value, label, tagId.value) > 0) {
            return CustomTagWriteResult.DuplicateLabel
        }
        val renamed = Tag(
            id = TagId(current.tagId),
            label = label,
            source = TagSource.USER,
            affinity = current.affinity,
            enabled = current.enabled,
        )
        upsert(profileId, renamed)
        return CustomTagWriteResult.Saved(renamed)
    }

    suspend fun deleteCustomTag(profileId: ProfileId, tagId: TagId): Boolean =
        dao.deleteUserTag(profileId.value, tagId.value) > 0

    private fun normalizeCustomLabel(rawLabel: String): String? =
        rawLabel.trim().takeIf { it.isNotEmpty() && it.length <= MAX_CUSTOM_TAG_LABEL_LENGTH }

    companion object {
        const val MAX_CUSTOM_TAGS = 10
        const val MAX_CUSTOM_TAG_LABEL_LENGTH = 24
    }
}
