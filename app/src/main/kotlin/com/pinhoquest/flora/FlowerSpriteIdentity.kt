package com.pinhoquest.flora

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/**
 * Stable identity contract for generated flower artwork.
 *
 * A flower SHA is based on the normalized scientific name, not a database UUID
 * or display name. Collection SHA is based on the sorted set of flower SHAs, so
 * ordering and duplicate rows cannot change the identity of the same collection.
 */
object FlowerSpriteIdentity {
    const val IDENTITY_SCHEMA_VERSION = 1

    fun normalizeScientificName(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .replace('_', ' ')
            .trim()
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" ")

    fun flowerSha256(scientificName: String): String {
        val normalized = normalizeScientificName(scientificName)
        require(normalized.isNotBlank()) { "scientific name must not be blank" }
        return sha256("pinhoquest:flower-taxonomy:v$IDENTITY_SCHEMA_VERSION\n$normalized")
    }

    fun collectionSha256(flowerSha256s: Iterable<String>): String {
        val members = flowerSha256s
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
        val payload = buildString {
            append("pinhoquest:flower-collection:v$IDENTITY_SCHEMA_VERSION\n")
            members.forEach { append(it).append('\n') }
        }
        return sha256(payload)
    }

    fun collectionShaFromScientificNames(scientificNames: Iterable<String>): String =
        collectionSha256(scientificNames
            .mapNotNull { name -> name.trim().takeIf(String::isNotBlank)?.let(::flowerSha256) })

    fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
}
