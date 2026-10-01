package com.pinhoquest.domain.profile

@JvmInline
value class ProfileId(val value: String)

data class UserProfile(
    val id: ProfileId,
    val gardenOwnerName: GardenOwnerName,
    val createdAtEpochMillis: Long,
)
