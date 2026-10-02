package com.pinhoquest.data.model

import com.pinhoquest.core.inference.InstalledModel
import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.inference.ModelInstallResult
import com.pinhoquest.core.inference.ModelStorePort
import com.pinhoquest.domain.model.ModelManifest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class AndroidModelStore(
    private val root: Path,
) : ModelStorePort {

    override fun install(stagedFile: Path, manifest: ModelManifest): ModelInstallResult {
        if (!Files.exists(stagedFile)) return ModelInstallResult.Rejected(ModelInstallRejection.STAGED_FILE_MISSING)
        if (Files.isDirectory(stagedFile)) return ModelInstallResult.Rejected(ModelInstallRejection.STAGED_FILE_IS_DIRECTORY)
        if (stagedFile.fileName.toString().endsWith(".part")) return ModelInstallResult.Rejected(ModelInstallRejection.PARTIAL_FILE)

        val size = runCatching { Files.size(stagedFile) }.getOrElse {
            return ModelInstallResult.Rejected(ModelInstallRejection.SIZE_MISMATCH)
        }
        if (size != manifest.bytes) return ModelInstallResult.Rejected(ModelInstallRejection.SIZE_MISMATCH)

        val hash = sha256(stagedFile) ?: return ModelInstallResult.Rejected(ModelInstallRejection.HASH_MISMATCH)
        if (hash != manifest.sha256) return ModelInstallResult.Rejected(ModelInstallRejection.HASH_MISMATCH)

        return runCatching {
            Files.createDirectories(root)
            val modelRoot = root.resolve(safeSegment(manifest.modelId))
            Files.createDirectories(modelRoot)
            val finalDir = modelRoot.resolve(safeSegment(manifest.version))
            val tempDir = modelRoot.resolve("." + safeSegment(manifest.version) + ".installing-" + System.nanoTime())
            Files.createDirectories(tempDir)
            Files.copy(stagedFile, tempDir.resolve("model"), StandardCopyOption.REPLACE_EXISTING)
            writeManifest(tempDir.resolve("manifest.properties"), manifest)
            Files.move(tempDir, finalDir, StandardCopyOption.ATOMIC_MOVE)

            val activeTmp = modelRoot.resolve(".active.tmp-" + System.nanoTime())
            Files.write(activeTmp, manifest.version.toByteArray(Charsets.UTF_8))
            Files.move(activeTmp, modelRoot.resolve(".active"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            InstalledModel(manifest, finalDir.resolve("model"))
        }.fold(
            onSuccess = { ModelInstallResult.Installed(it) },
            onFailure = { ModelInstallResult.Rejected(ModelInstallRejection.PROMOTION_FAILED) },
        )
    }

    override fun active(): InstalledModel? = runCatching {
        if (!Files.isDirectory(root)) return@runCatching null
        Files.list(root).use { models ->
            val iterator = models.iterator()
            while (iterator.hasNext()) {
                val modelRoot = iterator.next()
                if (!Files.isDirectory(modelRoot)) continue
                val activeFile = modelRoot.resolve(".active")
                if (!Files.isRegularFile(activeFile)) continue
                val version = String(Files.readAllBytes(activeFile), Charsets.UTF_8).trim()
                if (version.isBlank()) continue
                val versionDir = modelRoot.resolve(version)
                val modelFile = versionDir.resolve("model")
                val manifestFile = versionDir.resolve("manifest.properties")
                if (!Files.isRegularFile(modelFile) || !Files.isRegularFile(manifestFile)) continue
                return@runCatching InstalledModel(readManifest(manifestFile), modelFile)
            }
            null
        }
    }.getOrNull()

    private fun safeSegment(value: String): String {
        require(value.matches(Regex("[A-Za-z0-9._-]+"))) { "invalid model store path segment" }
        return value
    }

    private fun sha256(path: Path): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun writeManifest(path: Path, manifest: ModelManifest) {
        Files.write(
            path,
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
            ).joinToString("\n").toByteArray(Charsets.UTF_8),
        )
    }

    private fun readManifest(path: Path): ModelManifest {
        val values = Files.readAllLines(path).mapNotNull { line ->
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
