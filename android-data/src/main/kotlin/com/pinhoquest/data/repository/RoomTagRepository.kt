package com.pinhoquest.data.repository

import com.pinhoquest.data.db.dao.TagDao
import com.pinhoquest.data.db.entity.TagEntity
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource

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
}
