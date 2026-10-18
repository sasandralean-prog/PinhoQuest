package com.pinhoquest.flora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic photo-to-pixel-art conversion. The remover is intentionally
 * conservative: it removes only edge-connected pixels similar to the sampled
 * border colour. Photos whose background cannot be safely isolated are rejected
 * so the app keeps its existing emoji placeholder instead of shipping a bad cutout.
 */
internal object FlowerPixelArtProcessor {
    const val PIPELINE_VERSION = "flower-pixel-v1"
    private const val MAX_DECODE_SIDE = 480
    private const val SPRITE_SIZE = 96
    private const val CONTENT_SIZE = 78
    private const val PALETTE_SIZE = 24
    private const val BACKGROUND_DISTANCE_SQUARED = 52 * 52

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
        val written = FileOutputStream(output).use { stream ->
            val ok = pixelArt.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            ok
        }
        pixelArt.recycle()
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

        val borderColours = ArrayList<Int>()
        val step = max(1, min(width, height) / 160)
        for (x in 0 until width step step) {
            borderColours += pixels[x]
            borderColours += pixels[(height - 1) * width + x]
        }
        for (y in 0 until height step step) {
            borderColours += pixels[y * width]
            borderColours += pixels[y * width + width - 1]
        }
        val background = medianColour(borderColours)
        val removed = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        var read = 0
        var write = 0

        fun seed(index: Int) {
            if (!removed[index] && isNearBackground(pixels[index], background)) {
                removed[index] = true
                queue[write++] = index
            }
        }

        for (x in 0 until width) {
            seed(x)
            seed((height - 1) * width + x)
        }
        for (y in 0 until height) {
            seed(y * width)
            seed(y * width + width - 1)
        }

        while (read < write) {
            val index = queue[read++]
            val x = index % width
            val y = index / width
            if (x > 0) seed(index - 1)
            if (x + 1 < width) seed(index + 1)
            if (y > 0) seed(index - width)
            if (y + 1 < height) seed(index + width)
        }

        val remaining = pixels.size - write
        val removedRatio = write.toFloat() / pixels.size.toFloat()
        val foregroundRatio = remaining.toFloat() / pixels.size.toFloat()
        // Reject no-op masks and near-empty cutouts, both of which look broken.
        if (removedRatio < 0.015f || foregroundRatio < 0.025f) return null

        var left = width
        var top = height
        var right = -1
        var bottom = -1
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
            }
        }
        if (right <= left || bottom <= top) return null
        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1
        if (cropWidth < 8 || cropHeight < 8) return null

        val cropped = Bitmap.createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888,
        )
        val result = Bitmap.createBitmap(cropped, left, top, cropWidth, cropHeight)
        cropped.recycle()
        return result
    }

    private fun medianColour(colours: List<Int>): Int {
        fun median(channel: (Int) -> Int): Int {
            val values = colours.map(channel).sorted()
            return values[values.size / 2]
        }
        return Color.rgb(median(Color::red), median(Color::green), median(Color::blue))
    }

    private fun isNearBackground(pixel: Int, background: Int): Boolean {
        if (Color.alpha(pixel) < 16) return true
        val dr = Color.red(pixel) - Color.red(background)
        val dg = Color.green(pixel) - Color.green(background)
        val db = Color.blue(pixel) - Color.blue(background)
        return dr * dr + dg * dg + db * db <= BACKGROUND_DISTANCE_SQUARED
    }

    private fun composePixelArt(crop: Bitmap): Bitmap? {
        val scale = min(
            CONTENT_SIZE.toFloat() / crop.width.toFloat(),
            CONTENT_SIZE.toFloat() / crop.height.toFloat(),
        )
        val targetWidth = max(1, (crop.width * scale).toInt())
        val targetHeight = max(1, (crop.height * scale).toInt())
        // Nearest-neighbour scaling is deliberate: it gives the sprite hard pixel edges.
        val small = Bitmap.createScaledBitmap(crop, targetWidth, targetHeight, false)
        val xOffset = (SPRITE_SIZE - targetWidth) / 2
        val yOffset = (SPRITE_SIZE - targetHeight) / 2
        val sprite = Bitmap.createBitmap(SPRITE_SIZE, SPRITE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(sprite)
        canvas.drawBitmap(small, xOffset.toFloat(), yOffset.toFloat(), null)
        small.recycle()

        val pixels = IntArray(SPRITE_SIZE * SPRITE_SIZE)
        sprite.getPixels(pixels, 0, SPRITE_SIZE, 0, 0, SPRITE_SIZE, SPRITE_SIZE)
        val histogram = HashMap<Int, Int>()
        for (i in pixels.indices) {
            if (Color.alpha(pixels[i]) < 16) {
                pixels[i] = Color.TRANSPARENT
                continue
            }
            val reduced = reduceColour(pixels[i])
            pixels[i] = reduced
            histogram[reduced] = (histogram[reduced] ?: 0) + 1
        }
        if (histogram.isEmpty()) {
            sprite.recycle()
            return null
        }
        val palette = histogram.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(PALETTE_SIZE)
            .map { it.key }

        for (i in pixels.indices) {
            if (Color.alpha(pixels[i]) < 16) continue
            pixels[i] = palette.minByOrNull { colourDistance(pixels[i], it) } ?: pixels[i]
        }
        sprite.setPixels(pixels, 0, SPRITE_SIZE, 0, 0, SPRITE_SIZE, SPRITE_SIZE)
        return sprite
    }

    private fun reduceColour(colour: Int): Int = Color.rgb(
        (Color.red(colour) shr 5) shl 5,
        (Color.green(colour) shr 5) shl 5,
        (Color.blue(colour) shr 5) shl 5,
    )

    private fun colourDistance(a: Int, b: Int): Int {
        val dr = Color.red(a) - Color.red(b)
        val dg = Color.green(a) - Color.green(b)
        val db = Color.blue(a) - Color.blue(b)
        return dr * dr + dg * dg + db * db
    }
}
