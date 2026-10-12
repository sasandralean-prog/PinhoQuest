package com.pinhoquest.model

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pinhoquest.core.inference.ModelInstallResult
import com.pinhoquest.core.inference.ModelStorePort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
    private val store: ModelStorePort = appContext.modelStore(),
    private val transport: ModelDownloadTransport = ModelDownloadTransport(),
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val spec = inputSpec() ?: return@withContext Result.failure()
        val root = File(applicationContext.filesDir, "model-downloads")
        if (!root.exists() && !root.mkdirs()) return@withContext Result.failure()
        val partial = File(root, spec.modelId + "-" + spec.version + ".part")
        val staged = File(root, spec.modelId + "-" + spec.version + ".ready")
        return@withContext try {
            publishProgress(STAGE_DOWNLOADING, 0, partial.length(), spec.bytes)
            transport.download(spec, partial) { downloaded, total ->
                val percent = ((downloaded * 100L) / total.coerceAtLeast(1L)).toInt().coerceIn(0, 99)
                publishProgress(STAGE_DOWNLOADING, percent, downloaded, total)
            }
            publishProgress(STAGE_VERIFYING, 99, spec.bytes, spec.bytes)
            if (staged.exists()) staged.delete()
            if (!partial.renameTo(staged)) throw IOException("could not stage completed model")
            publishProgress(STAGE_INSTALLING, 99, spec.bytes, spec.bytes)
            when (store.install(staged, spec.toManifest())) {
                is ModelInstallResult.Installed -> {
                    staged.delete()
                    publishProgress(STAGE_INSTALLED, 100, spec.bytes, spec.bytes)
                    Result.success()
                }
                is ModelInstallResult.Rejected -> {
                    staged.delete()
                    Result.failure(androidx.work.workDataOf(KEY_ERROR to "validation_failed"))
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Creative model download will be retried", e)
            setProgress(androidx.work.workDataOf(KEY_STAGE to STAGE_RETRYING, KEY_ERROR to "network_or_io"))
            Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "Creative model installation failed", e)
            Result.failure(androidx.work.workDataOf(KEY_ERROR to "installation_failed"))
        }
    }

    private suspend fun publishProgress(stage: String, percent: Int, downloaded: Long, total: Long) {
        setProgress(
            androidx.work.workDataOf(
                KEY_STAGE to stage,
                KEY_PERCENT to percent,
                KEY_DOWNLOADED to downloaded,
                KEY_TOTAL to total,
            ),
        )
    }

    private fun inputSpec(): ModelDownloadSpec? = runCatching {
        ModelDownloadSpec(
            modelId = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_MODEL_ID)),
            version = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_VERSION)),
            runtimeFormat = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_RUNTIME_FORMAT)),
            sha256 = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_SHA256)),
            bytes = inputData.getLong(ModelInstallCoordinator.KEY_BYTES, -1L).also { require(it > 0L) },
            license = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_LICENSE)),
            source = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_SOURCE)),
            contextLimit = inputData.getInt(ModelInstallCoordinator.KEY_CONTEXT_LIMIT, -1).also { require(it > 0) },
            supportedBackends = inputData.getNullableStringArray(ModelInstallCoordinator.KEY_BACKENDS)?.filterNotNull().orEmpty().also { require(it.isNotEmpty()) },
            downloadUrl = requireNotNull(inputData.getString(ModelInstallCoordinator.KEY_URL)),
        )
    }.getOrNull()

    companion object {
        private const val TAG = "PinhoQuestModel"
        const val KEY_STAGE = "model_install_stage"
        const val KEY_PERCENT = "model_install_percent"
        const val KEY_DOWNLOADED = "model_install_downloaded_bytes"
        const val KEY_TOTAL = "model_install_total_bytes"
        const val KEY_ERROR = "model_install_error"
        const val STAGE_DOWNLOADING = "downloading"
        const val STAGE_VERIFYING = "verifying"
        const val STAGE_INSTALLING = "installing"
        const val STAGE_INSTALLED = "installed"
        const val STAGE_RETRYING = "retrying"
    }
}

private fun Context.modelStore(): ModelStorePort =
    com.pinhoquest.data.model.AndroidModelStore(File(filesDir, "models"))