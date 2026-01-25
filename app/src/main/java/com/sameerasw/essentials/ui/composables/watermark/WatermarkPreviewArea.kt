package com.sameerasw.essentials.ui.composables.watermark

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil.performSliderHaptic
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic
import com.sameerasw.essentials.viewmodels.WatermarkUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatermarkPreviewArea(
    initialUri: Uri?,
    previewState: WatermarkUiState,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val view = LocalView.current
    
    val maxPreviewHeightDp = screenHeightDp * 0.6f
    val minPreviewHeightDp = screenHeightDp * 0.3f
    
    val maxPx = with(density) { maxPreviewHeightDp.toPx() }
    val minPx = with(density) { minPreviewHeightDp.toPx() }
    
    val previewHeightPxState = remember { mutableFloatStateOf(maxPx) }
    var previewHeightPx by previewHeightPxState
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta < 0) {
                    val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                    val consumed = newHeight - previewHeightPx
                    if (kotlin.math.abs(consumed) > 0.5f) {
                        performSliderHaptic(view)
                    }
                    previewHeightPx = newHeight
                    return androidx.compose.ui.geometry.Offset(0f, consumed)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta > 0) {
                    val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                    val consumedY = newHeight - previewHeightPx
                    if (kotlin.math.abs(consumedY) > 0.5f) {
                        performSliderHaptic(view)
                    }
                    previewHeightPx = newHeight
                    return androidx.compose.ui.geometry.Offset(0f, consumedY)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { previewHeightPx.toDp() })
                .padding(16.dp)
                .clip(if (initialUri == null) RoundedCornerShape(24.dp) else RectangleShape)
                .background(if (initialUri == null) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                .clickable { 
                    performUIHaptic(view)
                    if (initialUri == null) {
                        onPickImage()
                    }
                }
                .padding(if (initialUri == null) 32.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            if (initialUri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     Icon(
                         painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
                         contentDescription = null, 
                         modifier = Modifier.size(64.dp),
                         tint = MaterialTheme.colorScheme.primary
                     )
                     Spacer(Modifier.size(8.dp))
                     Text(
                         stringResource(R.string.watermark_pick_image),
                         style = MaterialTheme.typography.bodyLarge,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                }
            } else {
                val current = previewState
                var lastSuccess by remember { mutableStateOf<WatermarkUiState.Success?>(null) }
                
                if (current is WatermarkUiState.Success) {
                    lastSuccess = current
                }
                
                val showBlur = current is WatermarkUiState.Processing
                
                val blurRadius by animateDpAsState(
                    targetValue = if (showBlur) 16.dp else 0.dp,
                    label = "blur"
                )
                
                val alpha by animateFloatAsState(
                    targetValue = if (showBlur) 0.6f else 1f,
                    label = "alpha"
                )
                
                Box(contentAlignment = Alignment.Center) {
                    if (lastSuccess != null) {
                        Box(
                            modifier = Modifier
                                .blur(blurRadius)
                                .alpha(alpha)
                        ) {
                            WatermarkPreview(uiState = lastSuccess!!)
                        }
                    } else if (current !is WatermarkUiState.Processing) {
                        WatermarkPreview(uiState = current)
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBlur,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LoadingIndicator()
                    }
                }
            }
        }
        
        content(PaddingValues(0.dp))
    }
}
