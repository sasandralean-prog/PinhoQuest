package com.pinhoquest.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE game_catalog_snapshots ADD COLUMN cycleId TEXT NOT NULL DEFAULT 'cycle-legacy'",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS game_quest_usages (" +
                "usageId TEXT NOT NULL, cycleId TEXT NOT NULL, gameIdentityKey TEXT NOT NULL, " +
                "variantFingerprint TEXT NOT NULL, usedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(usageId))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_game_quest_usages_cycleId ON game_quest_usages(cycleId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_game_quest_usages_gameIdentityKey ON game_quest_usages(gameIdentityKey)",
        )
    }
}
