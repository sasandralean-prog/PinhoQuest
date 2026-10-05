package com.pinhoquest.core.backup

import com.pinhoquest.domain.backup.BackupManifest
import com.pinhoquest.domain.backup.BackupPreferences
import com.pinhoquest.domain.backup.BackupQuest
import com.pinhoquest.domain.backup.BackupQuestObjective
import com.pinhoquest.domain.backup.BackupSnapshot
import com.pinhoquest.domain.backup.BackupTag
import com.pinhoquest.domain.backup.RestorePlanResult
import org.junit.Assert.assertTrue
import org.junit.Test

class RestorePlannerTest {
    private val planner = RestorePlanner()

    @Test
    fun validSnapshotIsReady() {
        assertTrue(planner.plan(sample()) is RestorePlanResult.Ready)
    }

    @Test
    fun unsupportedSchemaIsRejected() {
        val snapshot = sample().copy(
            manifest = sample().manifest.copy(schemaVersion = 3),
        )
        assertInvalid(snapshot)
    }

    @Test
    fun foreignProfileDataIsRejected() {
        val snapshot = sample().copy(
            tags = listOf(
                BackupTag(
                    profileId = "other",
                    tagId = "tag-1",
                    label = "Teste",
                    source = "USER",
                    affinity = 1.0,
                    enabled = true,
                ),
            ),
        )
        assertInvalid(snapshot)
    }

    @Test
    fun danglingObjectiveReferenceIsRejected() {
        val snapshot = sample().copy(
            questObjectives = listOf(
                BackupQuestObjective(
                    questId = "missing-quest",
                    objectiveId = "objective-1",
                    text = "Algo",
                    optional = false,
                ),
            ),
        )
        assertInvalid(snapshot)
    }

    @Test
    fun invalidFontScaleIsRejected() {
        val snapshot = sample().copy(
            preferences = BackupPreferences(theme = "SYSTEM", fontScale = 0f),
        )
        assertInvalid(snapshot)
    }

    private fun assertInvalid(snapshot: BackupSnapshot) {
        assertTrue(planner.plan(snapshot) is RestorePlanResult.Invalid)
    }

    private fun sample() = BackupSnapshot(
        manifest = BackupManifest(
            formatVersion = 1,
            schemaVersion = 4,
            profileId = "profile-1",
            datasetRevision = 0L,
            createdAt = 100L,
            appVersion = "0.1.0",
            catalogVersions = emptyMap(),
            integrityHash = "trusted-by-codec",
        ),
        profile = com.pinhoquest.domain.backup.BackupProfile(
            profileId = "profile-1",
            gardenOwnerName = "Jardim",
            createdAtEpochMillis = 100L,
        ),
        quests = listOf(
            BackupQuest(
                questId = "quest-1",
                title = "Quest",
                description = "Descricao",
                category = "CODING",
                environment = "ANDROID",
                minMinutes = 15,
                maxMinutes = 30,
                difficulty = "EASY",
                state = "GENERATED",
            ),
        ),
    )
}
