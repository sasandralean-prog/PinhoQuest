package com.pinhoquest.flora

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Downloads one attributable, transformation-compatible flower photograph, stores
 * the byte-exact original privately, and generates an RGBA pixel-art derivative.
 * Failed discovery leaves placeholders untouched and is negatively cached so the
 * app does not repeat an unsuccessful Commons query on every launch.
 */
class FlowerSpriteWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val scientificName = inputData.getString(KEY_SCIENTIFIC_NAME).orEmpty().trim()
        val commonName = inputData.getString(KEY_COMMON_NAME).orEmpty().trim()
        val flowerSha = inputData.getString(KEY_FLOWER_SHA).orEmpty()
        val collectionSha = inputData.getString(KEY_COLLECTION_SHA).orEmpty()

        if (scientificName.isBlank() || flowerSha != FlowerSpriteIdentity.flowerSha256(scientificName)) {
            return@withContext Result.failure()
        }

        val root = File(applicationContext.filesDir, ROOT_DIRECTORY)
        val flowerDirectory = File(root, flowerSha)
        flowerDirectory.mkdirs()

        if (isCachedSpriteValid(flowerDirectory, flowerSha)) return@withContext Result.success()
        if (hasFreshNegativeCache(flowerDirectory)) return@withContext Result.success()

        try {
            val downloaded = WikimediaFlowerPhotoProvider.findAndDownload(
                scientificName = scientificName,
                destinationDirectory = flowerDirectory,
            )
            if (downloaded == null) {
                writeStatus(
                    directory = flowerDirectory,
                    flowerSha = flowerSha,
                    scientificName = scientificName,
                    status = "NO_ELIGIBLE_MATCHING_PHOTO",
                    detail = "Commons search returned no matching image with recognized derivative-compatible license and attribution metadata.",
                    retryAfterMillis = NO_RESULT_CACHE_MILLIS,
                )
                return@withContext Result.success()
            }

            val spritePart = File(flowerDirectory, "$SPRITE_FILE.part")
            spritePart.delete()
            val processed = FlowerPixelArtProcessor.process(downloaded.originalFile, spritePart)
            if (!processed) {
                spritePart.delete()
                writeStatus(
                    directory = flowerDirectory,
                    flowerSha = flowerSha,
                    scientificName = scientificName,
                    status = "BACKGROUND_REMOVAL_OR_QUALITY_REJECTED",
                    detail = "The conservative edge-background remover could not isolate a usable flower silhouette. The placeholder remains active.",
                    sourceSha256 = downloaded.sourceSha256,
                    sourcePageUrl = downloaded.candidate.descriptionPageUrl,
                    creator = downloaded.candidate.creator,
                    licenseName = downloaded.candidate.licenseName,
                    retryAfterMillis = NO_RESULT_CACHE_MILLIS,
                )
                return@withContext Result.success()
            }

            val spriteSha256 = sha256File(spritePart)
            val candidate = downloaded.candidate
            val sourceRelativePath = ROOT_DIRECTORY + "/" + flowerSha +
                "/original-" + downloaded.sourceSha256 + "." +
                downloaded.originalFile.extension.ifBlank { "img" }
            val spriteRelativePath = ROOT_DIRECTORY + "/" + flowerSha + "/" + SPRITE_FILE
            val pdOrCc0 = candidate.licenseName.lowercase().let {
                it.contains("public domain") || it.contains("public-domain") ||
                    it.contains("cc0") || it.trim() == "pd"
            }
            val attributionText = when {
                candidate.creditLine.isNotBlank() -> candidate.creditLine
                candidate.creator.isNotBlank() -> candidate.creator
                else -> "Autor não informado pela fonte."
            }
            val metadata = JSONObject()
                .put("schemaVersion", 1)
                .put("flowerSha256", flowerSha)
                .put("scientificName", scientificName)
                .put("normalizedScientificName", FlowerSpriteIdentity.normalizeScientificName(scientificName))
                .put("commonName", commonName)
                .put("identityAlgorithm", "SHA-256 of 'pinhoquest:flower-taxonomy:v1\\n' plus normalized scientific name")
                .put("collectionSha256AtDiscovery", collectionSha)
                .put("retrievedAtUtc", Instant.now().toString())
                .put("provider", "Wikimedia Commons Action API")
                .put(
                    "originalPhoto",
                    JSONObject()
                        .put("fileTitle", candidate.fileTitle)
                        .put("originalFileName", candidate.fileName)
                        .put("localRelativePath", sourceRelativePath)
                        .put("mimeType", candidate.mimeType)
                        .put("byteLength", downloaded.originalFile.length())
                        .put("sha256", downloaded.sourceSha256)
                        .put("providerSha1", candidate.providerSha1)
                        .put("sourcePageUrl", candidate.descriptionPageUrl)
                        .put("originalDownloadUrl", candidate.downloadUrl)
                        .put("description", candidate.description),
                )
                .put(
                    "copyright",
                    JSONObject()
                        .put("creator", candidate.creator)
                        .put("creditLine", attributionText)
                        .put("licenseName", candidate.licenseName)
                        .put("licenseUrl", candidate.licenseUrl)
                        .put("sourcePageUrl", candidate.descriptionPageUrl)
                        .put("attributionRequired", !pdOrCc0)
                        .put("shareAlike", candidate.isShareAlike)
                        .put("derivativeLicense", candidate.licenseName)
                        .put(
                            "modificationNotice",
                            "Adapted into pixel art by PinhoQuest: background isolation, crop, downscale, palette reduction and transparent PNG export.",
                        )
                        .put("releaseAllowed", true),
                )
                .put(
                    "transform",
                    JSONObject()
                        .put("pipelineVersion", FlowerPixelArtProcessor.PIPELINE_VERSION)
                        .put("backgroundRemoval", "edge-connected border-colour flood fill; conservative heuristic")
                        .put("pixelization", "nearest-neighbour downscale and deterministic 24-colour palette reduction")
                        .put("transparentBackground", true),
                )
                .put(
                    "sprite",
                    JSONObject()
                        .put("fileName", SPRITE_FILE)
                        .put("localRelativePath", spriteRelativePath)
                        .put("mimeType", "image/png")
                        .put("width", 96)
                        .put("height", 96)
                        .put("sha256", spriteSha256),
                )

            val metadataFile = File(flowerDirectory, METADATA_FILE)
            writeJsonAtomically(metadataFile, metadata)
            val spriteFile = File(flowerDirectory, SPRITE_FILE)
            if (spriteFile.exists()) spriteFile.delete()
            if (!spritePart.renameTo(spriteFile)) {
                spritePart.copyTo(spriteFile, overwrite = true)
                spritePart.delete()
            }
            File(flowerDirectory, STATUS_FILE).delete()
            Result.success()
        } catch (io: IOException) {
            if (runAttemptCount < MAX_NETWORK_RETRIES) {
                Result.retry()
            } else {
                writeStatus(
                    directory = flowerDirectory,
                    flowerSha = flowerSha,
                    scientificName = scientificName,
                    status = "RESEARCH_TEMPORARILY_FAILED",
                    detail = io.message ?: "Transient source download error.",
                    retryAfterMillis = TEMPORARY_ERROR_CACHE_MILLIS,
                )
                Result.success()
            }
        } catch (failure: Exception) {
            writeStatus(
                directory = flowerDirectory,
                flowerSha = flowerSha,
                scientificName = scientificName,
                status = "RESEARCH_PROCESSING_FAILED",
                detail = failure.message ?: failure.javaClass.simpleName,
                retryAfterMillis = TEMPORARY_ERROR_CACHE_MILLIS,
            )
            Result.failure()
        }
    }

    private fun isCachedSpriteValid(directory: File, flowerSha: String): Boolean {
        val sprite = File(directory, SPRITE_FILE)
        val metadata = File(directory, METADATA_FILE)
        if (!sprite.isFile || !metadata.isFile || sprite.length() <= 0L) return false
        return runCatching {
            val json = JSONObject(metadata.readText(StandardCharsets.UTF_8))
            val storedSpriteSha = json.optJSONObject("sprite")?.optString("sha256").orEmpty()
            json.optString("flowerSha256") == flowerSha &&
                storedSpriteSha.isNotBlank() && storedSpriteSha == sha256File(sprite)
        }.getOrDefault(false)
    }

    private fun hasFreshNegativeCache(directory: File): Boolean {
        val status = File(directory, STATUS_FILE)
        if (!status.isFile) return false
        return runCatching {
            JSONObject(status.readText(StandardCharsets.UTF_8))
                .optLong("retryAfterEpochMillis", 0L) > System.currentTimeMillis()
        }.getOrDefault(false)
    }

    private fun writeStatus(
        directory: File,
        flowerSha: String,
        scientificName: String,
        status: String,
        detail: String,
        retryAfterMillis: Long,
        sourceSha256: String? = null,
        sourcePageUrl: String? = null,
        creator: String? = null,
        licenseName: String? = null,
    ) {
        val marker = JSONObject()
            .put("schemaVersion", 1)
            .put("flowerSha256", flowerSha)
            .put("scientificName", scientificName)
            .put("status", status)
            .put("detail", detail)
            .put("updatedAtUtc", Instant.now().toString())
            .put("retryAfterEpochMillis", System.currentTimeMillis() + retryAfterMillis)
        sourceSha256?.let { marker.put("sourceSha256", it) }
        sourcePageUrl?.let { marker.put("sourcePageUrl", it) }
        creator?.let { marker.put("creator", it) }
        licenseName?.let { marker.put("licenseName", it) }
        writeJsonAtomically(File(directory, STATUS_FILE), marker)
    }

    private fun writeJsonAtomically(target: File, json: JSONObject) {
        val temporary = File(target.parentFile, target.name + ".part")
        temporary.writeText(json.toString(2), StandardCharsets.UTF_8)
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    private fun sha256File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        const val TAG = "flower-sprite-research"
        const val ROOT_DIRECTORY = "flora-sprites"
        const val SPRITE_FILE = "sprite.png"
        const val METADATA_FILE = "metadata.json"
        const val STATUS_FILE = "research-status.json"

        const val KEY_SCIENTIFIC_NAME = "scientificName"
        const val KEY_COMMON_NAME = "commonName"
        const val KEY_FLOWER_SHA = "flowerSha256"
        const val KEY_COLLECTION_SHA = "collectionSha256"

        private const val MAX_NETWORK_RETRIES = 2
        private const val NO_RESULT_CACHE_MILLIS = 30L * 24L * 60L * 60L * 1000L
        private const val TEMPORARY_ERROR_CACHE_MILLIS = 6L * 60L * 60L * 1000L
    }
}
