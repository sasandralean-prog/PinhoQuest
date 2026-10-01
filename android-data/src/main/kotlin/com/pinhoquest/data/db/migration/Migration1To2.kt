package com.pinhoquest.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS xp_transactions (" +
                "transactionId TEXT NOT NULL, profileId TEXT NOT NULL, amount INTEGER NOT NULL, " +
                "type TEXT NOT NULL, completionId TEXT, flowerId TEXT, createdAtEpochMillis INTEGER NOT NULL, " +
                "PRIMARY KEY(transactionId))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_profileId ON xp_transactions(profileId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_completionId ON xp_transactions(completionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_flowerId ON xp_transactions(flowerId)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS goal_definitions (" +
                "goalId TEXT NOT NULL, title TEXT NOT NULL, ruleType TEXT NOT NULL, target INTEGER NOT NULL, " +
                "category TEXT, difficulty TEXT, version INTEGER NOT NULL, PRIMARY KEY(goalId))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS goal_progress (" +
                "profileId TEXT NOT NULL, goalId TEXT NOT NULL, currentValue INTEGER NOT NULL, " +
                "targetValue INTEGER NOT NULL, completed INTEGER NOT NULL, PRIMARY KEY(profileId, goalId))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_progress_profileId ON goal_progress(profileId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS goal_progress_applications (" +
                "profileId TEXT NOT NULL, goalId TEXT NOT NULL, completionId TEXT NOT NULL, " +
                "appliedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(profileId, goalId, completionId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_goal_progress_applications_completionId " +
                "ON goal_progress_applications(completionId)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS quest_completions (" +
                "completionId TEXT NOT NULL, sessionId TEXT NOT NULL, questId TEXT NOT NULL, " +
                "profileId TEXT NOT NULL, completedAtEpochMillis INTEGER NOT NULL, xpAward INTEGER NOT NULL, " +
                "PRIMARY KEY(completionId))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_quest_completions_sessionId " +
                "ON quest_completions(sessionId)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_completions_profileId ON quest_completions(profileId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_quest_completions_questId ON quest_completions(questId)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS completion_objectives (" +
                "completionId TEXT NOT NULL, objectiveId TEXT NOT NULL, PRIMARY KEY(completionId, objectiveId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_completion_objectives_completionId " +
                "ON completion_objectives(completionId)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS dataset_metadata (" +
                "profileId TEXT NOT NULL, datasetRevision INTEGER NOT NULL, schemaVersion INTEGER NOT NULL, " +
                "lastModifiedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(profileId))",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO dataset_metadata(" +
                "profileId, datasetRevision, schemaVersion, lastModifiedAtEpochMillis) " +
                "SELECT profileId, 0, 2, createdAtEpochMillis FROM profiles",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS rarity_scales (" +
                "scaleVersion INTEGER NOT NULL, referenceMinPopulation INTEGER NOT NULL, " +
                "referenceMaxPopulation INTEGER NOT NULL, countingBasis TEXT NOT NULL, methodology TEXT NOT NULL, " +
                "PRIMARY KEY(scaleVersion))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS catalog_packs (" +
                "packId TEXT NOT NULL, profileId TEXT NOT NULL, collectionIndex INTEGER NOT NULL, " +
                "version INTEGER NOT NULL, generatedAtEpochMillis INTEGER NOT NULL, rarityScaleVersion INTEGER NOT NULL, " +
                "sourceUrisJson TEXT NOT NULL, integrityHash TEXT NOT NULL, PRIMARY KEY(packId))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_packs_profileId ON catalog_packs(profileId)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_catalog_packs_profileId_collectionIndex " +
                "ON catalog_packs(profileId, collectionIndex)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS flower_definitions (" +
                "flowerId TEXT NOT NULL, commonName TEXT NOT NULL, scientificName TEXT NOT NULL, " +
                "description TEXT NOT NULL, rarity TEXT NOT NULL, rarityScaleVersion INTEGER, " +
                "estimatedIndividuals INTEGER, lowerBound INTEGER, upperBound INTEGER, " +
                "estimateDateEpochMillis INTEGER, evidenceSourceUrisJson TEXT NOT NULL, confidence TEXT, " +
                "countingBasis TEXT, PRIMARY KEY(flowerId))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS catalog_entries (" +
                "packId TEXT NOT NULL, flowerId TEXT NOT NULL, slotIndex INTEGER NOT NULL, " +
                "eligible INTEGER NOT NULL, PRIMARY KEY(packId, flowerId))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_entries_packId ON catalog_entries(packId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_entries_flowerId ON catalog_entries(flowerId)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS flower_discovery (" +
                "profileId TEXT NOT NULL, flowerId TEXT NOT NULL, state TEXT NOT NULL, " +
                "PRIMARY KEY(profileId, flowerId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_flower_discovery_profileId ON flower_discovery(profileId)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS flower_acquisitions (" +
                "profileId TEXT NOT NULL, flowerId TEXT NOT NULL, completionId TEXT NOT NULL, " +
                "acquiredAtEpochMillis INTEGER NOT NULL, xpAward INTEGER NOT NULL, " +
                "PRIMARY KEY(profileId, flowerId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_flower_acquisitions_profileId ON flower_acquisitions(profileId)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_flower_acquisitions_completionId " +
                "ON flower_acquisitions(completionId)",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS reward_opportunities (" +
                "opportunityId TEXT NOT NULL, profileId TEXT NOT NULL, completionId TEXT NOT NULL, " +
                "state TEXT NOT NULL, resolvedFlowerId TEXT, createdAtEpochMillis INTEGER NOT NULL, " +
                "resolvedAtEpochMillis INTEGER, PRIMARY KEY(opportunityId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_reward_opportunities_profileId ON reward_opportunities(profileId)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_reward_opportunities_completionId " +
                "ON reward_opportunities(completionId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_reward_opportunities_state ON reward_opportunities(state)",
        )
    }
}
