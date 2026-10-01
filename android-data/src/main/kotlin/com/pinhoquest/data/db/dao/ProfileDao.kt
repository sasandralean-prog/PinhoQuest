package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinhoquest.data.db.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun get(profileId: String): ProfileEntity?

    @Query(
        "SELECT * FROM profiles " +
            "ORDER BY createdAtEpochMillis ASC, profileId ASC LIMIT 1",
    )
    suspend fun current(): ProfileEntity?
}
