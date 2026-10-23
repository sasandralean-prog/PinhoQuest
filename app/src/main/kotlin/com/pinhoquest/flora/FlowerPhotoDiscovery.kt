package com.pinhoquest.flora

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class FlowerPhotoCandidate(
    val providerName: String,
    val fileTitle: String,
    val descriptionPageUrl: String,
    val downloadUrl: String,
    val fileName: String,
    val mimeType: String,
    val providerSha1: String,
    val creator: String,
    val creditLine: String,
    val licenseName: String,
    val licenseUrl: String,
    val description: String,
    val isShareAlike: Boolean,
    val sourceTaxonName: String,
)

internal data class DownloadedFlowerPhoto(
    val candidate: FlowerPhotoCandidate,
    val originalFile: File,
    val sourceSha256: String,
)

internal data class PhotoProviderBatch(
    val candidates: List<FlowerPhotoCandidate>,
    val mediaFound: Int,
    val rejectedByLicense: Int,
)

internal data class FlowerPhotoSearchResult(
    val candidates: List<FlowerPhotoCandidate>,
    val mediaFound: Int,
    val rejectedByLicense: Int,
    val providersSucceeded: Int,
    val providerErrors: List<String>,
    val providersAttempted: Int,
)

/**
 * Runtime photo discovery is separate from the checked-in asset ledger.
 * Only photos with a known derivative-permitting license and sufficient attribution
 * metadata can proceed to processing. NC, ND, unknown and missing licenses are rejected.
 */
internal object FlowerPhotoLicensePolicy {
    fun allowsDerivativesAndAppUse(rawLicense: String): Boolean {
        val value = rawLicense.trim().lowercase(Locale.ROOT)
        if (value.isBlank() ||
            value.contains("unknown") ||
            value.contains("all rights reserved") ||
            value.contains("non-commercial") ||
            value.contains("noncommercial") ||
            Regex("""(^|[^a-z])nc([^a-z]|$)""").containsMatchIn(value) ||
            value.contains("no derivatives") ||
            value.contains("noderivatives") ||
            Regex("""(^|[^a-z])nd([^a-z]|$)""").containsMatchIn(value)
        ) return false

        if (value.contains("public domain") ||
            value.contains("publicdomain") ||
            value.contains("cc0") ||
            value.trim() == "pd"
        ) return true

        val compact = value.replace(Regex("[^a-z0-9]"), "")
        val hasAttributionLicense =
            compact.contains("ccby") ||
                compact.contains("creativecommonsorglicensesby")
        val hasKnownVersion = listOf("10", "20", "25", "30", "40").any(compact::contains)
        return hasAttributionLicense && hasKnownVersion
    }

    fun isPublicDomainOrCc0(rawLicense: String): Boolean {
        val value = rawLicense.lowercase(Locale.ROOT).replace(" ", "").replace("-", "")
        return value.contains("publicdomain") || value.contains("cc0") || value == "pd"
    }

    fun isShareAlike(rawLicense: String): Boolean {
        val value = rawLicense.lowercase(Locale.ROOT)
        val compact = value.replace(Regex("[^a-z0-9]"), "")
        return compact.contains("ccbysa") ||
            compact.contains("creativecommonsorglicensesbysa") ||
            value.contains("by-sa")
    }

    fun displayName(rawLicense: String): String {
        val value = rawLicense.lowercase(Locale.ROOT)
        val compact = value.replace(Regex("[^a-z0-9]"), "")
        if (compact.contains("cc0")) return "CC0"
        if (value.contains("public domain") || compact.contains("publicdomain")) return "Public Domain"
        val version = when {
            compact.contains("40") -> "4.0"
            compact.contains("30") -> "3.0"
            compact.contains("25") -> "2.5"
            compact.contains("20") -> "2.0"
            compact.contains("10") -> "1.0"
            else -> ""
        }
        return when {
            isShareAlike(rawLicense) && version.isNotBlank() -> "CC BY-SA " + version
            compact.contains("ccby") && version.isNotBlank() -> "CC BY " + version
            else -> rawLicense.trim()
        }
    }

    fun canonicalUrl(rawLicense: String): String {
        if (rawLicense.startsWith("https://") || rawLicense.startsWith("http://")) {
            return rawLicense
        }
        val compact = rawLicense.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        if (compact.contains("cc0")) return "https://creativecommons.org/publicdomain/zero/1.0/"
        val version = when {
            compact.contains("40") -> "4.0"
            compact.contains("30") -> "3.0"
            compact.contains("25") -> "2.5"
            compact.contains("20") -> "2.0"
            compact.contains("10") -> "1.0"
            else -> return ""
        }
        val license = if (isShareAlike(rawLicense)) "by-sa" else "by"
        return "https://creativecommons.org/licenses/" + license + "/" + version + "/"
    }
}

