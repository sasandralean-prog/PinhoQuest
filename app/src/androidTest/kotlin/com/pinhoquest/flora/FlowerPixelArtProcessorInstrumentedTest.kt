package com.pinhoquest.flora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
import com.pinhoquest.domain.garden.FlowerDiscoveryState
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.ui.garden.GardenFlowerUi
import com.pinhoquest.ui.garden.GardenScreen
import com.pinhoquest.ui.garden.GardenUiState
import java.io.File
import java.io.FileOutputStream
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowerPixelArtProcessorInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun testScientificName(): String =
        "PinhoQuest Bitmap Ownership Regression ${android.os.Process.myPid()}"

    @Test
    fun fullFrameCropAndIdentityScaleProduceTransparentPixelArtWithoutRecyclingAliases() {
        val directory = File(context.cacheDir, "pinhoquest-bitmap-regression").apply { mkdirs() }
        val source = File(directory, "source.png")
        val output = File(directory, "sprite.png.part")
        try {
            writeSyntheticFlowerPhoto(source, size = 78)

            assertTrue(
                "A valid synthetic flower photo should be extracted and processed",
                FlowerPixelArtProcessor.process(source, output),
            )
            assertTrue("The processed PNG must be written", output.isFile && output.length() > 0L)

            val sprite = BitmapFactory.decodeFile(output.absolutePath)
            assertNotNull("The processed output must decode as a PNG", sprite)
            sprite!!
            try {
                assertEquals(96, sprite.width)
                assertEquals(96, sprite.height)
                assertTrue("Pixel-art output must preserve transparency", sprite.hasAlpha())

                val pixels = IntArray(sprite.width * sprite.height)
                sprite.getPixels(pixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
                val transparentPixels = pixels.count { Color.alpha(it) == 0 }
                val visiblePixels = pixels.count { Color.alpha(it) >= 16 }
                val visiblePalette = pixels.filter { Color.alpha(it) >= 16 }.toSet()

                assertTrue(
                    "The near-white source background should be removed, not baked into the sprite",
                    transparentPixels > pixels.size / 2,
                )
                assertTrue("The flower silhouette must remain visible", visiblePixels > 250)
                assertTrue("Palette reduction must cap visible colours at 24", visiblePalette.size <= 24)
                assertEquals("The sprite canvas outside the centred content must remain transparent", 0, Color.alpha(sprite.getPixel(0, 0)))
            } finally {
                sprite.recycle()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun persistedProcessedSpriteLoadsInsideTheRealGardenFlowerCard() {
        val scientificName = testScientificName()
        val flowerSha = FlowerSpriteIdentity.flowerSha256(scientificName)
        val sourceDirectory = File(context.cacheDir, "pinhoquest-garden-card-regression").apply { mkdirs() }
        val source = File(sourceDirectory, "source.png")
        val part = File(sourceDirectory, "sprite.png.part")
        val flowerDirectory = File(context.filesDir, "${FlowerSpriteWorker.ROOT_DIRECTORY}/$flowerSha")
        try {
            writeSyntheticFlowerPhoto(source, size = 78)
            assertTrue("Photo extraction/pixel-art conversion should succeed", FlowerPixelArtProcessor.process(source, part))

            assertTrue("Create the app's per-flower sprite directory", flowerDirectory.mkdirs() || flowerDirectory.isDirectory)
            val spriteFile = File(flowerDirectory, FlowerSpriteWorker.SPRITE_FILE)
            assertTrue("Publish the completed sprite atomically as the worker does", part.renameTo(spriteFile))
            val spriteSha = FlowerSpriteIdentity.sha256(spriteFile.readBytes())
            File(flowerDirectory, FlowerSpriteWorker.METADATA_FILE).writeText(
                JSONObject()
                    .put("schemaVersion", 1)
                    .put("flowerSha256", flowerSha)
                    .put("sprite", JSONObject().put("sha256", spriteSha))
                    .toString(2),
                Charsets.UTF_8,
            )

            val decoded = BitmapFactory.decodeFile(spriteFile.absolutePath)
            assertNotNull("The sprite file stored for the garden must be readable", decoded)
            decoded!!.recycle()
            val flowerName = "Regression Flower"
            composeRule.setContent {
                MaterialTheme {
                    Box(Modifier.width(360.dp).height(720.dp)) {
                        GardenScreen(
                        state = GardenUiState(
                            ownerName = "Jardim de Teste",
                            lifetimeXp = 50,
                            spendableXp = 50,
                            level = 1,
                            collectedCount = 0,
                            totalCount = 1,
                            flowers = listOf(
                                GardenFlowerUi(
                                    id = "bitmap-regression-${android.os.Process.myPid()}",
                                    commonName = flowerName,
                                    scientificName = scientificName,
                                    description = "Synthetic regression-test flower",
                                    rarity = FlowerRarity.COMMON,
                                    discoveryState = FlowerDiscoveryState.REVEALED,
                                    flowerSha256 = flowerSha,
                                ),
                            ),
                        ),
                            onFlowerSelected = {},
                            onInvestigate = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // The garden's home pane is a collection invitation. Open the collection
            // so the test exercises the actual flower-card image rather than a hidden list.
            composeRule.onNodeWithText("Abrir cole\u00e7\u00e3o").performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithContentDescription(
                    "Pixel art de $flowerName",
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithContentDescription(
                "Pixel art de $flowerName",
                useUnmergedTree = true,
            ).assertIsDisplayed()
        } finally {
            runCatching { composeRule.runOnIdle { composeRule.setContent {} } }
            sourceDirectory.deleteRecursively()
            flowerDirectory.deleteRecursively()
        }
    }

    private fun writeSyntheticFlowerPhoto(file: File, size: Int) {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(238, 238, 232))
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = false
            }
            val center = size / 2f

            // Distinct petals, centre and stem make background removal and palette reduction observable.
            paint.color = Color.rgb(202, 61, 91)
            canvas.drawCircle(center, center - 11f, 10f, paint)
            canvas.drawCircle(center + 11f, center, 10f, paint)
            canvas.drawCircle(center, center + 11f, 10f, paint)
            canvas.drawCircle(center - 11f, center, 10f, paint)
            paint.color = Color.rgb(248, 190, 48)
            canvas.drawCircle(center, center, 6f, paint)
            paint.color = Color.rgb(42, 116, 52)
            canvas.drawRect(center - 2f, center + 8f, center + 2f, size - 8f, paint)

            // Four isolated edge pixels force a full-frame crop while leaving most background connected.
            for ((x, y) in listOf(0 to 0, size - 1 to 0, 0 to size - 1, size - 1 to size - 1)) {
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { output ->
                assertTrue("Synthetic source image must encode successfully", bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }
}