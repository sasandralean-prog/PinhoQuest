package com.pinhoquest.core.inference

import com.pinhoquest.domain.model.ModelManifest
import java.io.File

data class InstalledModel(
    val manifest: ModelManifest,
    val modelFile: File,
)

sealed interface ModelInstallResult {
    data class Installed(val model: InstalledModel) : ModelInstallResult
    data class Rejected(val reason: ModelInstallRejection) : ModelInstallResult
}

enum class ModelInstallRejection {
    STAGED_FILE_MISSING,
    STAGED_FILE_IS_DIRECTORY,
    PARTIAL_FILE,
    SIZE_MISMATCH,
    HASH_MISMATCH,
    INVALID_DESTINATION,
    PROMOTION_FAILED,
}

interface ModelStorePort {
    fun install(stagedFile: File, manifest: ModelManifest): ModelInstallResult
    fun active(): InstalledModel?
}