internal object FlowerPhotoHttp {
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_ORIGINAL_BYTES = 6L * 1024L * 1024L
    private const val USER_AGENT =
        "PinhoQuest/1.0 (flower photo attribution; https://github.com/sasandralean-prog/PinhoQuest)"

    fun queryUrl(base: String, params: Map<String, String>): String =
        base + "?" + params.entries.joinToString("&") { entry ->
            encode(entry.key) + "=" + encode(entry.value)
        }

    fun getJson(url: String): JSONObject {
        val connection = openConnection(url, "application/json")
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Photo source returned HTTP " + connection.responseCode)
            }
            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    fun download(candidate: FlowerPhotoCandidate, destinationDirectory: File): DownloadedFlowerPhoto {
        if (!candidate.downloadUrl.startsWith("https://", ignoreCase = true)) {
            throw IOException("Refusing a photo URL that does not use HTTPS")
        }
        val extension = imageExtension(candidate.mimeType, candidate.downloadUrl)
            ?: throw IOException("Unsupported image format from " + candidate.providerName)
        destinationDirectory.mkdirs()
        val temporary = File(destinationDirectory, "original-download.part")
        temporary.delete()
        val connection = openConnection(candidate.downloadUrl, "image/jpeg,image/png,image/webp")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException(candidate.providerName + " image returned HTTP " + connection.responseCode)
            }
            if (connection.contentLengthLong > MAX_ORIGINAL_BYTES) {
                throw IOException("Flower image exceeds the local download size limit")
            }
            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { fileOutput ->
                    DigestOutputStream(fileOutput, digest).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_ORIGINAL_BYTES) {
                                throw IOException("Flower image exceeds the local download size limit")
                            }
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    }
                }
            }
            if (totalBytes <= 0L) throw IOException(candidate.providerName + " returned an empty image")
            val sourceSha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            val original = File(destinationDirectory, "original-" + sourceSha256 + "." + extension)
            if (original.exists()) {
                temporary.delete()
            } else if (!temporary.renameTo(original)) {
                temporary.copyTo(original, overwrite = true)
                temporary.delete()
            }
            return DownloadedFlowerPhoto(candidate, original, sourceSha256)
        } catch (failure: Throwable) {
            temporary.delete()
            throw failure
        } finally {
            connection.disconnect()
        }
    }

    fun imageExtension(mimeType: String, url: String): String? {
        when (mimeType.substringBefore(";").trim().lowercase(Locale.ROOT)) {
            "image/jpeg", "image/jpg" -> return "jpg"
            "image/png" -> return "png"
            "image/webp" -> return "webp"
        }
        val path = runCatching { URL(url).path.lowercase(Locale.ROOT) }.getOrDefault("")
        return when {
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
            path.endsWith(".png") -> "png"
            path.endsWith(".webp") -> "webp"
            else -> null
        }
    }

    private fun openConnection(url: String, accept: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", accept)
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

internal object GbifFlowerPhotoProvider {
    private const val API = "https://api.gbif.org/v1/occurrence/search"

    fun search(scientificName: String): PhotoProviderBatch {
        val normalized = FlowerSpriteIdentity.normalizeScientificName(scientificName)
        if (normalized.isBlank()) return PhotoProviderBatch(emptyList(), 0, 0)
        val response = FlowerPhotoHttp.getJson(
            FlowerPhotoHttp.queryUrl(
                API,
                linkedMapOf(
                    "scientificName" to normalized,
                    "mediaType" to "StillImage",
                    "limit" to "60",
                    "offset" to "0",
                ),
            ),
        )
        val records = response.optJSONArray("results") ?: JSONArray()
        val candidates = ArrayList<FlowerPhotoCandidate>()
        var mediaFound = 0
        var rejected = 0

        for (recordIndex in 0 until records.length()) {
            val occurrence = records.optJSONObject(recordIndex) ?: continue
            val media = occurrence.optJSONArray("media") ?: continue
            val occurrenceKey = occurrence.optString("key").trim()
            val occurrencePage = if (occurrenceKey.isNotBlank()) {
                "https://www.gbif.org/occurrence/" + occurrenceKey
            } else {
                occurrence.optString("references").takeIf { it.startsWith("https://") }.orEmpty()
            }
            val taxonName = occurrence.optString("scientificName")
                .ifBlank { occurrence.optString("acceptedScientificName") }
                .ifBlank { normalized }
            val datasetName = occurrence.optString("datasetName").ifBlank {
                occurrence.optString("publisher").ifBlank { "GBIF occurrence media" }
            }

            for (mediaIndex in 0 until media.length()) {
                val item = media.optJSONObject(mediaIndex) ?: continue
                val identifier = item.optString("identifier").trim()
                if (!identifier.startsWith("https://", ignoreCase = true)) continue
                val format = item.optString("format").ifBlank { inferMimeFromUrl(identifier) }
                if (FlowerPhotoHttp.imageExtension(format, identifier) == null) continue
                mediaFound++

                // Media-object rights must be explicit. Do not inherit an occurrence or
                // dataset license silently, because the image may have different rights.
                val rawLicense = item.optString("license").trim()
                if (!FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(rawLicense)) {
                    rejected++
                    continue
                }
                val creator = item.optString("creator").ifBlank {
                    item.optString("rightsHolder")
                }.trim()
                val publisher = item.optString("publisher").ifBlank { datasetName }.trim()
                if (!FlowerPhotoLicensePolicy.isPublicDomainOrCc0(rawLicense) && creator.isBlank()) {
                    rejected++
                    continue
                }
                val licenseName = FlowerPhotoLicensePolicy.displayName(rawLicense)
                val credit = buildList {
                    if (creator.isNotBlank()) add(creator)
                    if (publisher.isNotBlank()) add("data/source: " + publisher)
                }.joinToString("; ")
                val mediaTitle = item.optString("title").ifBlank {
                    taxonName + " — GBIF media " + item.optString("identifier")
                }
                val sourcePage = occurrencePage.ifBlank { "https://www.gbif.org/" }
                candidates += FlowerPhotoCandidate(
                    providerName = if (datasetName.contains("plantnet", ignoreCase = true)) {
                        "GBIF / Pl@ntNet dataset"
                    } else {
                        "GBIF / " + datasetName
                    },
                    fileTitle = mediaTitle,
                    descriptionPageUrl = sourcePage,
                    downloadUrl = identifier,
                    fileName = mediaTitle,
                    mimeType = format,
                    providerSha1 = "",
                    creator = creator,
                    creditLine = credit,
                    licenseName = licenseName,
                    licenseUrl = FlowerPhotoLicensePolicy.canonicalUrl(rawLicense),
                    description = item.optString("description").ifBlank { taxonName },
                    isShareAlike = FlowerPhotoLicensePolicy.isShareAlike(rawLicense),
                    sourceTaxonName = taxonName,
                )
            }
        }
        return PhotoProviderBatch(candidates.distinctBy { it.downloadUrl }, mediaFound, rejected)
    }

    private fun inferMimeFromUrl(url: String): String {
        val path = runCatching { URL(url).path.lowercase(Locale.ROOT) }.getOrDefault("")
        return when {
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            else -> ""
        }
    }
}

internal object INaturalistFlowerPhotoProvider {
    private const val API = "https://api.inaturalist.org/v1/observations"

    fun search(scientificName: String): PhotoProviderBatch {
        val normalized = FlowerSpriteIdentity.normalizeScientificName(scientificName)
        if (normalized.isBlank()) return PhotoProviderBatch(emptyList(), 0, 0)
        val response = FlowerPhotoHttp.getJson(
            FlowerPhotoHttp.queryUrl(
                API,
                linkedMapOf(
                    "taxon_name" to normalized,
                    "quality_grade" to "research",
                    "per_page" to "50",
                    "order_by" to "observed_on",
                    "order" to "desc",
                    "photo_license" to "any",
                    "has[]" to "photos",
                ),
            ),
        )
        val records = response.optJSONArray("results") ?: JSONArray()
        val candidates = ArrayList<FlowerPhotoCandidate>()
        var mediaFound = 0
        var rejected = 0

        for (recordIndex in 0 until records.length()) {
            val observation = records.optJSONObject(recordIndex) ?: continue
            val photos = observation.optJSONArray("photos") ?: continue
            val observationId = observation.optString("id").trim()
            if (observationId.isBlank()) continue
            val pageUrl = observation.optString("uri").takeIf {
                it.startsWith("https://www.inaturalist.org/")
            } ?: ("https://www.inaturalist.org/observations/" + observationId)
            val taxon = observation.optJSONObject("taxon")
            val taxonName = taxon?.optString("name").orEmpty().ifBlank { normalized }
            val observerName = observation.optJSONObject("user")?.optString("login").orEmpty()

            for (photoIndex in 0 until photos.length()) {
                val photo = photos.optJSONObject(photoIndex) ?: continue
                val originalUrl = photo.optString("url").trim()
                if (!originalUrl.startsWith("https://", ignoreCase = true)) continue
                val host = runCatching { URL(originalUrl).host.lowercase(Locale.ROOT) }.getOrDefault("")
                // iNaturalist documents this host as its open-photo store. The static
                // image host does not imply open redistribution rights, so reject it.
                if (host != "inaturalist-open-data.s3.amazonaws.com") continue
                val photoUrl = originalUrl.replace(
                    Regex("/(square|small|thumb|medium|large)\\.", RegexOption.IGNORE_CASE),
                    "/original.",
                )
                val extension = FlowerPhotoHttp.imageExtension("", photoUrl) ?: continue
                mediaFound++

                val rawCode = photo.optString("license_code").trim().lowercase(Locale.ROOT)
                val rawLicense = when (rawCode) {
                    "cc0" -> "CC0"
                    "cc-by" -> "CC BY 4.0"
                    "cc-by-sa" -> "CC BY-SA 4.0"
                    else -> rawCode
                }
                if (!FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(rawLicense)) {
                    rejected++
                    continue
                }
                val attribution = photo.optString("attribution").trim()
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                val creator = attribution.ifBlank { observerName }
                if (!FlowerPhotoLicensePolicy.isPublicDomainOrCc0(rawLicense) && creator.isBlank()) {
                    rejected++
                    continue
                }
                val photoId = photo.optString("id").ifBlank { photoIndex.toString() }
                val licenseName = FlowerPhotoLicensePolicy.displayName(rawLicense)
                val licenseUrl = FlowerPhotoLicensePolicy.canonicalUrl(rawLicense)
                val sourceCredit = if (creator.isBlank()) {
                    "iNaturalist"
                } else {
                    creator + "; via iNaturalist"
                }
                candidates += FlowerPhotoCandidate(
                    providerName = "iNaturalist",
                    fileTitle = taxonName + " — observation " + observationId + " photo " + photoId,
                    descriptionPageUrl = pageUrl,
                    downloadUrl = photoUrl,
                    fileName = "inat-observation-" + observationId + "-" + photoId + "." + extension,
                    mimeType = when (extension) {
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    },
                    providerSha1 = "",
                    creator = creator,
                    creditLine = sourceCredit,
                    licenseName = licenseName,
                    licenseUrl = licenseUrl,
                    description = taxonName,
                    isShareAlike = FlowerPhotoLicensePolicy.isShareAlike(rawLicense),
                    sourceTaxonName = taxonName,
                )
            }
        }
        return PhotoProviderBatch(candidates.distinctBy { it.downloadUrl }, mediaFound, rejected)
    }
}

internal object FlowerPhotoDiscovery {
    const val DISCOVERY_VERSION = 2
    const val MAX_CANDIDATE_ATTEMPTS = 18

    suspend fun search(scientificName: String): FlowerPhotoSearchResult = withContext(Dispatchers.IO) {
        val normalized = FlowerSpriteIdentity.normalizeScientificName(scientificName)
        if (normalized.isBlank()) {
            return@withContext FlowerPhotoSearchResult(emptyList(), 0, 0, 0, emptyList(), 3)
        }
        val providers = listOf(
            "Wikimedia Commons" to { WikimediaFlowerPhotoProvider.search(normalized) },
            "iNaturalist" to { INaturalistFlowerPhotoProvider.search(normalized) },
            "GBIF occurrence media" to { GbifFlowerPhotoProvider.search(normalized) },
        )
        val batches = ArrayList<PhotoProviderBatch>()
        val errors = ArrayList<String>()
        for ((providerName, search) in providers) {
            try {
                batches += search()
            } catch (failure: Exception) {
                val message = failure.message?.take(180).orEmpty().ifBlank {
                    failure.javaClass.simpleName
                }
                errors += providerName + ": " + message
            }
        }
        val unique = LinkedHashMap<String, FlowerPhotoCandidate>()
        for (batch in batches) {
            for (candidate in batch.candidates) {
                unique.putIfAbsent(candidate.downloadUrl, candidate)
            }
        }
        FlowerPhotoSearchResult(
            candidates = unique.values.take(MAX_CANDIDATE_ATTEMPTS),
            mediaFound = batches.sumOf { it.mediaFound },
            rejectedByLicense = batches.sumOf { it.rejectedByLicense },
            providersSucceeded = batches.size,
            providerErrors = errors,
            providersAttempted = providers.size,
        )
    }

    fun download(candidate: FlowerPhotoCandidate, destinationDirectory: File): DownloadedFlowerPhoto =
        FlowerPhotoHttp.download(candidate, destinationDirectory)
}
