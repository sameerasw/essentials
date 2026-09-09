/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: FeatureHelpMediaViewer.kt
 * Description: Seamlessly shared help media player supporting in-sheet preview and edge-to-edge immersive full-screen pinch-to-zoom.
 */

package com.sameerasw.essentials.ui.core.media

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.sameerasw.essentials.data.repository.FeatureHelpMedia
import com.sameerasw.essentials.data.repository.HelpMediaRepository
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.abs

@OptIn(UnstableApi::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FeatureHelpMediaViewer(
    feature: Feature,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    if (!viewModel.isOnlineHelpMediaEnabled.value) {
        return
    }

    val context = LocalContext.current
    val helpMediaRepo = remember { HelpMediaRepository.getInstance(context) }
    var mediaItem by remember(feature.id) { mutableStateOf<FeatureHelpMedia?>(null) }
    var hasCheckedMedia by remember(feature.id) { mutableStateOf(false) }
    var isFullScreenOpen by remember { mutableStateOf(false) }

    LaunchedEffect(feature.id) {
        hasCheckedMedia = false
        mediaItem = helpMediaRepo.getHelpMediaForFeature(feature.id)
        hasCheckedMedia = true
    }

    val exoPlayer =
        remember(mediaItem?.url) {
            val url = mediaItem?.url
            if (url != null && mediaItem?.type.equals("video", ignoreCase = true)) {
                ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = 0f
                    setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                    prepare()
                    playWhenReady = true
                }
            } else {
                null
            }
        }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    var isMediaReady by remember(feature.id) { mutableStateOf(false) }
    var isPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer?.isPlaying == true) }

    LaunchedEffect(exoPlayer) {
        exoPlayer?.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isMediaReady = true
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            },
        )
    }

    val playerView =
        remember(exoPlayer) {
            if (exoPlayer != null) {
                PlayerView(context).apply {
                    useController = false
                    player = exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            } else {
                null
            }
        }

    AnimatedVisibility(
        visible = (mediaItem != null && (mediaItem?.type.equals("video", ignoreCase = true) == false || isMediaReady)),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (mediaItem != null) {
            val currentMedia = mediaItem!!
            val localView = LocalView.current
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 12f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            HapticUtil.performUIHaptic(localView)
                            isFullScreenOpen = true
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (currentMedia.type.equals("video", ignoreCase = true)) {
                    if (playerView != null && !isFullScreenOpen) {
                        AndroidView(
                            factory = {
                                (playerView.parent as? ViewGroup)?.removeView(playerView)
                                playerView
                            },
                            update = { view ->
                                (view.parent as? ViewGroup)?.let { parent ->
                                    // already attached
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    SubcomposeAsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data(currentMedia.url)
                                .crossfade(true)
                                .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            if (isFullScreenOpen) {
                FullScreenMediaDialog(
                    media = currentMedia,
                    playerView = playerView,
                    exoPlayer = exoPlayer,
                    isPlaying = isPlaying,
                    onDismissRequest = { isFullScreenOpen = false },
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullScreenMediaDialog(
    media: FeatureHelpMedia,
    playerView: PlayerView?,
    exoPlayer: ExoPlayer?,
    isPlaying: Boolean,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val bgAlphaAnim = remember { Animatable(1f) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                window.setBackgroundDrawableResource(android.R.color.transparent)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.attributes =
                        window.attributes.apply {
                            layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                }

                window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                )

                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgAlphaAnim.value.coerceIn(0f, 1f))),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                            translationX = offsetAnim.value.x
                            translationY = offsetAnim.value.y
                            clip = false
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isMultiTouch = false
                                var totalDragY = 0f
                                var totalDragX = 0f
                                var hasMovedSignificantly = false

                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }

                                    if (pressedCount >= 2) {
                                        isMultiTouch = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val centroid = event.calculateCentroid(useCurrent = true)

                                        val oldScale = scaleAnim.value
                                        val newScale = (oldScale * zoomChange).coerceIn(1f, 6f)

                                        val centerOffset =
                                            centroid - Offset(size.width / 2f, size.height / 2f)
                                        val newOffset =
                                            (offsetAnim.value + panChange) +
                                                centerOffset * (1f - newScale / oldScale)

                                        coroutineScope.launch {
                                            scaleAnim.snapTo(newScale)
                                            if (newScale > 1.01f) {
                                                offsetAnim.snapTo(newOffset)
                                                bgAlphaAnim.snapTo(1f)
                                            } else {
                                                offsetAnim.snapTo(Offset.Zero)
                                            }
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (isMultiTouch || scaleAnim.value > 1.05f) {
                                        // Panning when zoomed in
                                        val panChange = event.calculatePan()
                                        if (panChange != Offset.Zero) {
                                            coroutineScope.launch {
                                                offsetAnim.snapTo(offsetAnim.value + panChange)
                                            }
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        // Single touch swipe-to-dismiss gesture when scale is 1f
                                        val panChange = event.calculatePan()
                                        totalDragY += panChange.y
                                        totalDragX += panChange.x

                                        if (abs(totalDragY) > 8f || abs(totalDragX) > 8f) {
                                            hasMovedSignificantly = true
                                        }

                                        if (hasMovedSignificantly) {
                                            val currentOffsetY = offsetAnim.value.y + panChange.y
                                            val currentOffsetX = offsetAnim.value.x + (panChange.x * 0.4f)
                                            val progress = (abs(currentOffsetY) / (size.height * 0.45f)).coerceIn(0f, 1f)
                                            val targetAlpha = 1f - (progress * 0.85f)

                                            coroutineScope.launch {
                                                offsetAnim.snapTo(Offset(currentOffsetX, currentOffsetY))
                                                bgAlphaAnim.snapTo(targetAlpha)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (scaleAnim.value > 1.01f) {
                                    // Animate zoom back
                                    coroutineScope.launch {
                                        launch {
                                            scaleAnim.animateTo(
                                                targetValue = 1f,
                                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                            )
                                        }
                                        launch {
                                            offsetAnim.animateTo(
                                                targetValue = Offset.Zero,
                                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                            )
                                        }
                                    }
                                } else if (hasMovedSignificantly) {
                                    val dismissThreshold = size.height * 0.15f
                                    if (abs(offsetAnim.value.y) > dismissThreshold) {
                                        // Instant dismiss directly so view seamlessly returns to sheet
                                        onDismissRequest()
                                    } else {
                                        // Spring bounce back to center
                                        coroutineScope.launch {
                                            launch {
                                                bgAlphaAnim.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                                )
                                            }
                                            launch {
                                                offsetAnim.animateTo(
                                                    targetValue = Offset.Zero,
                                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Single tap -> Toggle Play/Pause for video
                                    if (exoPlayer != null && media.type.equals("video", ignoreCase = true)) {
                                        HapticUtil.performUIHaptic(dialogView)
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            exoPlayer.play()
                                        }
                                    }
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (media.type.equals("video", ignoreCase = true)) {
                    if (playerView != null) {
                        AndroidView(
                            factory = {
                                (playerView.parent as? ViewGroup)?.removeView(playerView)
                                playerView
                            },
                            update = { view ->
                                (view.parent as? ViewGroup)?.let { parent ->
                                    // attached in dialog
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    SubcomposeAsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data(media.url)
                                .crossfade(false)
                                .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            AnimatedVisibility(
                visible = !isPlaying && media.type.equals("video", ignoreCase = true),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_pause_24),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
