package com.pinhoquest.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pinhoquest.data.db.entity.CompletionObjectiveEntity
import com.pinhoquest.data.db.entity.DatasetMetadataEntity
import com.pinhoquest.data.db.entity.GoalDefinitionEntity
import com.pinhoquest.data.db.entity.GoalProgressApplicationEntity
import com.pinhoquest.data.db.entity.GoalProgressEntity
import com.pinhoquest.data.db.entity.QuestCompletionEntity
import com.pinhoquest.data.db.entity.XpTransactionEntity

@Dao
interface ProgressionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertXpTransaction(entity: XpTransactionEntity)

    @Query("SELECT * FROM xp_transactions WHERE profileId = :profileId ORDER BY createdAtEpochMillis, transactionId")
    suspend fun xpTransactions(profileId: String): List<XpTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalDefinitions(entities: List<GoalDefinitionEntity>)

    @Query("SELECT * FROM goal_definitions ORDER BY goalId")
    suspend fun goalDefinitions(): List<GoalDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalProgress(entities: List<GoalProgressEntity>)

    @Query("SELECT * FROM goal_progress WHERE profileId = :profileId ORDER BY goalId")
    suspend fun goalProgress(profileId: String): List<GoalProgressEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalApplications(entities: List<GoalProgressApplicationEntity>): List<Long>

    @Query("SELECT * FROM goal_progress_applications WHERE profileId = :profileId ORDER BY goalId, completionId")
    suspend fun goalApplications(profileId: String): List<GoalProgressApplicationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompletion(entity: QuestCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompletionObjectives(entities: List<CompletionObjectiveEntity>)

    @Query("SELECT * FROM quest_completions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun completionBySession(sessionId: String): QuestCompletionEntity?

    @Query("SELECT * FROM quest_completions WHERE completionId = :completionId LIMIT 1")
    suspend fun completionById(completionId: String): QuestCompletionEntity?

    @Query("SELECT objectiveId FROM completion_objectives WHERE completionId = :completionId ORDER BY objectiveId")
    suspend fun completedObjectiveIds(completionId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDatasetMetadata(entity: DatasetMetadataEntity)

    @Update
    suspend fun updateDatasetMetadata(entity: DatasetMetadataEntity)

    @Query("SELECT * FROM dataset_metadata WHERE profileId = :profileId LIMIT 1")
    suspend fun datasetMetadata(profileId: String): DatasetMetadataEntity?
}
