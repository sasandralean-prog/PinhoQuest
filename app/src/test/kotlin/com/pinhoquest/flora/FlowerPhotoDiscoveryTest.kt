package com.pinhoquest.flora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowerPhotoDiscoveryTest {
    @Test
    fun acceptsOnlyRecognizedDerivativeCompatibleLicenses() {
        assertTrue(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC BY 4.0"))
        assertTrue(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC BY-SA 4.0"))
        assertTrue(
            FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(
                "https://creativecommons.org/licenses/by/4.0/",
            ),
        )
        assertTrue(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC0 1.0"))
        assertTrue(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("Public Domain"))

        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(""))
        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("unknown"))
        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("All rights reserved"))
        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC BY-NC 4.0"))
        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC BY-NC-SA 4.0"))
        assertFalse(FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse("CC BY-ND 4.0"))
        assertFalse(
            FlowerPhotoLicensePolicy.allowsDerivativesAndAppUse(
                "https://creativecommons.org/licenses/by-nc/4.0/",
            ),
        )
    }

    @Test
    fun preservesShareAlikeAndCanonicalLicenseInformation() {
        assertTrue(FlowerPhotoLicensePolicy.isShareAlike("CC BY-SA 4.0"))
        assertTrue(
            FlowerPhotoLicensePolicy.isShareAlike(
                "https://creativecommons.org/licenses/by-sa/4.0/",
            ),
        )
        assertFalse(FlowerPhotoLicensePolicy.isShareAlike("CC BY 4.0"))
        assertEquals(
            "https://creativecommons.org/licenses/by-sa/4.0/",
            FlowerPhotoLicensePolicy.canonicalUrl("CC BY-SA 4.0"),
        )
        assertEquals(
            "https://creativecommons.org/publicdomain/zero/1.0/",
            FlowerPhotoLicensePolicy.canonicalUrl("CC0"),
        )
    }

    @Test
    fun displaysLicenseUrisAsReadableCreativeCommonsNames() {
        assertEquals(
            "CC BY 4.0",
            FlowerPhotoLicensePolicy.displayName("https://creativecommons.org/licenses/by/4.0/"),
        )
        assertEquals(
            "CC BY-SA 4.0",
            FlowerPhotoLicensePolicy.displayName("https://creativecommons.org/licenses/by-sa/4.0/"),
        )
    }
}
