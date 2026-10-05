package com.pinhoquest.ui.copy

import com.pinhoquest.domain.backup.BackupSnapshotBuildResult
import com.pinhoquest.domain.backup.RestorePlanResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingCopyTest {
    @Test
    fun backupAndRestoreCopyIsWarmAndDoesNotLeakImplementationTerms() {
        val messages = listOf(
            UserFacingCopy.forBackupSnapshotResult(BackupSnapshotBuildResult.NoProfile),
            UserFacingCopy.forBackupSnapshotResult(
                BackupSnapshotBuildResult.InvalidState("Room failure"),
            ),
            UserFacingCopy.forRestorePlanResult(
                RestorePlanResult.Invalid("schema mismatch"),
            ),
        )

        messages.forEach { message ->
            assertFalse(message.contains("Room", ignoreCase = true))
            assertFalse(message.contains("exception", ignoreCase = true))
            assertFalse(message.contains("provider", ignoreCase = true))
            assertFalse(message.contains("inference", ignoreCase = true))
            assertFalse(message.contains("schema", ignoreCase = true))
        }
        assertTrue(messages.all { it.isNotBlank() })
    }
}
