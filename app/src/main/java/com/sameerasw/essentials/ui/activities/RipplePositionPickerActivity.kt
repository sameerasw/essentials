/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: RipplePositionPickerActivity.kt
 * Description: Activity component for RipplePositionPickerActivity.kt dialog overlay.
 */

package com.sameerasw.essentials.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

class RipplePositionPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel()
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                RipplePositionPickerScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RipplePositionPickerScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    val posX = viewModel.ripple.positionX.floatValue
    val posY = viewModel.ripple.positionY.floatValue

    fun commitAndPreview(
        newX: Float,
        newY: Float,
        preview: Boolean,
    ) {
        viewModel.ripple.savePositionXY(
            newX.coerceIn(0f, 100f),
            newY.coerceIn(0f, 100f),
        )
        if (preview) viewModel.triggerNotificationLightingForRipple(context)
    }

    fun updateFromOffset(
        offset: Offset,
        preview: Boolean,
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        commitAndPreview(
            offset.x / canvasSize.width * 100f,
            offset.y / canvasSize.height * 100f,
            preview,
        )
    }

    val markerX by animateFloatAsState(targetValue = posX, label = "RippleMarkerX")
    val markerY by animateFloatAsState(targetValue = posY, label = "RippleMarkerY")

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        updateFromOffset(offset, preview = true)
                    }
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> updateFromOffset(offset, preview = false) },
                        onDragEnd = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.triggerNotificationLightingForRipple(context)
                        },
                    ) { change, _ ->
                        updateFromOffset(change.position, preview = false)
                    }
                },
    ) {
        Card(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_target_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )

                Text(
                    text = stringResource(R.string.notification_lighting_ripple_position_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                Text(
                    text = stringResource(R.string.notification_lighting_ripple_position_picker_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "%.0f%% · %.0f%%".format(posX, posY),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            commitAndPreview(50f, 50f, preview = true)
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.notification_lighting_sweep_pos_center))
                    }

                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.removePreviewOverlay(context)
                            onDismiss()
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }
        }

        if (canvasSize != IntSize.Zero) {
            val markerSize = 56.dp
            val markerHalfPx = with(density) { (markerSize / 2).toPx() }

            Box(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                (canvasSize.width * markerX / 100f - markerHalfPx).toInt(),
                                (canvasSize.height * markerY / 100f - markerHalfPx).toInt(),
                            )
                        }.size(markerSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
