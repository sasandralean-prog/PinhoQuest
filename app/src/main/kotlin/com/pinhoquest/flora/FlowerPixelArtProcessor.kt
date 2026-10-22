package com.pinhoquest.flora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic photo-to-pixel-art conversion.
 *
 * V5 learns background colours from corner patches and restores a small halo of
 * plant-green pixels adjacent to the flower silhouette. This preserves near-flower
 * leaves against similarly green garden backgrounds without keeping the full frame.
 * It uses locally anchored background gradients, removes small foreground islands,
 * renders onto a 48px working grid and adds restrained directional edge lighting.
 * Difficult images are rejected rather than stored as visibly broken sprites.
 */
internal object FlowerPixelArtProcessor {
    private data class ColourWeight(val colour: Int, val count: Int)

    const val PIPELINE_VERSION = "flower-pixel-v5"

    private const val MAX_DECODE_SIDE = 480
    private const val SPRITE_SIZE = 96
    private const val PIXEL_GRID_SIZE = 48
    private const val GRID_CONTENT_SIZE = 39
    private const val PALETTE_SIZE = 16
    private const val BORDER_PALETTE_SIZE = 48
    private const val BACKGROUND_DISTANCE_SQUARED = 54 * 54
    private const val BACKGROUND_DRIFT_DISTANCE_SQUARED = 82 * 82
    private const val LOCAL_BACKGROUND_DISTANCE_SQUARED = 38 * 38

    fun process(source: File, output: File): Boolean {
        val decoded = decodeDownsampled(source) ?: return false
        val isolated = try {
            removeBorderBackground(decoded)
        } finally {
            decoded.recycle()
        } ?: return false

        val pixelArt = try {
            composePixelArt(isolated)
        } finally {
            isolated.recycle()
        } ?: return false

        output.parentFile?.mkdirs()
        val written = try {
            FileOutputStream(output).use { stream ->
                val ok = pixelArt.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                ok
            }
        } finally {
            if (!pixelArt.isRecycled) pixelArt.recycle()
        }
        if (!written || output.length() <= 0L) {
            output.delete()
            return false
        }
        return true
    }

