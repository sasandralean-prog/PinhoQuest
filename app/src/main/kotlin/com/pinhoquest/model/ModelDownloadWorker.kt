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
            transport.download(spec, partial)
            if (staged.exists()) staged.delete()
            if (!partial.renameTo(staged)) throw IOException("could not stage completed model")
            when (store.install(staged, spec.toManifest())) {
                is ModelInstallResult.Installed -> { staged.delete(); Result.success() }
                is ModelInstallResult.Rejected -> { staged.delete(); Result.failure() }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Creative model download will be retried", e)
            Result.retry()
        } catch (e: Exception) {
            Log.w(TAG, "Creative model installation failed", e)
            Result.failure()
        }
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

    companion object { private const val TAG = "PinhoQuestModel" }
}

private fun Context.modelStore(): ModelStorePort =
    com.pinhoquest.data.model.AndroidModelStore(File(filesDir, "models"))