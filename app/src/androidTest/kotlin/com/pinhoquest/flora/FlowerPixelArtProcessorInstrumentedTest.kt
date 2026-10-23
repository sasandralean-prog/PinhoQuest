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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Base64
import java.security.MessageDigest
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
    companion object {
        private const val EVIDENCE_LOG_CHUNK_SIZE = 512
    }

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
                val petalTones = visible.filter {
                    Color.red(it) in 128..255 && Color.green(it) in 16..64 && Color.blue(it) in 48..96
                }.toSet()
                assertTrue("Directional lighting should give the flat petal colour more than one shade; count=" + petalTones.size + "; tones=" + petalTones.take(12).joinToString(), petalTones.size >= 2)
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
    fun realCamelliaWithFoliageProducesSpriteAndVisualEvidence() {
        val testDirectory = File(context.cacheDir, "pinhoquest-real-camellia-regression").apply { mkdirs() }
        val source = File(testDirectory, "camellia-original.jpg")
        val spritePart = File(testDirectory, "camellia-sprite.png.part")
        val scientificName = "Camellia granthamiana"
        val commonName = "Grantham's Camellia"
        val flowerSha = FlowerSpriteIdentity.flowerSha256(scientificName)
        val unresolvedScientificName = "Didymocarpus phuquocensis"
        val unresolvedCommonName = "Didymocarpus phuquocensis"
        val unresolvedFlowerSha = FlowerSpriteIdentity.flowerSha256(unresolvedScientificName)
        val flowerDirectory = File(context.filesDir, "${FlowerSpriteWorker.ROOT_DIRECTORY}/$flowerSha")
        val unresolvedFlowerDirectory = File(context.filesDir, "${FlowerSpriteWorker.ROOT_DIRECTORY}/$unresolvedFlowerSha")
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        // Keep evidence in the target app's writable cache and stream it to logcat in
        // small numbered chunks. The CI runner reconstructs and verifies the exact bytes.
        val evidenceDirectory = File(context.cacheDir, "pinhoquest-flower-evidence").apply {
            deleteRecursively()
            mkdirs()
        }
        var generatedWidth = -1
        var generatedHeight = -1
        var visiblePixelCount = 0
        var greenPlantPixels = 0
        var palePetalPixels = 0
        var warmCentrePixels = 0
        var largestOpaqueComponent = 0
        var hasTransparentCanvas = false

        try {
            val testAssets = instrumentationContext.assets
            testAssets.open("flower_fixtures/camellia_with_foliage_original.jpg").use { input ->
                source.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // Always keep the original, even if the processor rejects the crop. This makes
            // source-discovery/processor failures diagnosable in the CI artifact.
            val originalEvidence = File(evidenceDirectory, "camellia-original.jpg")
            originalEvidence.writeBytes(source.readBytes())
            emitFlowerEvidenceFile(originalEvidence, originalEvidence.name)
            assertTrue(
                "The real botanical photo with leaves must be processed by the production engine",
                FlowerPixelArtProcessor.process(source, spritePart),
            )

            val spriteEvidence = File(evidenceDirectory, "camellia-sprite.png")
            spriteEvidence.writeBytes(spritePart.readBytes())
            emitFlowerEvidenceFile(spriteEvidence, spriteEvidence.name)
            val sprite = BitmapFactory.decodeFile(spritePart.absolutePath)
            val original = BitmapFactory.decodeFile(source.absolutePath)
            assertNotNull("The original botanical photo must decode", original)
            assertNotNull("The actual engine output must decode", sprite)
            original!!
            sprite!!
            try {
                generatedWidth = sprite.width
                generatedHeight = sprite.height
                val outputPixels = IntArray(sprite.width * sprite.height)
                sprite.getPixels(outputPixels, 0, sprite.width, 0, 0, sprite.width, sprite.height)
                val visible = outputPixels.filter { Color.alpha(it) >= 16 }
                visiblePixelCount = visible.size
                greenPlantPixels = visible.count {
                    Color.green(it) > Color.red(it) + 14 &&
                        Color.green(it) > Color.blue(it) + 4 &&
                        Color.green(it) > 45
                }
                palePetalPixels = visible.count {
                    minOf(Color.red(it), Color.green(it), Color.blue(it)) >= 150 &&
                        (maxOf(Color.red(it), Color.green(it), Color.blue(it)) -
                            minOf(Color.red(it), Color.green(it), Color.blue(it))) <= 100
                }
                warmCentrePixels = visible.count {
                    Color.red(it) >= 130 &&
                        Color.green(it) >= 75 &&
                        Color.red(it) > Color.green(it) + 18 &&
                        Color.green(it) > Color.blue(it) + 12
                }
                largestOpaqueComponent = largestOpaqueComponent(outputPixels, sprite.width, sprite.height)
                hasTransparentCanvas = outputPixels.any { Color.alpha(it) == 0 }

                // Save diagnostic evidence before assertions, so a failed foliage test still
                // publishes the exact source, processor output and side-by-side comparison.
                val comparisonEvidence = File(evidenceDirectory, "camellia-original-vs-sprite.jpg")
                writeFlowerComparisonEvidence(original, sprite, comparisonEvidence)
                emitFlowerEvidenceFile(comparisonEvidence, comparisonEvidence.name)
            } finally {
                original.recycle()
                sprite.recycle()
            }

            flowerDirectory.mkdirs()
            val publishedSprite = File(flowerDirectory, FlowerSpriteWorker.SPRITE_FILE)
            spritePart.copyTo(publishedSprite, overwrite = true)
            val spriteSha = FlowerSpriteIdentity.sha256(publishedSprite.readBytes())
            File(flowerDirectory, FlowerSpriteWorker.METADATA_FILE).writeText(
                JSONObject()
                    .put("schemaVersion", 1)
                    .put("flowerSha256", flowerSha)
                    .put("scientificName", scientificName)
                    .put(
                        "transform",
                        JSONObject().put("pipelineVersion", FlowerPixelArtProcessor.PIPELINE_VERSION),
                    )
                    .put("sprite", JSONObject().put("sha256", spriteSha))
                    .toString(2),
                Charsets.UTF_8,
            )

            composeRule.setContent {
                MaterialTheme {
                    Box(Modifier.width(360.dp).height(720.dp)) {
                        GardenScreen(
                            state = GardenUiState(
                                ownerName = "Jardim de Teste",
                                lifetimeXp = 50,
                                spendableXp = 50,
                                level = 1,
                                collectedCount = 2,
                                totalCount = 2,
                                flowers = listOf(
                                    GardenFlowerUi(
                                        id = "real-camellia-visual-regression",
                                        commonName = commonName,
                                        scientificName = scientificName,
                                        description = "Real botanical-photo regression fixture",
                                        rarity = FlowerRarity.COMMON,
                                        discoveryState = FlowerDiscoveryState.COLLECTED,
                                        flowerSha256 = flowerSha,
                                    ),
                                    GardenFlowerUi(
                                        id = "unresolved-didymocarpus-placeholder",
                                        commonName = unresolvedCommonName,
                                        scientificName = unresolvedScientificName,
                                        description = "Fixture to verify the unresolved species does not silently masquerade as generated art",
                                        rarity = FlowerRarity.COMMON,
                                        discoveryState = FlowerDiscoveryState.COLLECTED,
                                        flowerSha256 = unresolvedFlowerSha,
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
            composeRule.onNodeWithText("Abrir coleção").performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithContentDescription(
                    "Pixel art de $commonName",
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithContentDescription(
                "Pixel art de $commonName",
                useUnmergedTree = true,
            ).assertIsDisplayed()
            // The second collected flower intentionally has no approved sprite. This
            // screenshot proves the unresolved taxonomy remains a placeholder instead
            // of using the Camellia or some unrelated photo as a false match.
            composeRule.onNodeWithTag("flower-card-unresolved-didymocarpus-placeholder")
                .assertIsDisplayed()
            composeRule.onNodeWithText(unresolvedCommonName).assertIsDisplayed()
            composeRule.onNodeWithText("🌷").assertIsDisplayed()
            val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
            try {
                File(evidenceDirectory, "camellia-garden-collection-screen.jpg").outputStream().use { out ->
                    assertTrue(
                        "The collection screenshot should be written",
                        screenshot.compress(Bitmap.CompressFormat.JPEG, 86, out),
                    )
                }
            } finally {
                screenshot.recycle()
            }

            emitFlowerEvidenceFile(
                File(evidenceDirectory, "camellia-garden-collection-screen.jpg"),
                "camellia-garden-collection-screen.jpg",
            )

            // Assert after all screenshots have been written, so CI artifacts remain
            // inspectable even when the engine erases the foliage.
            assertEquals("The generated sprite width", 96, generatedWidth)
            assertEquals("The generated sprite height", 96, generatedHeight)
            assertTrue(
                "The sprite should preserve a substantial flower silhouette; visible=$visiblePixelCount, pale=$palePetalPixels, largest=$largestOpaqueComponent, green=$greenPlantPixels, warmCentre=$warmCentrePixels",
                visiblePixelCount > 900,
            )
            assertTrue(
                "The output should retain recognisable pale petals; pale pixels=$palePetalPixels",
                palePetalPixels > 220,
            )
            assertTrue(
                "The flower must form a meaningful connected silhouette; largest component=$largestOpaqueComponent",
                largestOpaqueComponent > 400,
            )
            assertTrue(
                "The sprite should retain nearby green leaf/stem detail; green pixels=$greenPlantPixels",
                greenPlantPixels >= 40,
            )
            assertTrue(
                "Green should remain part of a flower sprite, not overwhelm the bloom; visible=$visiblePixelCount green=$greenPlantPixels",
                greenPlantPixels.toFloat() < visiblePixelCount * 0.70f,
            )
            assertTrue(
                "The yellow/orange flower centre should survive; warm pixels=$warmCentrePixels",
                warmCentrePixels >= 80,
            )
            assertTrue("The generated pixel art must retain a transparent canvas", hasTransparentCanvas)
        } finally {
            runCatching { composeRule.runOnIdle { composeRule.setContent {} } }
            testDirectory.deleteRecursively()
            flowerDirectory.deleteRecursively()
            unresolvedFlowerDirectory.deleteRecursively()
        }
    }

    private fun emitFlowerEvidenceFile(file: File, evidenceName: String) {
        if (!file.isFile) {
            android.util.Log.e("PqFlowerEvidence", "Evidence file is missing: $evidenceName")
            return
        }
        val raw = file.readBytes()
        val encoded = Base64.encodeToString(raw, Base64.NO_WRAP)
        val chunkSize = EVIDENCE_LOG_CHUNK_SIZE
        val chunks = encoded.chunked(chunkSize)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw)
            .joinToString("") { byte -> "%02x".format(byte) }
        android.util.Log.i(
            "PqFlowerEvidence",
            "PQFLOWER|BEGIN|$evidenceName|${chunks.size}|${raw.size}|$digest",
        )
        chunks.forEachIndexed { index, chunk ->
            android.util.Log.i(
                "PqFlowerEvidence",
                "PQFLOWER|DATA|$evidenceName|$index|$chunk",
            )
        }
        android.util.Log.i("PqFlowerEvidence", "PQFLOWER|END|$evidenceName|$digest")
    }
    private fun largestOpaqueComponent(pixels: IntArray, width: Int, height: Int): Int {
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        var largest = 0
        for (start in pixels.indices) {
            if (visited[start] || Color.alpha(pixels[start]) < 16) continue
            var read = 0
            var write = 0
            visited[start] = true
            queue[write++] = start
            while (read < write) {
                val index = queue[read++]
                val x = index % width
                val y = index / width
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until width || ny !in 0 until height) continue
                        val next = ny * width + nx
                        if (!visited[next] && Color.alpha(pixels[next]) >= 16) {
                            visited[next] = true
                            queue[write++] = next
                        }
                    }
                }
            }
            largest = maxOf(largest, write)
        }
        return largest
    }

    private fun writeFlowerComparisonEvidence(original: Bitmap, sprite: Bitmap, output: File) {
        val width = 1024
        val height = 600
        val comparison = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(comparison)
            canvas.drawColor(Color.rgb(248, 246, 238))
            val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(50, 54, 48)
                textSize = 30f
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            }
            val subheading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(91, 96, 86)
                textSize = 19f
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }
            canvas.drawText("ORIGINAL PHOTO", 32f, 42f, heading)
            canvas.drawText("GRANTHAM'S CAMELLIA · WIKIMEDIA COMMONS", 32f, 70f, subheading)
            canvas.drawText("PINHOQUEST ENGINE · ${FlowerPixelArtProcessor.PIPELINE_VERSION}", 550f, 42f, heading)
            canvas.drawText("96 × 96 RGBA · enlarged with nearest-neighbour", 550f, 70f, subheading)

            val originalBounds = android.graphics.RectF(28f, 88f, 500f, 570f)
            val originalScale = minOf(
                originalBounds.width() / original.width,
                originalBounds.height() / original.height,
            )
            val originalWidth = original.width * originalScale
            val originalHeight = original.height * originalScale
            val originalRect = android.graphics.RectF(
                originalBounds.centerX() - originalWidth / 2f,
                originalBounds.centerY() - originalHeight / 2f,
                originalBounds.centerX() + originalWidth / 2f,
                originalBounds.centerY() + originalHeight / 2f,
            )
            canvas.drawBitmap(original, null, originalRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

            val tileSize = 24
            for (y in 92 until 572 step tileSize) {
                for (x in 548 until 1012 step tileSize) {
                    val shade = if (((x - 548) / tileSize + (y - 92) / tileSize) % 2 == 0) 238 else 221
                    val tilePaint = Paint().apply { color = Color.rgb(shade, shade, shade) }
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + tileSize).toFloat(), (y + tileSize).toFloat(), tilePaint)
                }
            }
            val spriteScale = minOf(440f / sprite.width, 440f / sprite.height)
            val spriteWidth = sprite.width * spriteScale
            val spriteHeight = sprite.height * spriteScale
            val spriteRect = android.graphics.RectF(
                780f - spriteWidth / 2f,
                332f - spriteHeight / 2f,
                780f + spriteWidth / 2f,
                332f + spriteHeight / 2f,
            )
            canvas.drawBitmap(sprite, null, spriteRect, Paint().apply { isFilterBitmap = false })
            output.parentFile?.mkdirs()
            FileOutputStream(output).use { stream ->
                assertTrue("The visual comparison image should be encoded", comparison.compress(Bitmap.CompressFormat.JPEG, 88, stream))
            }
        } finally {
            comparison.recycle()
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