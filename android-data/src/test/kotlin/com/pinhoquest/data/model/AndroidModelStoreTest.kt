package com.pinhoquest.data.model

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.inference.ModelInstallResult
import com.pinhoquest.domain.model.ModelManifest
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidModelStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun installAcceptsExactSizeAndHashAndMakesVersionActive() {
        val payload = "model-v1".toByteArray()
        val staged = temporaryFolder.newFile("model.bin").toPath().also { Files.write(it, payload) }
        val manifest = manifestFor(payload, version = "1")
        val store = AndroidModelStore(temporaryFolder.newFolder("store").toPath())

        val result = store.install(staged, manifest)

        assertTrue(result is ModelInstallResult.Installed)
        val active = requireNotNull(store.active())
        assertEquals(manifest, active.manifest)
        assertEquals("model-v1", String(Files.readAllBytes(active.modelPath)))
    }

    @Test
    fun installRejectsWrongHashWithoutChangingExistingActiveVersion() {
        val root = temporaryFolder.newFolder("store").toPath()
        val store = AndroidModelStore(root)
        val v1 = "model-v1".toByteArray()
        val stagedV1 = temporaryFolder.newFile("v1.bin").toPath().also { Files.write(it, v1) }
        assertTrue(store.install(stagedV1, manifestFor(v1, "1")) is ModelInstallResult.Installed)

        val v2 = "model-v2".toByteArray()
        val stagedV2 = temporaryFolder.newFile("v2.bin").toPath().also { Files.write(it, v2) }
        val badManifest = manifestFor(v2, "2").copy(sha256 = "0".repeat(64))

        val result = store.install(stagedV2, badManifest)

        assertEquals(ModelInstallRejection.HASH_MISMATCH, (result as ModelInstallResult.Rejected).reason)
        assertEquals("1", store.active()?.manifest?.version)
    }

    @Test
    fun installRejectsWrongSize() {
        val payload = "model".toByteArray()
        val staged = temporaryFolder.newFile("model.bin").toPath().also { Files.write(it, payload) }
        val manifest = manifestFor(payload, "1").copy(bytes = payload.size.toLong() + 1L)
        val store = AndroidModelStore(temporaryFolder.newFolder("store").toPath())

        val result = store.install(staged, manifest)

        assertEquals(ModelInstallRejection.SIZE_MISMATCH, (result as ModelInstallResult.Rejected).reason)
        assertEquals(null, store.active())
    }

    @Test
    fun interruptedPartFileNeverBecomesActive() {
        val root = temporaryFolder.newFolder("store").toPath()
        val store = AndroidModelStore(root)
        val partial = temporaryFolder.newFile("model.litertlm.part").toPath().also { Files.write(it, "partial".toByteArray()) }
        val manifest = manifestFor("partial".toByteArray(), "1")

        val result = store.install(partial, manifest)

        assertEquals(ModelInstallRejection.PARTIAL_FILE, (result as ModelInstallResult.Rejected).reason)
        assertEquals(null, store.active())
    }

    @Test
    fun installingNewVersionAtomicallyPromotesItAndKeepsPreviousUntilPromotionSucceeds() {
        val root = temporaryFolder.newFolder("store").toPath()
        val store = AndroidModelStore(root)
        val v1 = "model-v1".toByteArray()
        val stagedV1 = temporaryFolder.newFile("v1.bin").toPath().also { Files.write(it, v1) }
        store.install(stagedV1, manifestFor(v1, "1"))

        val v2 = "model-v2".toByteArray()
        val stagedV2 = temporaryFolder.newFile("v2.bin").toPath().also { Files.write(it, v2) }
        store.install(stagedV2, manifestFor(v2, "2"))

        val active = requireNotNull(store.active())
        assertEquals("2", active.manifest.version)
        assertEquals("model-v2", String(Files.readAllBytes(active.modelPath)))
        assertTrue(Files.exists(root.resolve("creative-model").resolve("1").resolve("model")))
        assertTrue(Files.exists(root.resolve("creative-model").resolve("2").resolve("model")))
    }

    private fun manifestFor(payload: ByteArray, version: String): ModelManifest = ModelManifest(
        modelId = "creative-model",
        version = version,
        runtimeFormat = "litertlm",
        sha256 = sha256(payload),
        bytes = payload.size.toLong(),
        license = "Apache-2.0",
        source = "test",
        contextLimit = 1024,
        supportedBackends = setOf("CPU"),
    )

    private fun sha256(payload: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(payload)
        .joinToString("") { "%02x".format(it) }
}