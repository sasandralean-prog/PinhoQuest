package com.pinhoquest.data.repository

import com.pinhoquest.data.db.dao.ProfileDao
import com.pinhoquest.data.db.entity.ProfileEntity
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.profile.UserProfile

class RoomProfileRepository(
    private val dao: ProfileDao,
) {
    suspend fun upsert(profile: UserProfile) {
        dao.upsert(
            ProfileEntity(
                profileId = profile.id.value,
                gardenOwnerName = profile.gardenOwnerName.value,
                createdAtEpochMillis = profile.createdAtEpochMillis,
            ),
        )
    }

    suspend fun get(profileId: ProfileId): UserProfile? =
        dao.get(profileId.value)?.let { entity ->
            UserProfile(
                id = ProfileId(entity.profileId),
                gardenOwnerName = GardenOwnerName.create(entity.gardenOwnerName).getOrThrow(),
                createdAtEpochMillis = entity.createdAtEpochMillis,
            )
        }
}
