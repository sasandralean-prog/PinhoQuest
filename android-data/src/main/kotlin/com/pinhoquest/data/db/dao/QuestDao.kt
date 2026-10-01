package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pinhoquest.data.db.entity.QuestEntity
import com.pinhoquest.data.db.entity.QuestObjectiveEntity
import com.pinhoquest.data.db.entity.QuestWithObjectives

@Dao
abstract class QuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertQuest(entity: QuestEntity)

    @Query("DELETE FROM quest_objectives WHERE questId = :questId")
    protected abstract suspend fun deleteObjectives(questId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertObjectives(entities: List<QuestObjectiveEntity>)

    @Transaction
    open suspend fun replace(
        quest: QuestEntity,
        objectives: List<QuestObjectiveEntity>,
    ) {
        upsertQuest(quest)
        deleteObjectives(quest.questId)
        if (objectives.isNotEmpty()) {
            insertObjectives(objectives)
        }
    }

    @Transaction
    @Query("SELECT * FROM quests WHERE questId = :questId LIMIT 1")
    abstract suspend fun getWithObjectives(questId: String): QuestWithObjectives?
}
