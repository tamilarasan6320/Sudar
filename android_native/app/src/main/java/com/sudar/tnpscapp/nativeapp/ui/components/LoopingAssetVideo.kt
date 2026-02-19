package com.sudar.tnpscapp.nativeapp.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * A composable that plays a video from assets in a continuous loop.
 * - Muted by default (volume = 0f)
 * - Loops indefinitely
 * - Lifecycle-aware (pauses on background, resumes on foreground)
 * - No player controls shown
 *
 * @param assetFileName The name of the video file in the assets folder (e.g., "login_header.mp4")
 * @param modifier Modifier for the video container
 * @param volume Volume level from 0f (muted) to 1f (full volume). Default is 0f (muted).
 */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun LoopingAssetVideo(
    assetFileName: String,
    modifier: Modifier = Modifier,
    volume: Float = 0f,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    useVideoAspectRatio: Boolean = false,
    defaultAspectRatio: Float = 16f / 9f,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Used only when caller wants layout height based on actual video size.
    val aspectRatio = remember(assetFileName) { mutableStateOf(defaultAspectRatio) }

    // Create and remember the ExoPlayer instance
    val exoPlayer = remember(assetFileName) {
        ExoPlayer.Builder(context).build().apply {
            // Build the asset URI
            val assetUri = Uri.parse("asset:///$assetFileName")
            val mediaItem = MediaItem.fromUri(assetUri)
            
            // Set audio attributes for media playback (needed for proper volume control)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, volume > 0f)
            
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE // Loop continuously
            this.volume = volume // Apply volume parameter
            playWhenReady = true
            prepare()
        }
    }

    // If enabled, compute aspect ratio from decoded video size and let Compose size the view accordingly.
    DisposableEffect(exoPlayer, useVideoAspectRatio) {
        if (!useVideoAspectRatio) {
            return@DisposableEffect onDispose { }
        }

        fun updateRatio(videoSize: VideoSize) {
            val w0 = videoSize.width
            val h0 = videoSize.height
            if (w0 <= 0 || h0 <= 0) return

            val rotation = videoSize.unappliedRotationDegrees
            val (w, h) = if (rotation == 90 || rotation == 270) (h0 to w0) else (w0 to h0)

            // Clamp to a "max portrait" of 9:16 so super-tall videos won't make the banner huge.
            val ratio = (w.toFloat() * videoSize.pixelWidthHeightRatio) / h.toFloat()
            if (ratio.isFinite() && ratio > 0.05f) {
                val minRatio = 9f / 16f // width/height
                aspectRatio.value = ratio.coerceAtLeast(minRatio)
            }
        }

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateRatio(videoSize)
            }
        }

        // Best-effort: apply immediately if already known.
        updateRatio(exoPlayer.videoSize)
        exoPlayer.addListener(listener)

        onDispose { exoPlayer.removeListener(listener) }
    }

    // Handle lifecycle events to pause/resume playback
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val layoutModifier = if (useVideoAspectRatio) modifier.aspectRatio(aspectRatio.value) else modifier

    // Display the PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide controls
                this.resizeMode = resizeMode
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = layoutModifier
    )
}

/**
 * A composable that plays a video from a file path or falls back to an asset.
 * - Muted by default (volume = 0f)
 * - Loops indefinitely
 * - Lifecycle-aware (pauses on background, resumes on foreground)
 * - No player controls shown
 *
 * @param filePath The local file path of the video (e.g., from internal storage). If null or file doesn't exist, falls back to asset.
 * @param assetFallback The name of the video file in the assets folder to use as fallback (e.g., "premium_video.mp4")
 * @param modifier Modifier for the video container
 * @param volume Volume level from 0f (muted) to 1f (full volume). Default is 0f (muted).
 */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun LoopingVideo(
    filePath: String?,
    assetFallback: String,
    modifier: Modifier = Modifier,
    volume: Float = 0f,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    useVideoAspectRatio: Boolean = false,
    defaultAspectRatio: Float = 16f / 9f,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Determine the URI to use
    val videoUri = remember(filePath, assetFallback) {
        if (!filePath.isNullOrBlank()) {
            val file = File(filePath)
            if (file.exists()) {
                Uri.fromFile(file)
            } else {
                Uri.parse("asset:///$assetFallback")
            }
        } else {
            Uri.parse("asset:///$assetFallback")
        }
    }

    // Used only when caller wants layout height based on actual video size.
    val aspectRatio = remember(videoUri) { mutableStateOf(defaultAspectRatio) }

    // Create and remember the ExoPlayer instance
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri)
            
            // Set audio attributes for media playback (needed for proper volume control)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, volume > 0f)
            
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE // Loop continuously
            this.volume = volume // Apply volume parameter
            playWhenReady = true
            prepare()
        }
    }

    // If enabled, compute aspect ratio from decoded video size and let Compose size the view accordingly.
    DisposableEffect(exoPlayer, useVideoAspectRatio) {
        if (!useVideoAspectRatio) {
            return@DisposableEffect onDispose { }
        }

        fun updateRatio(videoSize: VideoSize) {
            val w0 = videoSize.width
            val h0 = videoSize.height
            if (w0 <= 0 || h0 <= 0) return

            val rotation = videoSize.unappliedRotationDegrees
            val (w, h) = if (rotation == 90 || rotation == 270) (h0 to w0) else (w0 to h0)

            // Clamp to a "max portrait" of 9:16 so super-tall videos won't make the banner huge.
            val ratio = (w.toFloat() * videoSize.pixelWidthHeightRatio) / h.toFloat()
            if (ratio.isFinite() && ratio > 0.05f) {
                val minRatio = 9f / 16f // width/height
                aspectRatio.value = ratio.coerceAtLeast(minRatio)
            }
        }

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updateRatio(videoSize)
            }
        }

        updateRatio(exoPlayer.videoSize)
        exoPlayer.addListener(listener)

        onDispose { exoPlayer.removeListener(listener) }
    }

    // Handle lifecycle events to pause/resume playback
    DisposableEffect(lifecycleOwner, videoUri) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val layoutModifier = if (useVideoAspectRatio) modifier.aspectRatio(aspectRatio.value) else modifier

    // Display the PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Hide controls
                this.resizeMode = resizeMode
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = layoutModifier
    )
}
