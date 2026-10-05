package com.pinhoquest.domain.backup

sealed interface RestorePlanResult {
    data class Ready(val snapshot: BackupSnapshot) : RestorePlanResult
    data class Invalid(val reason: String) : RestorePlanResult
}
