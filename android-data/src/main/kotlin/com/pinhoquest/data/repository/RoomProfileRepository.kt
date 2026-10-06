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
        dao.get(profileId.value)?.toDomain()

    suspend fun current(): UserProfile? =
        dao.current()?.toDomain()

    private fun ProfileEntity.toDomain() = UserProfile(
        id = ProfileId(profileId),
        gardenOwnerName = GardenOwnerName.create(gardenOwnerName).getOrThrow(),
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
