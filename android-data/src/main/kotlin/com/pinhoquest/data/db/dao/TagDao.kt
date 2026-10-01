package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinhoquest.data.db.entity.TagEntity

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TagEntity)

    @Query("SELECT * FROM tags WHERE profileId = :profileId ORDER BY label COLLATE NOCASE, tagId")
    suspend fun listByProfile(profileId: String): List<TagEntity>
}
