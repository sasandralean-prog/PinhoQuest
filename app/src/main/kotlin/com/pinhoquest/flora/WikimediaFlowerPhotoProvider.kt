package com.pinhoquest.flora

import android.text.Html
import java.util.Locale
import org.json.JSONObject

/** Wikimedia Commons search. Queries both an exact phrase and an unquoted scientific name. */
internal object WikimediaFlowerPhotoProvider {
    private const val API = "https://commons.wikimedia.org/w/api.php"

    fun search(scientificName: String): PhotoProviderBatch {
        val normalized = FlowerSpriteIdentity.normalizeScientificName(scientificName)
        if (normalized.isBlank()) return PhotoProviderBatch(emptyList(), 0, 0)
        val taxonTokens = normalizeForMatch(normalized).split(' ').filter(String::isNotBlank)
        val requiredTaxonTokens = taxonTokens.take(2)
        val queries = listOf("\"" + normalized + "\"", normalized)
        val candidates = ArrayList<Pair<Int, FlowerPhotoCandidate>>()
        val seenTitles = HashSet<String>()
        var mediaFound = 0
        var rejected = 0

        for ((queryIndex, searchText) in queries.withIndex()) {
            val params = linkedMapOf(
                "action" to "query",
                "format" to "json",
                "generator" to "search",
                "gsrsearch" to searchText,
                "gsrnamespace" to "6",
                "gsrlimit" to "25",
                "prop" to "imageinfo",
                "iiprop" to "url|size|mime|sha1|extmetadata",
                "iiextmetadatafilter" to "Artist|Credit|LicenseShortName|LicenseUrl|ImageDescription",
                "maxlag" to "5",
            )
            val response = FlowerPhotoHttp.getJson(FlowerPhotoHttp.queryUrl(API, params))
            val pages = response.optJSONObject("query")?.optJSONObject("pages") ?: continue
            val pageIds = pages.keys()
            while (pageIds.hasNext()) {
                val page = pages.optJSONObject(pageIds.next()) ?: continue
                val title = page.optString("title").trim()
                if (title.isBlank() || !seenTitles.add(title.lowercase(Locale.ROOT))) continue
                val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val mime = imageInfo.optString("mime").lowercase(Locale.ROOT)
                if (FlowerPhotoHttp.imageExtension(mime, imageInfo.optString("url")) == null) continue
                val metadata = imageInfo.optJSONObject("extmetadata") ?: JSONObject()
                val description = plain(metadata.value("ImageDescription"))
                val searchable = normalizeForMatch(title + " " + description)

                // Broad search improves recall, but require both genus and species
                // epithet as metadata tokens to avoid silently assigning a sibling species.
                // Punctuation and word order are normalized, so the full phrase need not
                // be contiguous or identically punctuated in the source description.
                val searchableTokens = searchable.split(' ').toSet()
                if (requiredTaxonTokens.size < 2 ||
                    !requiredTaxonTokens.all(searchableTokens::contains)
                ) continue
                mediaFound++

                val rawLicenseName = plain(metadata.value("LicenseShortName"))
                val rawLicenseUrl = plain(metadata.value("LicenseUrl"))
                val licenseForCheck = rawLicenseName.ifBlank { rawLicenseUrl }
                if (!FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(licenseForCheck)) {
                    rejected++
                    continue
                }
                val creator = plain(metadata.value("Artist"))
                val suppliedCredit = plain(metadata.value("Credit"))
                val credit = suppliedCredit.ifBlank { creator }
                if (!FlowerPhotoLicensePolicy.isPublicDomainOrCc0(licenseForCheck) &&
                    creator.isBlank() && credit.isBlank()
                ) {
                    rejected++
                    continue
                }
                val downloadUrl = imageInfo.optString("url").trim()
                if (!downloadUrl.startsWith("https://upload.wikimedia.org/")) continue
                val pageUrl = imageInfo.optString("descriptionurl").ifBlank {
                    "https://commons.wikimedia.org/wiki/" + title.replace(' ', '_')
                }
                val licenseName = FlowerPhotoLicensePolicy.displayName(licenseForCheck)
                val licenseUrl = rawLicenseUrl.ifBlank {
                    FlowerPhotoLicensePolicy.canonicalUrl(licenseForCheck)
                }
                val matchRank = if (searchable.contains(normalizeForMatch(normalized))) 0 else queryIndex + 1
                candidates += matchRank to FlowerPhotoCandidate(
                    providerName = "Wikimedia Commons",
                    fileTitle = title,
                    descriptionPageUrl = pageUrl,
                    downloadUrl = downloadUrl,
                    fileName = title.removePrefix("File:"),
                    mimeType = mime,
                    providerSha1 = imageInfo.optString("sha1"),
                    creator = creator,
                    creditLine = credit,
                    licenseName = licenseName,
                    licenseUrl = licenseUrl,
                    description = description,
                    isShareAlike = FlowerPhotoLicensePolicy.isShareAlike(licenseForCheck),
                    sourceTaxonName = normalized,
                )
            }
        }

        return PhotoProviderBatch(
            candidates = candidates.sortedBy { it.first }.map { it.second },
            mediaFound = mediaFound,
            rejectedByLicense = rejected,
        )
    }

    private fun JSONObject.value(key: String): String =
        optJSONObject(key)?.optString("value").orEmpty()

    @Suppress("DEPRECATION")
    private fun plain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun normalizeForMatch(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
}
