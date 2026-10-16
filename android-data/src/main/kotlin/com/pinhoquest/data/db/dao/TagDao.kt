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

    @Query("SELECT COUNT(*) FROM tags WHERE profileId = :profileId AND source = 'USER'")
    suspend fun countUserTags(profileId: String): Int

    @Query("SELECT COUNT(*) FROM tags WHERE profileId = :profileId AND label = :label COLLATE NOCASE")
    suspend fun countTagsWithLabel(profileId: String, label: String): Int

    @Query("SELECT COUNT(*) FROM tags WHERE profileId = :profileId AND label = :label COLLATE NOCASE AND tagId != :excludingTagId")
    suspend fun countTagsWithLabelExcluding(profileId: String, label: String, excludingTagId: String): Int

    @Query("DELETE FROM tags WHERE profileId = :profileId AND tagId = :tagId AND source = 'USER'")
    suspend fun deleteUserTag(profileId: String, tagId: String): Int
}
