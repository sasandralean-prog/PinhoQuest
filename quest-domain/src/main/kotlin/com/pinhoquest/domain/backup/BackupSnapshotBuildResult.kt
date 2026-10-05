package com.pinhoquest.domain.backup

sealed interface BackupSnapshotBuildResult {
    data class Ready(val snapshot: BackupSnapshot) : BackupSnapshotBuildResult
    data object NoProfile : BackupSnapshotBuildResult
    data object MultipleProfiles : BackupSnapshotBuildResult
    data class InvalidState(val reason: String) : BackupSnapshotBuildResult
}
