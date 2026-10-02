package com.pinhoquest.model

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pinhoquest.domain.model.ModelManifest
import java.util.UUID

data class ModelDownloadSpec(
    val modelId: String,
    val version: String,
    val runtimeFormat: String,
    val sha256: String,
    val bytes: Long,
    val license: String,
    val source: String,
    val contextLimit: Int,
    val supportedBackends: List<String>,
    val downloadUrl: String,
) {
    fun toManifest(): ModelManifest = ModelManifest(
        modelId = modelId,
        version = version,
        runtimeFormat = runtimeFormat,
        sha256 = sha256,
        bytes = bytes,
        license = license,
        source = source,
        contextLimit = contextLimit,
        supportedBackends = supportedBackends.toSet(),
    )
}

interface ModelPackageCatalog {
    fun current(): ModelDownloadSpec?
}

sealed interface ModelInstallUiState {
    data object NotConfigured : ModelInstallUiState
    data object Queued : ModelInstallUiState
    data object Running : ModelInstallUiState
    data class Installed(val version: String) : ModelInstallUiState
    data class Failed(val message: String) : ModelInstallUiState
}

class ModelInstallCoordinator(
    private val context: Context,
    private val catalog: ModelPackageCatalog,
) {
    fun enqueue(): UUID? {
        val spec = catalog.current() ?: return null
        val input = Data.Builder()
            .putString(KEY_MODEL_ID, spec.modelId)
            .putString(KEY_VERSION, spec.version)
            .putString(KEY_RUNTIME_FORMAT, spec.runtimeFormat)
            .putString(KEY_SHA256, spec.sha256)
            .putLong(KEY_BYTES, spec.bytes)
            .putString(KEY_LICENSE, spec.license)
            .putString(KEY_SOURCE, spec.source)
            .putInt(KEY_CONTEXT_LIMIT, spec.contextLimit)
            .putStringArray(KEY_BACKENDS, spec.supportedBackends.toTypedArray())
            .putString(KEY_URL, spec.downloadUrl)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(input)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return request.id
    }

    companion object {
        const val WORK_NAME = "pinhoquest-creative-model-install"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_VERSION = "version"
        const val KEY_RUNTIME_FORMAT = "runtime_format"
        const val KEY_SHA256 = "sha256"
        const val KEY_BYTES = "bytes"
        const val KEY_LICENSE = "license"
        const val KEY_SOURCE = "source"
        const val KEY_CONTEXT_LIMIT = "context_limit"
        const val KEY_BACKENDS = "backends"
        const val KEY_URL = "url"
    }
}

object UnconfiguredModelPackageCatalog : ModelPackageCatalog {
    override fun current(): ModelDownloadSpec? = null
}