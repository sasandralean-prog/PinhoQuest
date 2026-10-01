package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "tags",
    primaryKeys = ["profileId", "tagId"],
    indices = [Index("profileId")],
)
data class TagEntity(
    val profileId: String,
    val tagId: String,
    val label: String,
    val source: String,
    val affinity: Double,
    val enabled: Boolean,
)
