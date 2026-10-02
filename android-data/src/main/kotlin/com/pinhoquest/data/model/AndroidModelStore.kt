package com.pinhoquest.data.model

import com.pinhoquest.core.inference.InstalledModel
import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.inference.ModelInstallResult
import com.pinhoquest.core.inference.ModelStorePort
import com.pinhoquest.domain.model.ModelManifest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class AndroidModelStore(
    private val root: File,
) : ModelStorePort {

    override fun install(stagedFile: File, manifest: ModelManifest): ModelInstallResult {
        if (!stagedFile.exists()) return ModelInstallResult.Rejected(ModelInstallRejection.STAGED_FILE_MISSING)
        if (stagedFile.isDirectory) return ModelInstallResult.Rejected(ModelInstallRejection.STAGED_FILE_IS_DIRECTORY)
        if (stagedFile.name.endsWith(".part")) return ModelInstallResult.Rejected(ModelInstallRejection.PARTIAL_FILE)
        if (stagedFile.length() != manifest.bytes) return ModelInstallResult.Rejected(ModelInstallRejection.SIZE_MISMATCH)
        if (sha256(stagedFile) != manifest.sha256) return ModelInstallResult.Rejected(ModelInstallRejection.HASH_MISMATCH)

        return runCatching {
            if (!root.exists() && !root.mkdirs()) error("cannot create model store")
            val modelRoot = File(root, safeSegment(manifest.modelId))
            if (!modelRoot.exists() && !modelRoot.mkdirs()) error("cannot create model directory")
            val finalDir = File(modelRoot, safeSegment(manifest.version))
            val tempDir = File(modelRoot, "." + safeSegment(manifest.version) + ".installing-" + System.nanoTime())
            if (!tempDir.mkdirs()) error("cannot create staging directory")
            stagedFile.copyTo(File(tempDir, "model"), overwrite = true)
            writeManifest(File(tempDir, "manifest.properties"), manifest)
            if (finalDir.exists()) finalDir.deleteRecursively()
            if (!tempDir.renameTo(finalDir)) error("cannot promote model version")

            val activeTmp = File(modelRoot, ".active.tmp-" + System.nanoTime())
            activeTmp.writeText(manifest.version)
            val activeFile = File(modelRoot, ".active")
            if (activeFile.exists() && !activeFile.delete()) error("cannot replace active marker")
            if (!activeTmp.renameTo(activeFile)) error("cannot promote active marker")
            InstalledModel(manifest, File(finalDir, "model"))
        }.fold(
            onSuccess = { ModelInstallResult.Installed(it) },
            onFailure = { ModelInstallResult.Rejected(ModelInstallRejection.PROMOTION_FAILED) },
        )
    }

    override fun active(): InstalledModel? = runCatching {
        if (!root.isDirectory) return@runCatching null
        root.listFiles()?.firstOrNull { modelRoot ->
            if (!modelRoot.isDirectory) return@firstOrNull false
            val activeFile = File(modelRoot, ".active")
            if (!activeFile.isFile) return@firstOrNull false
            val version = activeFile.readText().trim()
            val versionDir = File(modelRoot, version)
            File(versionDir, "model").isFile && File(versionDir, "manifest.properties").isFile
        }?.let { modelRoot ->
            val version = File(modelRoot, ".active").readText().trim()
            val versionDir = File(modelRoot, version)
            InstalledModel(readManifest(File(versionDir, "manifest.properties")), File(versionDir, "model"))
        }
    }.getOrNull()

    private fun safeSegment(value: String): String {
        require(value.matches(Regex("[A-Za-z0-9._-]+"))) { "invalid model store path segment" }
        return value
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").let { digest ->
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun writeManifest(file: File, manifest: ModelManifest) {
        FileOutputStream(file).use { output ->
            output.write(
                listOf(
                    "modelId=" + manifest.modelId,
                    "version=" + manifest.version,
                    "runtimeFormat=" + manifest.runtimeFormat,
                    "sha256=" + manifest.sha256,
                    "bytes=" + manifest.bytes,
                    "license=" + manifest.license,
                    "source=" + manifest.source,
                    "contextLimit=" + manifest.contextLimit,
                    "supportedBackends=" + manifest.supportedBackends.sorted().joinToString(","),
                ).joinToString("\n").plus("\n").toByteArray(),
            )
        }
    }

    private fun readManifest(file: File): ModelManifest {
        val values = file.readLines().mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
        }.toMap()
        return ModelManifest(
            modelId = requireNotNull(values["modelId"]),
            version = requireNotNull(values["version"]),
            runtimeFormat = requireNotNull(values["runtimeFormat"]),
            sha256 = requireNotNull(values["sha256"]),
            bytes = requireNotNull(values["bytes"]).toLong(),
            license = requireNotNull(values["license"]),
            source = requireNotNull(values["source"]),
            contextLimit = requireNotNull(values["contextLimit"]).toInt(),
            supportedBackends = requireNotNull(values["supportedBackends"]).split(',').filter { it.isNotBlank() }.toSet(),
        )
    }
}