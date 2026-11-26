package com.pinhoquest.flora

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Searches multiple public botanical-image sources, admits only attributable and
 * transformation-compatible photos, and tries multiple photos until the generic
 * pixel-art processor produces a usable flower silhouette.
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
            val search = FlowerPhotoDiscovery.search(scientificName)
            if (search.candidates.isEmpty()) {
                val status = when {
                    search.providerErrors.isNotEmpty() -> "SOURCE_QUERY_FAILED"
                    search.mediaFound > 0 && search.rejectedByLicense >= search.mediaFound ->
                        "ALL_CANDIDATES_REJECTED_LICENSE"
                    search.mediaFound > 0 -> "NO_SUPPORTED_LICENSED_PHOTO"
                    else -> "NO_RESULTS"
                }
                val transientFailure = status == "SOURCE_QUERY_FAILED" &&
                    runAttemptCount < MAX_NETWORK_RETRIES
                val retryAfter = when {
                    transientFailure -> 0L
                    status == "SOURCE_QUERY_FAILED" -> TEMPORARY_ERROR_CACHE_MILLIS
                    status == "ALL_CANDIDATES_REJECTED_LICENSE" -> LICENSE_REJECTION_CACHE_MILLIS
                    else -> NO_RESULT_CACHE_MILLIS
                }
                writeStatus(
                    directory = flowerDirectory,
                    flowerSha = flowerSha,
                    scientificName = scientificName,
                    status = status,
                    detail = buildString {
                        append("providersSucceeded=")
                        append(search.providersSucceeded)
                        append("/")
                        append(search.providersAttempted)
                        append("; mediaFound=")
                        append(search.mediaFound)
                        append("; licenseOrAttributionRejected=")
                        append(search.rejectedByLicense)
                        if (search.providerErrors.isNotEmpty()) {
                            append("; errors=")
                            append(search.providerErrors.joinToString(" | "))
                        }
                    },
                    retryAfterMillis = retryAfter,
                    providerErrors = search.providerErrors,
                    mediaFound = search.mediaFound,
                    rejectedByLicense = search.rejectedByLicense,
                )
                if (transientFailure) return@withContext Result.retry()
                return@withContext Result.success()
            }

            var selectedDownload: DownloadedFlowerPhoto? = null
            var selectedSpritePart: File? = null
            var lastAttemptedDownload: DownloadedFlowerPhoto? = null
            var lastFailure = ""
            var downloadFailures = 0
            var segmentationFailures = 0
            var attempted = 0

            for (candidate in search.candidates.take(FlowerPhotoDiscovery.MAX_CANDIDATE_ATTEMPTS)) {
                attempted++
                val downloaded = try {
                    FlowerPhotoDiscovery.download(candidate, flowerDirectory)
                } catch (failure: IOException) {
                    downloadFailures++
                    lastFailure = candidate.providerName + ": " +
                        (failure.message ?: "image download failed")
                    continue
                } catch (failure: Exception) {
                    downloadFailures++
                    lastFailure = candidate.providerName + ": " +
                        (failure.message ?: failure.javaClass.simpleName)
                    continue
                }
                lastAttemptedDownload = downloaded

                val spritePart = File(flowerDirectory, SPRITE_FILE + ".part")
                spritePart.delete()
                val processed = try {
                    FlowerPixelArtProcessor.process(downloaded.originalFile, spritePart)
                } catch (failure: Exception) {
                    lastFailure = candidate.providerName + ": " +
                        (failure.message ?: "image processing failed")
                    false
                }
                if (processed && spritePart.isFile && spritePart.length() > 0L) {
                    selectedDownload = downloaded
                    selectedSpritePart = spritePart
                    break
                }
                spritePart.delete()
                segmentationFailures++
                lastFailure = candidate.providerName +
                    ": background removal or image quality validation rejected this photo"
            }

            val downloaded = selectedDownload
            val spritePart = selectedSpritePart
            if (downloaded == null || spritePart == null) {
                val onlyDownloadsFailed = downloadFailures > 0 && segmentationFailures == 0
                val transientFailure = onlyDownloadsFailed && runAttemptCount < MAX_NETWORK_RETRIES
                val status = if (onlyDownloadsFailed) "PHOTO_DOWNLOAD_FAILED" else "SEGMENTATION_REJECTED"
                val retryAfter = when {
                    transientFailure -> 0L
                    onlyDownloadsFailed -> TEMPORARY_ERROR_CACHE_MILLIS
                    else -> SEGMENTATION_REJECTION_CACHE_MILLIS
                }
                writeStatus(
                    directory = flowerDirectory,
                    flowerSha = flowerSha,
                    scientificName = scientificName,
                    status = status,
                    detail = "Tried " + attempted + " licensed candidate(s); downloads failed=" +
                        downloadFailures + "; segmentation rejected=" + segmentationFailures +
                        "; lastFailure=" + lastFailure +
                        if (search.providerErrors.isNotEmpty()) {
                            "; providerErrors=" + search.providerErrors.joinToString(" | ")
                        } else {
                            ""
                        },
                    retryAfterMillis = retryAfter,
                    sourceSha256 = lastAttemptedDownload?.sourceSha256,
                    sourcePageUrl = lastAttemptedDownload?.candidate?.descriptionPageUrl,
                    creator = lastAttemptedDownload?.candidate?.creator,
                    licenseName = lastAttemptedDownload?.candidate?.licenseName,
                    providerName = lastAttemptedDownload?.candidate?.providerName,
                    mediaFound = search.mediaFound,
                    rejectedByLicense = search.rejectedByLicense,
                    providerErrors = search.providerErrors,
                )
                if (transientFailure) return@withContext Result.retry()
                return@withContext Result.success()
            }

            val candidate = downloaded.candidate
            val spriteSha256 = sha256File(spritePart)
            val sourceRelativePath = ROOT_DIRECTORY + "/" + flowerSha +
                "/original-" + downloaded.sourceSha256 + "." +
                downloaded.originalFile.extension.ifBlank { "img" }
            val spriteRelativePath = ROOT_DIRECTORY + "/" + flowerSha + "/" + SPRITE_FILE
            val pdOrCc0 = FlowerPhotoLicensePolicy.isPublicDomainOrCc0(candidate.licenseName)
            val attributionText = when {
                candidate.creditLine.isNotBlank() -> candidate.creditLine
                candidate.creator.isNotBlank() -> candidate.creator
                else -> "Autor não informado pela fonte."
            }
            val metadata = JSONObject()
                .put("schemaVersion", 2)
                .put("discoveryVersion", FlowerPhotoDiscovery.DISCOVERY_VERSION)
                .put("flowerSha256", flowerSha)
                .put("scientificName", scientificName)
                .put("normalizedScientificName", FlowerSpriteIdentity.normalizeScientificName(scientificName))
                .put("commonName", commonName)
                .put("identityAlgorithm", "SHA-256 of 'pinhoquest:flower-taxonomy:v1\\n' plus normalized scientific name")
                .put("collectionSha256AtDiscovery", collectionSha)
                .put("retrievedAtUtc", FlowerSpriteIdentity.utcNowIso8601())
                .put("provider", candidate.providerName)
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
                        .put("sourceProvider", candidate.providerName)
                        .put("sourceTaxonName", candidate.sourceTaxonName)
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
                        .put("backgroundRemoval", "corner-seeded multi-colour palette, central bloom-detail protection, bounded nearby green-foliage restoration, locally anchored flood fill, source-resolution component cleanup, and 48px-grid speckle removal")
                        .put("pixelization", "48px working grid, deterministic 16-colour median-cut palette, nearest-neighbour 2x upscale, subtle directional edge lighting and shadow")
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

            writeJsonAtomically(File(flowerDirectory, METADATA_FILE), metadata)
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
                    detail = io.message ?: "Transient photo-source download error.",
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
            val storedPipeline = json.optJSONObject("transform")?.optString("pipelineVersion").orEmpty()
            json.optString("flowerSha256") == flowerSha &&
                storedPipeline == FlowerPixelArtProcessor.PIPELINE_VERSION &&
                storedSpriteSha.isNotBlank() && storedSpriteSha == sha256File(sprite)
        }.getOrDefault(false)
    }

    private fun hasFreshNegativeCache(directory: File): Boolean {
        val status = File(directory, STATUS_FILE)
        if (!status.isFile) return false
        return runCatching {
            val marker = JSONObject(status.readText(StandardCharsets.UTF_8))
            marker.optString("discoveryVersion").toIntOrNull() == FlowerPhotoDiscovery.DISCOVERY_VERSION &&
                marker.optLong("retryAfterEpochMillis", 0L) > System.currentTimeMillis()
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
        providerName: String? = null,
        mediaFound: Int? = null,
        rejectedByLicense: Int? = null,
        providerErrors: List<String> = emptyList(),
    ) {
        val marker = JSONObject()
            .put("schemaVersion", 2)
            .put("discoveryVersion", FlowerPhotoDiscovery.DISCOVERY_VERSION)
            .put("flowerSha256", flowerSha)
            .put("scientificName", scientificName)
            .put("status", status)
            .put("detail", detail)
            .put("pipelineVersion", FlowerPixelArtProcessor.PIPELINE_VERSION)
            .put("updatedAtUtc", FlowerSpriteIdentity.utcNowIso8601())
            .put("retryAfterEpochMillis", System.currentTimeMillis() + retryAfterMillis)
        sourceSha256?.let { marker.put("sourceSha256", it) }
        sourcePageUrl?.let { marker.put("sourcePageUrl", it) }
        creator?.let { marker.put("creator", it) }
        licenseName?.let { marker.put("licenseName", it) }
        providerName?.let { marker.put("providerName", it) }
        mediaFound?.let { marker.put("mediaFound", it) }
        rejectedByLicense?.let { marker.put("rejectedByLicense", it) }
        if (providerErrors.isNotEmpty()) {
            marker.put("providerErrors", JSONArray(providerErrors))
        }
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
        private const val NO_RESULT_CACHE_MILLIS = 3L * 24L * 60L * 60L * 1000L
        private const val LICENSE_REJECTION_CACHE_MILLIS = 3L * 24L * 60L * 60L * 1000L
        private const val SEGMENTATION_REJECTION_CACHE_MILLIS = 24L * 60L * 60L * 1000L
        private const val TEMPORARY_ERROR_CACHE_MILLIS = 6L * 60L * 60L * 1000L
    }
}
