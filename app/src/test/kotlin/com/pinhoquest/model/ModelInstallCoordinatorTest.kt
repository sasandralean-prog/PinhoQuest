package com.pinhoquest.model

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.ui.copy.UserFacingCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelInstallCoordinatorTest {
    @Test
    fun unconfiguredCatalogDoesNotEnqueueWork() {
        assertNull(UnconfiguredModelPackageCatalog.current())
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