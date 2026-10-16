package com.pinhoquest.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pinhoquest.R

/**
 * Muted, looping garden video rendered through TextureView so Compose can draw
 * the screen's controls and labels over it without SurfaceView z-order issues.
 * Player resources are released as soon as the TextureView surface is destroyed.
 */
@Composable
fun PinhoGardenVideoBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GardenLoopTextureView(context).apply {
                setPlaybackEnabled(enabled)
            }
        },
        update = { view -> view.setPlaybackEnabled(enabled) },
    )
}

private class GardenLoopTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var outputSurface: Surface? = null
    private var prepared = false
    private var playbackEnabled = true

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    fun setPlaybackEnabled(enabled: Boolean) {
        playbackEnabled = enabled
        val current = player ?: return
        if (!prepared) return
        runCatching {
            if (enabled && isAttachedToWindow && !current.isPlaying) current.start()
            if (!enabled && current.isPlaying) current.pause()
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        releasePlayer()
        val surface = Surface(surfaceTexture)
        outputSurface = surface
        val candidate = MediaPlayer()
        player = candidate
        candidate.isLooping = true
        candidate.setVolume(0f, 0f)
        candidate.setOnPreparedListener { mediaPlayer ->
            prepared = true
            mediaPlayer.isLooping = true
            mediaPlayer.setVolume(0f, 0f)
            if (playbackEnabled && isAttachedToWindow) {
                runCatching { mediaPlayer.start() }
            }
        }
        candidate.setOnErrorListener { _, _, _ ->
            releasePlayer()
            true
        }
        runCatching {
            candidate.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${R.raw.garden_loop}"),
            )
            candidate.setSurface(surface)
            candidate.prepareAsync()
        }.onFailure {
            releasePlayer()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onDetachedFromWindow() {
        releasePlayer()
        super.onDetachedFromWindow()
    }

    private fun releasePlayer() {
        prepared = false
        player?.let { activePlayer ->
            runCatching {
                activePlayer.setOnPreparedListener(null)
                activePlayer.setOnErrorListener(null)
                if (activePlayer.isPlaying) activePlayer.pause()
                activePlayer.stop()
                activePlayer.release()
            }
        }
        player = null
        outputSurface?.release()
        outputSurface = null
    }
}
