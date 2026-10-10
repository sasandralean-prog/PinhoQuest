package com.pinhoquest.model

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.ui.copy.UserFacingCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelInstallCoordinatorTest {
    @Test
    fun cr74CatalogPinsTheValidatedReleaseArtifact() {
        val spec = Cr74SemanticIsolationModelCatalog.current()

        assertNotNull(spec)
        assertEquals("cr74_semantic_isolation", spec.modelId)
        assertEquals("1", spec.version)
        assertEquals("litertlm", spec.runtimeFormat)
        assertEquals("e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb", spec.sha256)
        assertEquals(284_692_656L, spec.bytes)
        assertEquals(1_280, spec.contextLimit)
        assertEquals(listOf("CPU"), spec.supportedBackends)
        assertEquals(
            "https://github.com/sasandralean-prog/PinhoQuest/releases/download/" +
                "cr74-semantic-isolation-v1/pinhoquest-cr74-semantic-isolation-v1.litertlm",
            spec.downloadUrl,
        )
    }

    @Test
    fun validationFailureUsesHumanizedCopyForEveryTechnicalRejection() {
        ModelInstallRejection.entries.forEach { reason ->
            assertEquals(
                "Não consegui preparar o cérebro criativo agora. Você pode tentar de novo daqui a pouco.",
                UserFacingCopy.forModelInstallRejection(reason),
            )
        }
    }
}