    private fun decodeDownsampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        if (bounds.outWidth > 12_000 || bounds.outHeight > 12_000) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_DECODE_SIDE * 2 ||
            bounds.outHeight / sample > MAX_DECODE_SIDE * 2
        ) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            },
        )
    }

    private fun removeBorderBackground(source: Bitmap): Bitmap? {
        val width = source.width
        val height = source.height
        if (width < 16 || height < 16) return null
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Learn the background from the four corner patches instead of the full
        // perimeter. Leaves and stems often touch the sides; using every border
        // pixel as a seed incorrectly teaches the remover that those plant colours
        // are background.
        val cornerColours = sampleCornerColours(pixels, width, height)
        val backgroundPalette = buildBorderPalette(cornerColours)
        if (backgroundPalette.isEmpty()) return null

        val removed = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        var read = 0
        var write = 0

        fun seed(index: Int) {
            if (!removed[index] && isBackgroundCandidate(pixels[index], backgroundPalette)) {
                removed[index] = true
                queue[write++] = index
            }
        }

        val cornerWidth = max(3, width / 9)
        val cornerHeight = max(3, height / 9)
        for (cornerTop in intArrayOf(0, height - cornerHeight)) {
            for (cornerLeft in intArrayOf(0, width - cornerWidth)) {
                for (y in cornerTop until cornerTop + cornerHeight) {
                    for (x in cornerLeft until cornerLeft + cornerWidth) {
                        seed(y * width + x)
                    }
                }
            }
        }

        fun visitNeighbour(candidateIndex: Int, parentIndex: Int) {
            if (removed[candidateIndex] || Color.alpha(pixels[candidateIndex]) < 16) return
            val candidate = pixels[candidateIndex]
            val nearPalette = minimumPaletteDistance(candidate, backgroundPalette)
            val directMatch = nearPalette <= BACKGROUND_DISTANCE_SQUARED
            // Let smooth gradients propagate, but keep them anchored to sampled border tones.
            val smoothBackgroundDrift =
                nearPalette <= BACKGROUND_DRIFT_DISTANCE_SQUARED &&
                    colourDistance(candidate, pixels[parentIndex]) <= LOCAL_BACKGROUND_DISTANCE_SQUARED
            if (directMatch || smoothBackgroundDrift) {
                removed[candidateIndex] = true
                queue[write++] = candidateIndex
            }
        }

        while (read < write) {
            val index = queue[read++]
            val x = index % width
            val y = index / width
            if (x > 0) visitNeighbour(index - 1, index)
            if (x + 1 < width) visitNeighbour(index + 1, index)
            if (y > 0) visitNeighbour(index - width, index)
            if (y + 1 < height) visitNeighbour(index + width, index)
        }

        // In photos of leafy plants, the leaves around a central flower can have
        // exactly the same colours as the garden behind them. Colour flood-fill alone
        // removes both. Restore only green plant pixels within a bounded halo of the
        // surviving flower silhouette; distant foliage/background stays transparent.
        restoreNearbyGreenFoliage(pixels, removed, width, height)

        val removedCount = removed.count { it }
        val removedRatio = removedCount.toFloat() / pixels.size.toFloat()
        val foregroundRatio = 1f - removedRatio
        if (removedRatio < 0.015f || foregroundRatio < 0.025f) return null

        // Delete small foreground islands (camera noise / texture fragments),
        // including border-touching speckles, while preserving larger connected foliage.
        removeSmallForegroundComponents(pixels, removed, width, height)

        var left = width
        var top = height
        var right = -1
        var bottom = -1
        var visible = 0
        for (index in pixels.indices) {
            if (removed[index]) {
                pixels[index] = Color.TRANSPARENT
            } else {
                val x = index % width
                val y = index / width
                left = min(left, x)
                top = min(top, y)
                right = max(right, x)
                bottom = max(bottom, y)
                visible++
            }
        }
        if (visible < pixels.size * 0.025f || right <= left || bottom <= top) return null
        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1
        if (cropWidth < 8 || cropHeight < 8) return null

        val cropped = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val result = try {
            Bitmap.createBitmap(cropped, left, top, cropWidth, cropHeight)
        } catch (failure: Throwable) {
            if (!cropped.isRecycled) cropped.recycle()
            throw failure
        }
        if (result !== cropped && !cropped.isRecycled) cropped.recycle()
        return result
    }

    private fun restoreNearbyGreenFoliage(
        pixels: IntArray,
        removed: BooleanArray,
        width: Int,
        height: Int,
    ) {
        val distance = IntArray(pixels.size) { -1 }
        val queue = IntArray(pixels.size)
        var read = 0
        var write = 0

        // Seed from the real, non-background flower silhouette. A bounded breadth-first
        // expansion avoids reclassifying green pixels across the entire image as foliage.
        for (index in pixels.indices) {
            if (!removed[index] && Color.alpha(pixels[index]) >= 16) {
                distance[index] = 0
                queue[write++] = index
            }
        }
        if (write == 0) return
        val maximumHaloDistance = max(6, min(width, height) / 16)

        while (read < write) {
            val index = queue[read++]
            val nextDistance = distance[index] + 1
            if (nextDistance > maximumHaloDistance) continue
            val x = index % width
            val y = index / width
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) continue
                    val next = ny * width + nx
                    if (distance[next] >= 0) continue
                    distance[next] = nextDistance
                    queue[write++] = next
                }
            }
        }

        for (index in pixels.indices) {
            if (removed[index] && distance[index] in 1..maximumHaloDistance &&
                isPlantGreen(pixels[index])
            ) {
                removed[index] = false
            }
        }
    }

    private fun isPlantGreen(pixel: Int): Boolean {
        if (Color.alpha(pixel) < 16) return false
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        return green > 45 && green > red + 14 && green > blue + 4
    }

    private fun sampleCornerColours(pixels: IntArray, width: Int, height: Int): List<Int> {
        val cornerWidth = max(3, width / 9)
        val cornerHeight = max(3, height / 9)
        val samples = ArrayList<Int>(cornerWidth * cornerHeight * 4)
        for (cornerTop in intArrayOf(0, height - cornerHeight)) {
            for (cornerLeft in intArrayOf(0, width - cornerWidth)) {
                for (y in cornerTop until cornerTop + cornerHeight) {
                    for (x in cornerLeft until cornerLeft + cornerWidth) {
                        samples += pixels[y * width + x]
                    }
                }
            }
        }
        return samples
    }

    private fun buildBorderPalette(border: List<Int>): List<Int> {
        val counts = HashMap<Int, Int>()
        for (colour in border) {
            if (Color.alpha(colour) < 16) continue
            val key = quantizeCoarse(colour)
            counts[key] = (counts[key] ?: 0) + 1
        }
        if (counts.isEmpty()) return emptyList()

        // A wider palette covers natural edge variation without admitting one-off speckles.
        val minimumFrequency = max(3, border.size / 400)
        val palette = counts.entries
            .filter { it.value >= minimumFrequency }
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(BORDER_PALETTE_SIZE)
            .map { it.key }
            .toMutableList()
        palette.add(quantizeCoarse(medianColour(border)))
        // Avoid allowing a handful of edge speckles to define the background model.
        if (palette.size < 3) {
            counts.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .take(3).mapTo(palette) { it.key }
        }
        return palette.distinct()
    }

    private fun removeSmallForegroundComponents(
        pixels: IntArray,
        removed: BooleanArray,
        width: Int,
        height: Int,
    ) {
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        val minimumComponentSize = max(18, pixels.size / 2000)
        for (start in pixels.indices) {
            if (removed[start] || visited[start] || Color.alpha(pixels[start]) < 16) continue
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
                        if (!removed[next] && !visited[next] && Color.alpha(pixels[next]) >= 16) {
                            visited[next] = true
                            queue[write++] = next
                        }
                    }
                }
            }
            // Edge speckles are common when the photo background has foliage or texture;
            // border contact alone is not enough evidence to keep a tiny island.
            if (write < minimumComponentSize) {
                for (i in 0 until write) removed[queue[i]] = true
            }
        }
    }

    private fun medianColour(colours: List<Int>): Int {
        fun median(channel: (Int) -> Int): Int {
            val values = colours.filter { Color.alpha(it) >= 16 }.map(channel).sorted()
            return if (values.isEmpty()) 0 else values[values.size / 2]
        }
        return Color.rgb(median(Color::red), median(Color::green), median(Color::blue))
    }

    private fun isBackgroundCandidate(pixel: Int, palette: List<Int>): Boolean =
        Color.alpha(pixel) < 16 || minimumPaletteDistance(pixel, palette) <= BACKGROUND_DISTANCE_SQUARED

    private fun minimumPaletteDistance(pixel: Int, palette: List<Int>): Int =
        palette.minOfOrNull { colourDistance(pixel, it) } ?: Int.MAX_VALUE

    private fun composePixelArt(crop: Bitmap): Bitmap? {
        val scale = min(
            GRID_CONTENT_SIZE.toFloat() / crop.width.toFloat(),
            GRID_CONTENT_SIZE.toFloat() / crop.height.toFloat(),
        )
        val targetWidth = max(1, (crop.width * scale).toInt())
        val targetHeight = max(1, (crop.height * scale).toInt())
        val small = Bitmap.createScaledBitmap(crop, targetWidth, targetHeight, false)
        var grid: Bitmap? = null
        try {
            val xOffset = (PIXEL_GRID_SIZE - targetWidth) / 2
            val yOffset = (PIXEL_GRID_SIZE - targetHeight) / 2
            val lowRes = Bitmap.createBitmap(PIXEL_GRID_SIZE, PIXEL_GRID_SIZE, Bitmap.Config.ARGB_8888)
            grid = lowRes
            Canvas(lowRes).drawBitmap(small, xOffset.toFloat(), yOffset.toFloat(), Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            })

            val pixels = IntArray(PIXEL_GRID_SIZE * PIXEL_GRID_SIZE)
            lowRes.getPixels(pixels, 0, PIXEL_GRID_SIZE, 0, 0, PIXEL_GRID_SIZE, PIXEL_GRID_SIZE)
            val quantized = IntArray(pixels.size)
            val histogram = HashMap<Int, Int>()
            for (i in pixels.indices) {
                if (Color.alpha(pixels[i]) < 16) {
                    quantized[i] = Color.TRANSPARENT
                    continue
                }
                val colour = quantizeCoarse(pixels[i])
                quantized[i] = colour
                histogram[colour] = (histogram[colour] ?: 0) + 1
            }
            if (histogram.isEmpty()) return null

            val palette = medianCutPalette(histogram, PALETTE_SIZE)
            for (i in quantized.indices) {
                if (Color.alpha(quantized[i]) < 16) continue
                quantized[i] = palette.minByOrNull { colourDistance(quantized[i], it) } ?: quantized[i]
            }

            val denoised = removeTinyGridComponents(quantized)
            val shaded = applyDirectionalShading(denoised)
            lowRes.setPixels(shaded, 0, PIXEL_GRID_SIZE, 0, 0, PIXEL_GRID_SIZE, PIXEL_GRID_SIZE)
            return Bitmap.createScaledBitmap(lowRes, SPRITE_SIZE, SPRITE_SIZE, false)
        } finally {
            if (small !== crop && !small.isRecycled) small.recycle()
            grid?.let { if (!it.isRecycled) it.recycle() }
            // createScaledBitmap above returns an independently owned 96x96 output.
        }
    }

    private fun medianCutPalette(histogram: Map<Int, Int>, maximum: Int): List<Int> {
        var boxes = listOf(histogram.entries.map { ColourWeight(it.key, it.value) }.sortedBy { it.colour })
        while (boxes.size < maximum) {
            val splittable = boxes.indices
                .filter { boxes[it].size > 1 }
                .maxByOrNull { boxRange(boxes[it]) } ?: break
            val box = boxes[splittable]
            val channel = widestChannel(box)
            val sorted = box.sortedBy { channelValue(it.colour, channel) }
            val halfWeight = max(1, sorted.sumOf { it.count } / 2)
            var accumulated = 0
            var splitAt = 1
            for (i in sorted.indices) {
                accumulated += sorted[i].count
                if (accumulated >= halfWeight) {
                    splitAt = (i + 1).coerceIn(1, sorted.size - 1)
                    break
                }
            }
            if (splitAt !in 1 until sorted.size) break
            boxes = boxes.toMutableList().also {
                it.removeAt(splittable)
                it.add(splittable, sorted.drop(splitAt))
                it.add(splittable, sorted.take(splitAt))
            }
        }
        return boxes.map { box ->
            val total = box.sumOf { it.count }.coerceAtLeast(1)
            fun average(channel: (Int) -> Int): Int =
                box.sumOf { channel(it.colour).toLong() * it.count }.div(total).toInt().coerceIn(0, 255)
            Color.rgb(average(Color::red), average(Color::green), average(Color::blue))
        }.distinct()
    }

    private fun boxRange(box: List<ColourWeight>): Int {
        fun range(channel: Int): Int {
            val values = box.map { channelValue(it.colour, channel) }
            return (values.maxOrNull() ?: 0) - (values.minOrNull() ?: 0)
        }
        return max(range(0), max(range(1), range(2)))
    }

    private fun widestChannel(box: List<ColourWeight>): Int {
        fun range(channel: Int): Int {
            val values = box.map { channelValue(it.colour, channel) }
            return (values.maxOrNull() ?: 0) - (values.minOrNull() ?: 0)
        }
        return (0..2).maxByOrNull(::range) ?: 0
    }

    private fun channelValue(colour: Int, channel: Int): Int = when (channel) {
        0 -> Color.red(colour)
        1 -> Color.green(colour)
        else -> Color.blue(colour)
    }

    private fun quantizeCoarse(colour: Int): Int = Color.rgb(
        (Color.red(colour) shr 4) shl 4,
        (Color.green(colour) shr 4) shl 4,
        (Color.blue(colour) shr 4) shl 4,
    )

    /**
     * Drop one- and two-pixel opaque islands left by textured photo backgrounds
     * after downscaling. Keep tiny components in the flower's centre as pollen/
     * stamens can legitimately be very small at 48px resolution.
     */
    private fun removeTinyGridComponents(pixels: IntArray): IntArray {
        val result = pixels.copyOf()
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        val centreLeft = PIXEL_GRID_SIZE * 30 / 100
        val centreRight = PIXEL_GRID_SIZE * 70 / 100
        val centreTop = PIXEL_GRID_SIZE * 20 / 100
        val centreBottom = PIXEL_GRID_SIZE * 80 / 100

        for (start in pixels.indices) {
            if (visited[start] || Color.alpha(pixels[start]) < 16) continue
            var read = 0
            var write = 0
            var hasCentralDetail = false
            visited[start] = true
            queue[write++] = start
            while (read < write) {
                val index = queue[read++]
                val x = index % PIXEL_GRID_SIZE
                val y = index / PIXEL_GRID_SIZE
                if (x in centreLeft..centreRight && y in centreTop..centreBottom) {
                    hasCentralDetail = true
                }
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until PIXEL_GRID_SIZE || ny !in 0 until PIXEL_GRID_SIZE) continue
                        val next = ny * PIXEL_GRID_SIZE + nx
                        if (!visited[next] && Color.alpha(pixels[next]) >= 16) {
                            visited[next] = true
                            queue[write++] = next
                        }
                    }
                }
            }
            if (write < 3 && !hasCentralDetail) {
                for (i in 0 until write) result[queue[i]] = Color.TRANSPARENT
            }
        }
        return result
    }

    private fun applyDirectionalShading(pixels: IntArray): IntArray {
        val result = pixels.copyOf()
        fun transparent(x: Int, y: Int): Boolean {
            if (x !in 0 until PIXEL_GRID_SIZE || y !in 0 until PIXEL_GRID_SIZE) return true
            return Color.alpha(pixels[y * PIXEL_GRID_SIZE + x]) < 16
        }
        for (y in 0 until PIXEL_GRID_SIZE) {
            for (x in 0 until PIXEL_GRID_SIZE) {
                val index = y * PIXEL_GRID_SIZE + x
                val colour = pixels[index]
                if (Color.alpha(colour) < 16) {
                    result[index] = Color.TRANSPARENT
                    continue
                }
                val topLeftLight = transparent(x, y - 1) || transparent(x - 1, y)
                val bottomRightShadow = transparent(x, y + 1) || transparent(x + 1, y)
                val factor = when {
                    topLeftLight && !bottomRightShadow -> 1.10f
                    bottomRightShadow && !topLeftLight -> 0.88f
                    else -> 1.0f
                }
                result[index] = shadeColour(colour, factor)
            }
        }
        return result
    }

    private fun shadeColour(colour: Int, factor: Float): Int {
        fun channel(value: Int): Int = (((value * factor).toInt() + 8) / 16 * 16).coerceIn(0, 255)
        return Color.rgb(channel(Color.red(colour)), channel(Color.green(colour)), channel(Color.blue(colour)))
    }

    private fun colourDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return dr * dr + dg * dg + db * db
    }
}
