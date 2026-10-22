package com.pinhoquest.ui.music

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.math.abs

private val PINHOQUEST_SOUNDTRACK = listOf(
    "jardim.mp3",
    "exploracao.mp3",
    "segredo_folhas.mp3",
    "jardim_descansa.mp3",
)

private const val TRACK_TRANSITION_FADE_MS = 1_500L
private const val PLAYBACK_RESUME_FADE_MS = 1_800L
private const val PLAYBACK_PAUSE_FADE_MS = 650L
private const val MAX_MUSIC_VOLUME = 0.35f

/**
 * App-wide soundtrack host. Keep this composable mounted above navigation so
 * music survives screen changes. Audio assets are packaged at app/src/main/assets/music.
 */
@Composable
internal fun PinhoQuestMusicHost(
    enabled: Boolean,
    volume: Float,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val foreground = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val shouldPlay by rememberUpdatedState(enabled && foreground)
    val targetVolume by rememberUpdatedState(volume.coerceIn(0f, MAX_MUSIC_VOLUME))
    val assetsReady = remember(context) { hasCompleteSoundtrack(context) }

    val player = remember(context, assetsReady) {
        if (!assetsReady) {
            null
        } else {
            runCatching {
                ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_GAME)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true,
                    )
                    setHandleAudioBecomingNoisy(true)
                    setMediaItems(
                        PINHOQUEST_SOUNDTRACK.map { name ->
                            MediaItem.fromUri(Uri.parse("asset:///music/$name"))
                        },
                    )
                    repeatMode = Player.REPEAT_MODE_ALL
                    setShuffleModeEnabled(false)
                    setVolume(0f)
                    prepare()
                }
            }.getOrNull()
        }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    // Observe the latest switches/volume without recreating the player or
    // restarting the coroutine whenever a slider value changes.
    LaunchedEffect(player) {
        if (player == null) return@LaunchedEffect

        var wasPlaying = false
        var lastTrackIndex = player.currentMediaItemIndex
        var fadingOutForTrackChange = false

        while (true) {
            if (!shouldPlay) {
                if (wasPlaying) {
                    fadePlayerVolume(player, 0f, PLAYBACK_PAUSE_FADE_MS)
                    player.pause()
                    wasPlaying = false
                    fadingOutForTrackChange = false
                }
                delay(100L)
                continue
            }

            if (!wasPlaying) {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
                wasPlaying = true
                fadePlayerVolume(
                    player,
                    targetVolume,
                    PLAYBACK_RESUME_FADE_MS,
                    continueWhile = { shouldPlay },
                )
                lastTrackIndex = player.currentMediaItemIndex
                fadingOutForTrackChange = false
                continue
            }

            val currentTrackIndex = player.currentMediaItemIndex
            if (currentTrackIndex != C.INDEX_UNSET && currentTrackIndex != lastTrackIndex) {
                // The playlist has advanced. Bring in the next piece gently.
                lastTrackIndex = currentTrackIndex
                fadingOutForTrackChange = false
                player.setVolume(0f)
                fadePlayerVolume(
                    player,
                    targetVolume,
                    TRACK_TRANSITION_FADE_MS,
                    continueWhile = { shouldPlay },
                )
                continue
            }

            val duration = player.duration
            if (duration != C.TIME_UNSET && duration > 0L) {
                val remainingMs = duration - player.currentPosition
                if (
                    remainingMs in 1..TRACK_TRANSITION_FADE_MS &&
                    !fadingOutForTrackChange
                ) {
                    fadingOutForTrackChange = true
                    fadePlayerVolume(
                        player,
                        0f,
                        remainingMs.coerceAtLeast(120L),
                        continueWhile = { shouldPlay },
                    )
                    continue
                }
            }

            // Keep slider changes responsive during playback without repeatedly
            // writing/recreating media items.
            if (!fadingOutForTrackChange && abs(player.volume - targetVolume) >= 0.025f) {
                player.setVolume(targetVolume)
            }
            delay(80L)
        }
    }
}

private fun hasCompleteSoundtrack(context: Context): Boolean =
    PINHOQUEST_SOUNDTRACK.all { filename ->
        runCatching { context.assets.open("music/$filename").use { } }.isSuccess
    }

private suspend fun fadePlayerVolume(
    player: ExoPlayer,
    targetVolume: Float,
    durationMs: Long,
    continueWhile: () -> Boolean = { true },
) {
    val startVolume = player.volume
    val steps = 24
    for (step in 1..steps) {
        if (!continueWhile()) return
        val fraction = step.toFloat() / steps
        player.setVolume(startVolume + (targetVolume - startVolume) * fraction)
        delay((durationMs / steps).coerceAtLeast(1L))
    }
    if (continueWhile()) player.setVolume(targetVolume)
}
