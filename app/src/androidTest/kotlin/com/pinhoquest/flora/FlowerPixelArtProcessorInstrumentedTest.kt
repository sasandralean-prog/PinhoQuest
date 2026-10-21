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
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
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
                assertTrue("The compact palette with three shading levels must cap visible colours at 48", visiblePalette.size <= 48)
                assertEquals("The sprite canvas outside the centred content must remain transparent", 0, Color.alpha(sprite.getPixel(0, 0)))
            } finally {
                sprite.recycle()
            }
        } finally {
            directory.deleteRecursively()
        }
    }


    @Test
    fun texturedBorderBackgroundIsRemovedAndShadedSpriteKeepsACompactPalette() {
        val directory = File(context.cacheDir, "pinhoquest-textured-background-regression").apply { mkdirs() }
        val source = File(directory, "textured-source.png")
        val output = File(directory, "textured-sprite.png.part")
        try {
            writeTexturedFlowerPhoto(source, size = 128)
            assertTrue(
                "The multi-colour edge model should extract a flower from a subtly textured background",
                FlowerPixelArtProcessor.process(source, output),
            )

            val sprite = BitmapFactory.decodeFile(output.absolutePath)
            assertNotNull("The processed sprite must be a decodable PNG", sprite)
            sprite!!
            try {
                assertEquals(96, sprite.width)
                assertEquals(96, sprite.height)
                val pixels = IntArray(sprite.width * sprite.height)
                sprite.getPixels(pixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
                val visible = pixels.filter { Color.alpha(it) >= 16 }
                assertTrue("The cutout should retain the flower and leaves", visible.size > 180)
                assertTrue("The exterior must be transparent", pixels.count { Color.alpha(it) == 0 } > pixels.size / 2)
                assertTrue("The median-cut palette plus edge shading should stay compact", visible.toSet().size in 4..48)
                assertEquals("The centred canvas corner must remain transparent", 0, Color.alpha(sprite.getPixel(0, 0)))
            } finally {
                sprite.recycle()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun writeTexturedFlowerPhoto(file: File, size: Int) {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = false
            }
            // Several near-neutral tones exercise multi-colour edge sampling and gentle local drift.
            for (y in 0 until size) {
                paint.color = when ((y / 5) % 3) {
                    0 -> Color.rgb(230, 229, 218)
                    1 -> Color.rgb(222, 226, 213)
                    else -> Color.rgb(236, 233, 222)
                }
                canvas.drawRect(0f, y.toFloat(), size.toFloat(), (y + 1).toFloat(), paint)
            }
            val random = java.util.Random(20261021L)
            repeat(240) {
                val x = random.nextInt(size)
                val y = random.nextInt(size)
                val delta = random.nextInt(13) - 6
                paint.color = Color.rgb(
                    (230 + delta).coerceIn(0, 255),
                    (229 + delta).coerceIn(0, 255),
                    (218 + delta).coerceIn(0, 255),
                )
                canvas.drawRect(x.toFloat(), y.toFloat(), (x + 2).toFloat(), (y + 2).toFloat(), paint)
            }

            val cx = size / 2f
            val cy = size * 0.40f
            paint.color = Color.rgb(173, 48, 79)
            canvas.drawOval(cx - 27f, cy - 13f, cx - 4f, cy + 13f, paint)
            canvas.drawOval(cx + 4f, cy - 13f, cx + 27f, cy + 13f, paint)
            canvas.drawOval(cx - 13f, cy - 27f, cx + 13f, cy - 4f, paint)
            canvas.drawOval(cx - 13f, cy + 4f, cx + 13f, cy + 27f, paint)
            paint.color = Color.rgb(225, 102, 127)
            canvas.drawOval(cx - 21f, cy - 10f, cx - 6f, cy + 4f, paint)
            canvas.drawOval(cx + 6f, cy - 10f, cx + 21f, cy + 4f, paint)
            paint.color = Color.rgb(247, 191, 52)
            canvas.drawCircle(cx, cy, 8f, paint)

            paint.color = Color.rgb(40, 112, 55)
            canvas.drawRect(cx - 3f, cy + 20f, cx + 3f, size - 11f, paint)
            canvas.drawOval(cx - 28f, cy + 33f, cx - 3f, cy + 46f, paint)
            canvas.drawOval(cx + 3f, cy + 43f, cx + 28f, cy + 56f, paint)

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { output ->
                assertTrue("Textured synthetic source should encode", bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun schedulerRegeneratesAValidSpriteWhenThePipelineVersionChanges() {
        val scientificName = "Pipeline Version Regression ${android.os.Process.myPid()}"
        val flowerSha = FlowerSpriteIdentity.flowerSha256(scientificName)
        val directory = File(context.filesDir, "${FlowerSpriteWorker.ROOT_DIRECTORY}/$flowerSha")
        val spriteFile = File(directory, FlowerSpriteWorker.SPRITE_FILE)
        val metadataFile = File(directory, FlowerSpriteWorker.METADATA_FILE)
        val statusFile = File(directory, FlowerSpriteWorker.STATUS_FILE)
        val uniqueWorkName = "flower-sprite-$flowerSha"
        directory.mkdirs()
        try {
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.rgb(46, 126, 58))
                FileOutputStream(spriteFile).use { output ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                }
            } finally {
                bitmap.recycle()
            }
            val spriteSha = FlowerSpriteIdentity.sha256(spriteFile.readBytes())
            metadataFile.writeText(
                JSONObject()
                    .put("flowerSha256", flowerSha)
                    .put("transform", JSONObject().put("pipelineVersion", "flower-pixel-v2"))
                    .put("sprite", JSONObject().put("sha256", spriteSha))
                    .toString(2),
                Charsets.UTF_8,
            )
            // The old pipeline's negative cache must also be ignored after an engine upgrade.
            statusFile.writeText(
                JSONObject()
                    .put("pipelineVersion", "flower-pixel-v2")
                    .put("retryAfterEpochMillis", System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
                    .toString(2),
                Charsets.UTF_8,
            )
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName).result.get(5, TimeUnit.SECONDS)

            FlowerSpriteScheduler(context).scheduleCollection(
                listOf(FlowerSpriteRequest(scientificName, "Regression Flower")),
            )

            val scheduled = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(uniqueWorkName)
                .get(5, TimeUnit.SECONDS)
            assertTrue(
                "A checksum-valid sprite produced by an older pipeline must enqueue regeneration",
                scheduled.isNotEmpty(),
            )
        } finally {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
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
                    .put("transform", JSONObject().put("pipelineVersion", FlowerPixelArtProcessor.PIPELINE_VERSION))
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
                            collectedCount = 1,
                            totalCount = 1,
                            flowers = listOf(
                                GardenFlowerUi(
                                    id = "bitmap-regression-${android.os.Process.myPid()}",
                                    commonName = flowerName,
                                    scientificName = scientificName,
                                    description = "Synthetic regression-test flower",
                                    rarity = FlowerRarity.COMMON,
                                    discoveryState = FlowerDiscoveryState.COLLECTED,
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

            // Four multicolour corner islands force a full-frame crop while testing
            // that only repeated edge colours are treated as background.
            for (corner in 0 until 4) {
                for (dy in 0 until 7) {
                    for (dx in 0 until 7) {
                        val ordinal = corner * 49 + dy * 7 + dx
                        paint.color = Color.rgb(
                            (ordinal % 15) * 16,
                            ((ordinal / 15) % 7) * 16,
                            80 + corner * 16,
                        )
                        val x = if (corner == 0 || corner == 2) dx else size - 7 + dx
                        val y = if (corner == 0 || corner == 1) dy else size - 7 + dy
                        canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
                    }
                }
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