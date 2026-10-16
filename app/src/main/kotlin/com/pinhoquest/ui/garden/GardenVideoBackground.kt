package com.pinhoquest.ui.garden

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import java.security.MessageDigest

/**
 * Muted, lifecycle-bound looping Garden scenery. The currently approved static
 * background remains under this surface as the deterministic fallback.
 *
 * The video is an optional binary asset at app/src/main/assets/videos/garden_loop.mp4.
 * Its SHA-256 must match GARDEN_VIDEO_SHA256 before it is copied into app cache
 * and played; missing or invalid assets simply leave the static background visible.
 */
@Composable
internal fun GardenVideoBackground(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val source = remember(context) {
        runCatching { installVerifiedGardenVideo(context) }.getOrNull()
    } ?: return
    val player = remember(context, source) {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                setMediaItem(MediaItem.fromUri(Uri.fromFile(source)))
                prepare()
                playWhenReady = true
            }
        }.getOrNull()
    } ?: return

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { view -> if (view.player !== player) view.player = player },
    )
}

private fun installVerifiedGardenVideo(context: Context): File? {
    val target = File(context.cacheDir, "pinho-garden-loop.mp4")
    if (target.isFile && sha256(target.readBytes()) == GARDEN_VIDEO_SHA256) {
        return target
    }

    // This open will fail safely until the product video is included in app assets.
    val bytes = runCatching {
        context.assets.open("videos/garden_loop.mp4").use { it.readBytes() }
    }.getOrNull() ?: return null
    if (sha256(bytes) != GARDEN_VIDEO_SHA256) return null
    target.writeBytes(bytes)
    return target
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }

private const val GARDEN_VIDEO_SHA256 = "25ed47f10d0fda6f0de8d5a7cb02c207dd5ad9f247e5ac331163e520e539173d"
