package com.pinhoquest.data.model

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.inference.ModelInstallResult
import com.pinhoquest.domain.model.ModelManifest
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidModelStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun installAcceptsExactSizeAndHashAndMakesVersionActive() {
        val payload = "model-v1".toByteArray()
        val staged = temporaryFolder.newFile("model.bin").also { it.writeBytes(payload) }
        val manifest = manifestFor(payload, "1")
        val store = AndroidModelStore(temporaryFolder.newFolder("store"))
        val result = store.install(staged, manifest)
        assertTrue(result is ModelInstallResult.Installed)
        val active = requireNotNull(store.active())
        assertEquals(manifest, active.manifest)
        assertEquals("model-v1", active.modelFile.readText())
        assertEquals("model.litertlm", active.modelFile.name)
        assertTrue(File(temporaryFolder.root, "store/creative-model/1/model.litertlm").isFile)
    }

    @Test fun legacyLitertLmFilenameIsMigratedBeforeActivation() {
        val root = temporaryFolder.newFolder("store")
        val store = AndroidModelStore(root)
        val payload = "model-v1".toByteArray()
        val staged = temporaryFolder.newFile("model.bin").also { it.writeBytes(payload) }
        val manifest = manifestFor(payload, "1")
        assertTrue(store.install(staged, manifest) is ModelInstallResult.Installed)

        val versionDir = File(root, "creative-model/1")
        val canonical = File(versionDir, "model.litertlm")
        val legacy = File(versionDir, "model")
        assertTrue(canonical.renameTo(legacy))

        val active = requireNotNull(store.active())
        assertEquals("model.litertlm", active.modelFile.name)
        assertEquals("model-v1", active.modelFile.readText())
        assertTrue(!legacy.exists())
    }

    @Test fun installRejectsWrongHashWithoutChangingExistingActiveVersion() {
        val store = AndroidModelStore(temporaryFolder.newFolder("store"))
        val v1 = "model-v1".toByteArray()
        val stagedV1 = temporaryFolder.newFile("v1.bin").also { it.writeBytes(v1) }
        assertTrue(store.install(stagedV1, manifestFor(v1, "1")) is ModelInstallResult.Installed)
        val v2 = "model-v2".toByteArray()
        val stagedV2 = temporaryFolder.newFile("v2.bin").also { it.writeBytes(v2) }
        val result = store.install(stagedV2, manifestFor(v2, "2").copy(sha256 = "0".repeat(64)))
        assertEquals(ModelInstallRejection.HASH_MISMATCH, (result as ModelInstallResult.Rejected).reason)
        assertEquals("1", store.active()?.manifest?.version)
    }

    @Test fun installRejectsWrongSize() {
        val payload = "model".toByteArray()
        val staged = temporaryFolder.newFile("model.bin").also { it.writeBytes(payload) }
        val store = AndroidModelStore(temporaryFolder.newFolder("store"))
        val result = store.install(staged, manifestFor(payload, "1").copy(bytes = payload.size.toLong() + 1L))
        assertEquals(ModelInstallRejection.SIZE_MISMATCH, (result as ModelInstallResult.Rejected).reason)
        assertEquals(null, store.active())
    }

    @Test fun interruptedPartFileNeverBecomesActive() {
        val store = AndroidModelStore(temporaryFolder.newFolder("store"))
        val partial = temporaryFolder.newFile("model.litertlm.part").also { it.writeText("partial") }
        val result = store.install(partial, manifestFor("partial".toByteArray(), "1"))
        assertEquals(ModelInstallRejection.PARTIAL_FILE, (result as ModelInstallResult.Rejected).reason)
        assertEquals(null, store.active())
    }

    @Test fun installingNewVersionPromotesItAndKeepsPreviousVersion() {
        val root = temporaryFolder.newFolder("store")
        val store = AndroidModelStore(root)
        val v1 = "model-v1".toByteArray()
        store.install(temporaryFolder.newFile("v1.bin").also { it.writeBytes(v1) }, manifestFor(v1, "1"))
        val v2 = "model-v2".toByteArray()
        store.install(temporaryFolder.newFile("v2.bin").also { it.writeBytes(v2) }, manifestFor(v2, "2"))
        val active = requireNotNull(store.active())
        assertEquals("2", active.manifest.version)
        assertEquals("model-v2", active.modelFile.readText())
        assertEquals("model.litertlm", active.modelFile.name)
        assertTrue(File(root, "creative-model/1/model.litertlm").isFile)
        assertTrue(File(root, "creative-model/2/model.litertlm").isFile)
    }

    private fun manifestFor(payload: ByteArray, version: String) = ModelManifest(
        modelId = "creative-model", version = version, runtimeFormat = "litertlm",
        sha256 = sha256(payload), bytes = payload.size.toLong(), license = "Apache-2.0",
        source = "test", contextLimit = 1024, supportedBackends = setOf("CPU"),
    )

    private fun sha256(payload: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(payload).joinToString("") { "%02x".format(it) }
}