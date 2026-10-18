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

    /**
     * Accepts the app's .pqbackup ZIP and a raw JSON BackupSnapshot exported by
     * compatible builds. Both paths validate the manifest, profile identity,
     * format version and SHA-256 integrity before a restore can be attempted.
     */
    fun decode(bytes: ByteArray): BackupReadResult {
        if (bytes.isEmpty()) return BackupReadResult.InvalidBackup("Backup file is empty")
        if (bytes.size > MAX_BACKUP_BYTES) {
            return BackupReadResult.InvalidBackup("Backup file exceeds the 32 MiB size limit")
        }

        return try {
            if (isZip(bytes)) {
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
                validate(snapshot, manifest)
            } else {
                val snapshot = json.decodeFromString(
                    BackupSnapshot.serializer(),
                    bytes.toString(Charsets.UTF_8),
                )
                validate(snapshot, snapshot.manifest)
            }
        } catch (t: Throwable) {
            BackupReadResult.InvalidBackup(
                t.message?.takeIf { it.isNotBlank() }?.take(180) ?: "Invalid backup",
            )
        }
    }

    private fun validate(snapshot: BackupSnapshot, manifest: BackupManifest): BackupReadResult {
        if (manifest.formatVersion != FORMAT_VERSION) {
            return BackupReadResult.InvalidBackup("Unsupported backup format")
        }
        if (snapshot.manifest != manifest) {
            return BackupReadResult.InvalidBackup("Manifest mismatch")
        }
        if (manifest.profileId.isBlank() || manifest.profileId != snapshot.profile.profileId) {
            return BackupReadResult.InvalidBackup("Profile identity mismatch")
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
        return BackupReadResult.Valid(snapshot)
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        var totalInflatedBytes = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (entry.name !in ALLOWED_ENTRIES) {
                    throw IllegalArgumentException("Unexpected backup entry")
                }
                if (result.containsKey(entry.name)) {
                    throw IllegalArgumentException("Duplicate backup entry")
                }
                val entryBytes = readBoundedEntry(zip, MAX_BACKUP_BYTES - totalInflatedBytes)
                totalInflatedBytes += entryBytes.size
                result[entry.name] = entryBytes
                zip.closeEntry()
            }
        }
        return result
    }

    private fun readBoundedEntry(input: java.io.InputStream, remainingLimit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > remainingLimit) {
                throw IllegalArgumentException("Expanded backup exceeds the size limit")
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == ZIP_MAGIC_0 &&
            bytes[1] == ZIP_MAGIC_1 &&
            bytes[2] == ZIP_MAGIC_2 &&
            bytes[3] == ZIP_MAGIC_3

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val FORMAT_VERSION = 1
        const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
        val ALLOWED_ENTRIES = setOf("manifest.json", "snapshot.json")
        const val ZIP_MAGIC_0: Byte = 0x50
        const val ZIP_MAGIC_1: Byte = 0x4b
        const val ZIP_MAGIC_2: Byte = 0x03
        const val ZIP_MAGIC_3: Byte = 0x04
    }
}
