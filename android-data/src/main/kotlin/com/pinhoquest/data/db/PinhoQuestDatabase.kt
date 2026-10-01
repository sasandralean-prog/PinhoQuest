package com.pinhoquest.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pinhoquest.data.db.dao.ProfileDao
import com.pinhoquest.data.db.dao.QuestDao
import com.pinhoquest.data.db.dao.QuestSessionDao
import com.pinhoquest.data.db.dao.TagDao
import com.pinhoquest.data.db.entity.ProfileEntity
import com.pinhoquest.data.db.entity.QuestEntity
import com.pinhoquest.data.db.entity.QuestObjectiveEntity
import com.pinhoquest.data.db.entity.QuestSessionEntity
import com.pinhoquest.data.db.entity.TagEntity

@Database(
    entities = [
        ProfileEntity::class,
        TagEntity::class,
        QuestEntity::class,
        QuestObjectiveEntity::class,
        QuestSessionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PinhoQuestDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun tagDao(): TagDao
    abstract fun questDao(): QuestDao
    abstract fun questSessionDao(): QuestSessionDao
}
