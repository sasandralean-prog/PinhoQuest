package com.pinhoquest.flora

import android.text.Html
import org.json.JSONObject
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

internal data class WikimediaFlowerPhoto(
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
)

internal data class DownloadedFlowerPhoto(
    val candidate: WikimediaFlowerPhoto,
    val originalFile: File,
    val sourceSha256: String,
)

/** Wikimedia Commons Action API client. Only derivative-permitting, attributable files are admitted. */
internal object WikimediaFlowerPhotoProvider {
    private const val API = "https://commons.wikimedia.org/w/api.php"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 25_000
    private const val MAX_ORIGINAL_BYTES = 6L * 1024L * 1024L

    suspend fun findAndDownload(
        scientificName: String,
        destinationDirectory: File,
    ): DownloadedFlowerPhoto? = withContext(Dispatchers.IO) {
        val candidate = searchEligiblePhoto(scientificName) ?: return@withContext null
        destinationDirectory.mkdirs()
        val extension = extensionForMime(candidate.mimeType) ?: return@withContext null
        val temporary = File(destinationDirectory, "original-download.part")
        temporary.delete()
        val sourceSha256 = downloadWithSha256(candidate.downloadUrl, temporary)
        val original = File(destinationDirectory, "original-$sourceSha256.$extension")
        if (original.exists()) {
            temporary.delete()
        } else if (!temporary.renameTo(original)) {
            temporary.copyTo(original, overwrite = true)
            temporary.delete()
        }
        DownloadedFlowerPhoto(candidate, original, sourceSha256)
    }

    private fun searchEligiblePhoto(scientificName: String): WikimediaFlowerPhoto? {
        val normalized = FlowerSpriteIdentity.normalizeScientificName(scientificName)
        if (normalized.isBlank()) return null
        val params = linkedMapOf(
            "action" to "query",
            "format" to "json",
            "generator" to "search",
            "gsrsearch" to "\"$normalized\"",
            "gsrnamespace" to "6",
            "gsrlimit" to "10",
            "prop" to "imageinfo",
            "iiprop" to "url|size|mime|sha1|extmetadata",
            "iiextmetadatafilter" to "Artist|Credit|LicenseShortName|LicenseUrl|ImageDescription",
            "maxlag" to "5",
        )
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val response = getJson("$API?$query")
        val pages = response.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val pageIds = pages.keys()
        while (pageIds.hasNext()) {
            val page = pages.optJSONObject(pageIds.next()) ?: continue
            val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val mime = imageInfo.optString("mime").lowercase(Locale.ROOT)
            if (extensionForMime(mime) == null) continue
            val metadata = imageInfo.optJSONObject("extmetadata") ?: JSONObject()
            val description = plain(metadata.value("ImageDescription"))
            val title = page.optString("title").orEmpty()
            val searchable = normalizeForMatch("$title $description")
            if (!searchable.contains(normalizeForMatch(normalized))) continue

            val licenseName = plain(metadata.value("LicenseShortName"))
            val licenseUrl = plain(metadata.value("LicenseUrl"))
            if (!allowsDerivativesAndAppUse(licenseName)) continue
            val creator = plain(metadata.value("Artist"))
            val credit = plain(metadata.value("Credit")).ifBlank { creator }
            if (!isPublicDomainOrCc0(licenseName) && creator.isBlank() && credit.isBlank()) continue
            if (!isPublicDomainOrCc0(licenseName) && licenseUrl.isBlank()) continue

            val url = imageInfo.optString("url").orEmpty()
            if (!url.startsWith("https://upload.wikimedia.org/")) continue
            val pageUrl = imageInfo.optString("descriptionurl").ifBlank {
                "https://commons.wikimedia.org/wiki/" + encode(title.replace(' ', '_'))
            }
            val fileName = title.removePrefix("File:").ifBlank {
                "flower-source." + (extensionForMime(mime) ?: "img")
            }
            return WikimediaFlowerPhoto(
                fileTitle = title,
                descriptionPageUrl = pageUrl,
                downloadUrl = url,
                fileName = fileName,
                mimeType = mime,
                providerSha1 = imageInfo.optString("sha1").orEmpty(),
                creator = creator,
                creditLine = credit,
                licenseName = licenseName,
                licenseUrl = licenseUrl,
                description = description,
                isShareAlike = isShareAlikeLicense(licenseName),
            )
        }
        return null
    }

    private fun allowsDerivativesAndAppUse(rawName: String): Boolean {
        val name = rawName.lowercase(Locale.ROOT)
        if (name.isBlank() || name.contains("unknown") ||
            name.contains("non-commercial") || name.contains("noncommercial") ||
            Regex("""(^|[^a-z])nc([^a-z]|$)""").containsMatchIn(name) ||
            name.contains("no derivatives") || name.contains("noderivatives") ||
            Regex("""(^|[^a-z])nd([^a-z]|$)""").containsMatchIn(name)
        ) return false
        if (name.contains("public domain") || name.contains("public-domain") ||
            name.contains("cc0") || name.trim() == "pd"
        ) return true
        val normalized = name.replace("-", "").replace(" ", "").replace("_", "")
        return normalized.startsWith("ccby") &&
            listOf("2.0", "2.5", "3.0", "4.0").any(normalized::contains)
    }

    private fun isShareAlikeLicense(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT).replace("-", "").replace(" ", "")
        return normalized.startsWith("ccbysa")
    }

    private fun isPublicDomainOrCc0(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT).replace(" ", "").replace("-", "")
        return normalized.contains("publicdomain") || normalized.contains("cc0") || normalized == "pd"
    }

    private fun normalizeForMatch(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    private fun JSONObject.value(key: String): String =
        optJSONObject(key)?.optString("value").orEmpty()

    @Suppress("DEPRECATION")
    private fun plain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun extensionForMime(mime: String): String? = when (mime.lowercase(Locale.ROOT)) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> null
    }

    private fun getJson(url: String): JSONObject {
        val connection = openConnection(url, "application/json")
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Wikimedia API returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadWithSha256(url: String, destination: File): String {
        val connection = openConnection(url, "image/jpeg,image/png,image/webp")
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Wikimedia image returned HTTP ${connection.responseCode}")
            }
            if (connection.contentLengthLong > MAX_ORIGINAL_BYTES) {
                throw IOException("Flower image exceeds the local download size limit")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { fileOutput ->
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
            if (totalBytes <= 0L) throw IOException("Wikimedia returned an empty image")
            return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, accept: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "PinhoQuest/1.0 (Android flower-sprite attribution; https://github.com/sasandralean-prog/PinhoQuest)",
            )
            setRequestProperty("Accept", accept)
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
