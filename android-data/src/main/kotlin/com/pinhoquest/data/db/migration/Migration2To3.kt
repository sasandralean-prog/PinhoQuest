package com.pinhoquest.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS game_catalog_snapshots (" +
                "snapshotId TEXT NOT NULL, researchedAtEpochMillis INTEGER NOT NULL, " +
                "expiresAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(snapshotId))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS game_discoveries (" +
                "identityKey TEXT NOT NULL, canonicalName TEXT NOT NULL, platformsJson TEXT NOT NULL, " +
                "genresJson TEXT NOT NULL, availability TEXT NOT NULL, provenanceJson TEXT NOT NULL, " +
                "PRIMARY KEY(identityKey))",
        )
    }
}
