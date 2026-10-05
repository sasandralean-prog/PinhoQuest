package com.pinhoquest.data.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pinhoquest.data.db.migration.MIGRATION_2_3
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "pinho-quest-migration-2-3.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PinhoQuestDatabase::class.java,
    )

    @Before
    fun before() {
        context.deleteDatabase(dbName)
    }

    @After
    fun after() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrationCreatesNormalizedGameCatalogTablesWithoutLosingExistingData() = runBlocking {
        helper.createDatabase(dbName, 2).close()

        val database = helper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            MIGRATION_2_3,
        )

        database.close()
    }

    @Test
    fun migratedDatabaseAcceptsNormalizedSnapshotRows() = runBlocking {
        helper.createDatabase(dbName, 2).close()

        val database = helper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            MIGRATION_2_3,
        )

        database.execSQL(
            "INSERT INTO game_catalog_snapshots(snapshotId, researchedAtEpochMillis, expiresAtEpochMillis) " +
                "VALUES('default', 100, 1100)",
        )

        val cursor = database.query(
            "SELECT snapshotId, researchedAtEpochMillis, expiresAtEpochMillis FROM game_catalog_snapshots",
        )
        cursor.use {
            assertEquals(1, it.count)
            assertEquals(true, it.moveToFirst())
            assertEquals("default", it.getString(0))
            assertEquals(100L, it.getLong(1))
            assertEquals(1_100L, it.getLong(2))
        }
        database.close()
    }
}
