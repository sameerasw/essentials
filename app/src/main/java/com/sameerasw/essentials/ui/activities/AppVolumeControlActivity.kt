/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Activities
 * File: AppVolumeControlActivity.kt
 * Description: Bottom sheet with a volume slider for every app that is playing audio.
 */

package com.sameerasw.essentials.ui.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.media.AppVolumeController
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class AppVolumeControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EssentialsTheme {
                NowPlayingSheet(onDismiss = {
                    AppVolumeController.refresh()
                    finish()
                })
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

private const val ROW_ANIMATION_MS = 250L
private const val ENTER_MS = 350
private const val EXIT_MS = 180
private const val MIN_RESTORE_LEVEL = 0.05f
private val OUTER_CORNER = 24.dp
private val INNER_CORNER = 4.dp

private class PlayerRow(
    val packageName: String,
    val isInitial: Boolean = false,
) {
    var isPresent by mutableStateOf(true)
}

@Composable
private fun NowPlayingSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val playing by AppVolumeController.playingPackages.collectAsState()
    val levels = remember { mutableStateMapOf<String, Float>().apply { putAll(settings.getAppVolumeLevels()) } }
    val rows =
        remember {
            mutableStateListOf<PlayerRow>().apply {
                AppVolumeController.playingPackages.value.forEach { add(PlayerRow(it, isInitial = true)) }
            }
        }

    LaunchedEffect(playing) {
        rows.forEach { it.isPresent = it.packageName in playing }
        playing.forEach { packageName ->
            if (rows.none { it.packageName == packageName && it.isPresent }) rows.add(PlayerRow(packageName))
        }
        delay(ROW_ANIMATION_MS)
        rows.removeAll { !it.isPresent }
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessLow))
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            MediaVolumeRow()
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EqualizerBars(
                    playing = playing.isNotEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    size = 18.dp,
                )
                Text(
                    text = stringResource(R.string.app_volume_overlay_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            AnimatedVisibility(
                visibleState = remember { MutableTransitionState(rows.none { it.isPresent }) }.apply { targetState = rows.none { it.isPresent } },
                enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(tween(ENTER_MS)),
                exit = shrinkVertically(tween(EXIT_MS)) + fadeOut(tween(EXIT_MS)),
            ) {
                Text(
                    text = stringResource(R.string.app_volume_overlay_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val present = rows.filter { it.isPresent }
                rows.forEach { row ->
                    key(row) {
                        val index = present.indexOf(row).takeIf { it >= 0 } ?: present.size
                        PlayerVolumeRow(
                            row = row,
                            isFirst = index == 0,
                            isLast = index >= present.size - 1,
                            level = levels[row.packageName] ?: 1f,
                            onLevelChange = {
                                levels[row.packageName] = it
                                AppVolumeController.preview(row.packageName, it)
                            },
                            onLevelChangeFinished = {
                                settings.setAppVolumeLevels(
                                    settings.getAppVolumeLevels() + (row.packageName to (levels[row.packageName] ?: 1f)),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerVolumeRow(
    row: PlayerRow,
    isFirst: Boolean,
    isLast: Boolean,
    level: Float,
    onLevelChange: (Float) -> Unit,
    onLevelChangeFinished: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val icon =
        remember(row.packageName) {
            runCatching {
                AppUtil.drawableToBitmap(context.packageManager.getApplicationIcon(row.packageName)).asImageBitmap()
            }.getOrNull()
        }
    val visibleState = remember { MutableTransitionState(row.isInitial) }.apply { targetState = row.isPresent }
    val cornerSpec = spring<Dp>(stiffness = Spring.StiffnessMediumLow)
    val topCorner by animateDpAsState(if (isFirst) OUTER_CORNER else INNER_CORNER, cornerSpec, label = "topCorner")
    val bottomCorner by animateDpAsState(if (isLast) OUTER_CORNER else INNER_CORNER, cornerSpec, label = "bottomCorner")
    var sliderValue by remember { mutableFloatStateOf(level) }
    var restoreLevel by remember { mutableFloatStateOf(1f) }
    val iconAlpha by animateFloatAsState(if (sliderValue > 0f) 1f else 0.4f, label = "muteAlpha")

    AnimatedVisibility(
        visibleState = visibleState,
        enter = expandVertically(spring(stiffness = Spring.StiffnessLow)) + fadeIn(tween(ENTER_MS)),
        exit = shrinkVertically(tween(EXIT_MS)) + fadeOut(tween(EXIT_MS)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = topCorner, topEnd = topCorner, bottomStart = bottomCorner, bottomEnd = bottomCorner))
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icon?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                if (sliderValue > 0f) {
                                    restoreLevel = sliderValue
                                    sliderValue = 0f
                                } else {
                                    sliderValue = restoreLevel.coerceAtLeast(MIN_RESTORE_LEVEL)
                                }
                                onLevelChange(sliderValue)
                                onLevelChangeFinished()
                            }.graphicsLayer { alpha = iconAlpha },
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = {
                    if (it != sliderValue) HapticUtil.performSliderHaptic(view)
                    sliderValue = it
                    onLevelChange(it)
                },
                onValueChangeFinished = onLevelChangeFinished,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MediaVolumeRow() {
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var value by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var restoreVolume by remember { mutableFloatStateOf(1f) }

    DisposableEffect(Unit) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (!isDragging) value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OUTER_CORNER))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (value > 0f) {
                            restoreVolume = value
                            value = 0f
                        } else {
                            value = restoreVolume.coerceAtLeast(1f)
                        }
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value.roundToInt(), 0)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (value > 0f) R.drawable.rounded_volume_up_24 else R.drawable.rounded_volume_off_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = {
                isDragging = true
                if (it.roundToInt() != value.roundToInt()) HapticUtil.performSliderHaptic(view)
                value = it
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0)
            },
            onValueChangeFinished = { isDragging = false },
            valueRange = 0f..maxVolume,
            modifier = Modifier.weight(1f),
        )
    }
}
