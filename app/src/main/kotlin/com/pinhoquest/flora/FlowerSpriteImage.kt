package com.pinhoquest.flora

import android.graphics.BitmapFactory
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Replaces a discovered-flower emoji with the locally generated transparent PNG.
 * A FileObserver triggers recomposition when the background worker atomically
 * publishes sprite.png. Until then, the provided placeholder remains visible.
 */
@Composable
fun FlowerSpriteImageOrPlaceholder(
    flowerSha256: String,
    placeholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier.size(64.dp),
    placeholderFontSize: TextUnit = 42.sp,
) {
    val context = LocalContext.current
    val directory = remember(flowerSha256) {
        File(context.filesDir, "${FlowerSpriteWorker.ROOT_DIRECTORY}/$flowerSha256")
    }
    var sprite by remember(flowerSha256) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(flowerSha256) {
        directory.mkdirs()
        sprite = withContext(Dispatchers.IO) { readVerifiedSprite(directory, flowerSha256)?.asImageBitmap() }
    }

    DisposableEffect(directory, flowerSha256) {
        directory.mkdirs()
        val observer = object : FileObserver(
            directory.absolutePath,
            CLOSE_WRITE or MOVED_TO or CREATE,
        ) {
            override fun onEvent(event: Int, path: String?) {
                if (path != FlowerSpriteWorker.SPRITE_FILE ||
                    (event and (CLOSE_WRITE or MOVED_TO or CREATE)) == 0
                ) return
                val bitmap = readVerifiedSprite(directory, flowerSha256)
                mainHandler.post { sprite = bitmap?.asImageBitmap() }
            }
        }
        observer.startWatching()
        onDispose { observer.stopWatching() }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val image = sprite
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = placeholder,
                fontSize = placeholderFontSize,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun readVerifiedSprite(directory: File, flowerSha256: String): android.graphics.Bitmap? {
    val image = File(directory, FlowerSpriteWorker.SPRITE_FILE)
    val metadata = File(directory, FlowerSpriteWorker.METADATA_FILE)
    if (!image.isFile || !metadata.isFile) return null
    return runCatching {
        val record = JSONObject(metadata.readText(StandardCharsets.UTF_8))
        val spriteSha = record.optJSONObject("sprite")?.optString("sha256").orEmpty()
        require(record.optString("flowerSha256") == flowerSha256)
        require(spriteSha.isNotBlank())
        require(FlowerSpriteIdentity.sha256(image.readBytes()) == spriteSha)
        BitmapFactory.decodeFile(image.absolutePath)
    }.getOrNull()
}
