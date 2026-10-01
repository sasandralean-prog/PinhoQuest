package com.pinhoquest.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val questId: String,
    val title: String,
    val description: String,
    val category: String,
    val environment: String,
    val minMinutes: Int,
    val maxMinutes: Int,
    val difficulty: String,
    val state: String,
)

@Entity(
    tableName = "quest_objectives",
    primaryKeys = ["questId", "objectiveId"],
)
data class QuestObjectiveEntity(
    val questId: String,
    val objectiveId: String,
    val text: String,
    val optional: Boolean,
)

data class QuestWithObjectives(
    @Embedded val quest: QuestEntity,
    @Relation(
        parentColumn = "questId",
        entityColumn = "questId",
    )
    val objectives: List<QuestObjectiveEntity>,
)
