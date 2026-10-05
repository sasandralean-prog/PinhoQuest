package com.pinhoquest.core.backup

import com.pinhoquest.domain.backup.BackupManifest
import com.pinhoquest.domain.backup.BackupReadResult
import com.pinhoquest.domain.backup.BackupSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

class BackupCodec(
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    },
) {
    fun encode(snapshot: BackupSnapshot): ByteArray {
        require(snapshot.manifest.profileId == snapshot.profile.profileId)
        val unsigned = snapshot.copy(manifest = snapshot.manifest.copy(integrityHash = ""))
        val unsignedBytes = json.encodeToString(BackupSnapshot.serializer(), unsigned).toByteArray(Charsets.UTF_8)
        val manifest = snapshot.manifest.copy(integrityHash = sha256(unsignedBytes))
        val signed = snapshot.copy(manifest = manifest)
        val snapshotBytes = json.encodeToString(BackupSnapshot.serializer(), signed).toByteArray(Charsets.UTF_8)
        val manifestBytes = json.encodeToString(BackupManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)

        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("snapshot.json"))
                zip.write(snapshotBytes)
                zip.closeEntry()
            }
            output.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): BackupReadResult {
        return try {
            val entries = readEntries(bytes)
            val manifestBytes = entries["manifest.json"]
                ?: return BackupReadResult.InvalidBackup("Missing manifest")
            val snapshotBytes = entries["snapshot.json"]
                ?: return BackupReadResult.InvalidBackup("Missing snapshot")
            val manifest = json.decodeFromString(
                BackupManifest.serializer(),
                manifestBytes.toString(Charsets.UTF_8),
            )
            val snapshot = json.decodeFromString(
                BackupSnapshot.serializer(),
                snapshotBytes.toString(Charsets.UTF_8),
            )
            if (manifest.formatVersion != FORMAT_VERSION) {
                return BackupReadResult.InvalidBackup("Unsupported backup format")
            }
            if (snapshot.manifest != manifest) {
                return BackupReadResult.InvalidBackup("Manifest mismatch")
            }
            val unsigned = snapshot.copy(manifest = manifest.copy(integrityHash = ""))
            val expected = sha256(
                json.encodeToString(BackupSnapshot.serializer(), unsigned).toByteArray(Charsets.UTF_8),
            )
            if (!MessageDigest.isEqual(
                    expected.toByteArray(Charsets.UTF_8),
                    manifest.integrityHash.toByteArray(Charsets.UTF_8),
                )
            ) {
                return BackupReadResult.InvalidBackup("Integrity check failed")
            }
            BackupReadResult.Valid(snapshot)
        } catch (_: Throwable) {
            BackupReadResult.InvalidBackup("Invalid backup")
        }
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    result[entry.name] = zip.readBytes()
                }
            }
        }
        return result
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val FORMAT_VERSION = 1
    }
}
