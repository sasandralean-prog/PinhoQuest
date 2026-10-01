package com.pinhoquest.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "xp_transactions",
    indices = [
        Index("profileId"),
        Index("completionId"),
        Index("flowerId"),
    ],
)
data class XpTransactionEntity(
    @PrimaryKey val transactionId: String,
    val profileId: String,
    val amount: Int,
    val type: String,
    val completionId: String?,
    val flowerId: String?,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "goal_definitions")
data class GoalDefinitionEntity(
    @PrimaryKey val goalId: String,
    val title: String,
    val ruleType: String,
    val target: Int,
    val category: String?,
    val difficulty: String?,
    val version: Int,
)

@Entity(
    tableName = "goal_progress",
    primaryKeys = ["profileId", "goalId"],
    indices = [Index("profileId")],
)
data class GoalProgressEntity(
    val profileId: String,
    val goalId: String,
    val currentValue: Int,
    val targetValue: Int,
    val completed: Boolean,
)

@Entity(
    tableName = "goal_progress_applications",
    primaryKeys = ["profileId", "goalId", "completionId"],
    indices = [Index("completionId")],
)
data class GoalProgressApplicationEntity(
    val profileId: String,
    val goalId: String,
    val completionId: String,
    val appliedAtEpochMillis: Long,
)

@Entity(
    tableName = "quest_completions",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index("profileId"),
        Index("questId"),
    ],
)
data class QuestCompletionEntity(
    @PrimaryKey val completionId: String,
    val sessionId: String,
    val questId: String,
    val profileId: String,
    val completedAtEpochMillis: Long,
    val xpAward: Int,
)

@Entity(
    tableName = "completion_objectives",
    primaryKeys = ["completionId", "objectiveId"],
    indices = [Index("completionId")],
)
data class CompletionObjectiveEntity(
    val completionId: String,
    val objectiveId: String,
)

@Entity(tableName = "dataset_metadata")
data class DatasetMetadataEntity(
    @PrimaryKey val profileId: String,
    val datasetRevision: Long,
    val schemaVersion: Int,
    val lastModifiedAtEpochMillis: Long,
)
