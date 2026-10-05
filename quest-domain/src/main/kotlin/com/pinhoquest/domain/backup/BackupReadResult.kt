package com.pinhoquest.domain.backup

sealed interface BackupReadResult {
    data class Valid(val snapshot: BackupSnapshot) : BackupReadResult
    data class InvalidBackup(val reason: String) : BackupReadResult
}
