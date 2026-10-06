package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val profileId: String,
    val gardenOwnerName: String,
    val createdAtEpochMillis: Long,
)
