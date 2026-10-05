package com.pinhoquest.core.backup

import com.pinhoquest.domain.backup.BackupManifest
import com.pinhoquest.domain.backup.BackupProfile
import com.pinhoquest.domain.backup.BackupReadResult
import com.pinhoquest.domain.backup.BackupSnapshot
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()
    private val json = Json { encodeDefaults = true }

    @Test
    fun roundTripPreservesProfileAndMetadata() {
        val source = sample()
        val result = codec.decode(codec.encode(source))
        assertTrue(result is BackupReadResult.Valid)
        result as BackupReadResult.Valid
        assertEquals(source.profile, result.snapshot.profile)
        assertEquals(source.manifest.copy(integrityHash = result.snapshot.manifest.integrityHash), result.snapshot.manifest)
    }

    @Test
    fun corruptInputIsInvalid() {
        val encoded = codec.encode(sample())
        val corrupt = encoded.clone().also { it[it.lastIndex / 2] = (it[it.lastIndex / 2].toInt() xor 0x7f).toByte() }
        assertTrue(codec.decode(corrupt) is BackupReadResult.InvalidBackup)
    }

    @Test
    fun truncatedInputIsInvalid() {
        val encoded = codec.encode(sample())
        assertTrue(codec.decode(encoded.copyOf(encoded.size / 2)) is BackupReadResult.InvalidBackup)
    }

    @Test
    fun checksumMismatchIsInvalid() {
        val encoded = codec.encode(sample())
        val entries = readEntries(encoded)
        val original = json.decodeFromString(BackupSnapshot.serializer(), entries.getValue("snapshot.json").toString(Charsets.UTF_8))
        val tampered = original.copy(profile = original.profile.copy(gardenOwnerName = "Outro Jardim"))
        val rewritten = zip(entries + ("snapshot.json" to json.encodeToString(BackupSnapshot.serializer(), tampered).toByteArray(Charsets.UTF_8)))
        val result = codec.decode(rewritten)
        assertTrue(result is BackupReadResult.InvalidBackup)
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                result[entry.name] = zip.readBytes()
            }
        }
        return result
    }

    private fun zip(entries: Map<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { out ->
                entries.forEach { (name, bytes) ->
                    out.putNextEntry(ZipEntry(name))
                    out.write(bytes)
                    out.closeEntry()
                }
            }
            output.toByteArray()
        }

    private fun sample() = BackupSnapshot(
        manifest = BackupManifest(1, 4, "profile-1", 7L, 1234L, "1.0.0", mapOf("garden" to 1), ""),
        profile = BackupProfile("profile-1", "Jardim de Rafa", 100L),
    )
}
