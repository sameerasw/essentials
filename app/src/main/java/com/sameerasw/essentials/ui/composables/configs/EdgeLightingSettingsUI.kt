package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.EdgeLightingColorMode
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.pickers.EdgeLightingColorModePicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.pickers.EdgeLightingStylePicker
import com.sameerasw.essentials.domain.model.EdgeLightingStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EdgeLightingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // App selection Logic
    var showAppSelectionSheet by remember { mutableStateOf(false) }

    // Corner radius state

    // Corner radius state
    var cornerRadiusDp by remember { mutableStateOf(viewModel.loadEdgeLightingCornerRadius(context).toFloat()) }
    var strokeThicknessDp by remember { mutableStateOf(viewModel.loadEdgeLightingStrokeThickness(context).toFloat()) }
    val coroutineScope = rememberCoroutineScope()

    // Cleanup overlay when composable is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.removePreviewOverlay(context)
        }
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {


        RoundedCardContainer(
            modifier = Modifier.padding(top = 8.dp),
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_power_settings_new_24,
                title = "Only show when screen off",
                isChecked = viewModel.onlyShowWhenScreenOff.value,
                onCheckedChange = { checked ->
                    viewModel.setOnlyShowWhenScreenOff(checked, context)
                }
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_off_24,
                title = "Skip silent notifications",
                isChecked = viewModel.skipSilentNotifications.value,
                onCheckedChange = { checked ->
                    viewModel.setSkipSilentNotifications(checked, context)
                }
            )
        }

        // Style Picker
        Text(
            text = "Style",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        EdgeLightingStylePicker(
            selectedStyle = viewModel.edgeLightingStyle.value,
            onStyleSelected = { style ->
                viewModel.setEdgeLightingStyle(style, context)
                viewModel.triggerEdgeLighting(context)
            },
        )

        // Stroke Adjustment Section (For STROKE style)
        val style = viewModel.edgeLightingStyle.value
        if (style == EdgeLightingStyle.STROKE) {
            Text(
                text = "Stroke adjustment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = "Corner radius",
                    value = cornerRadiusDp,
                    onValueChange = { newValue ->
                        cornerRadiusDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        // Show preview overlay while dragging
                        viewModel.triggerEdgeLightingWithRadiusAndThickness(context, newValue.toInt(), strokeThicknessDp.toInt())
                    },
                    valueRange = 0f..50f,
                    onValueChangeFinished = {
                        // Save the corner radius
                        viewModel.saveEdgeLightingCornerRadius(context, cornerRadiusDp.toInt())
                        // Wait 5 seconds then remove preview overlay
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
                
                ConfigSliderItem(
                    title = "Stroke thickness",
                    value = strokeThicknessDp,
                    onValueChange = { newValue ->
                        strokeThicknessDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        // Show preview overlay while dragging
                        viewModel.triggerEdgeLightingWithRadiusAndThickness(context, cornerRadiusDp.toInt(), newValue.toInt())
                    },
                    valueRange = 1f..20f,
                    onValueChangeFinished = {
                        // Save the stroke thickness
                        viewModel.saveEdgeLightingStrokeThickness(context, strokeThicknessDp.toInt())
                        // Wait 5 seconds then remove preview overlay
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
            }
        }

        // Glow Adjustment Section (For GLOW style)
        if (style == EdgeLightingStyle.GLOW) {
            Text(
                text = "Glow adjustment",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = "Glow spread",
                    value = strokeThicknessDp,
                    onValueChange = { newValue ->
                        strokeThicknessDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        viewModel.triggerEdgeLightingWithRadiusAndThickness(context, cornerRadiusDp.toInt(), newValue.toInt())
                    },
                    valueRange = 1f..10f,
                    onValueChangeFinished = {
                        viewModel.saveEdgeLightingStrokeThickness(context, strokeThicknessDp.toInt())
                        coroutineScope.launch {
                            delay(2000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
            }
        }


        // Animation Settings
        Text(
            text = "Animation",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        
        RoundedCardContainer(modifier = Modifier) {
            ConfigSliderItem(
                title = "Pulse count",
                value = viewModel.edgeLightingPulseCount.intValue.toFloat(),
                onValueChange = { 
                    viewModel.saveEdgeLightingPulseCount(context, it.toInt())
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 1f..5f,
                steps = 3,
                valueFormatter = { "%.0f".format(it) },
                onValueChangeFinished = { viewModel.triggerEdgeLighting(context) }
            )
                
            ConfigSliderItem(
                title = "Pulse duration",
                value = viewModel.edgeLightingPulseDuration.value,
                onValueChange = { 
                    viewModel.saveEdgeLightingPulseDuration(context, it)
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 100f..10000f,
                valueFormatter = { "%.1fs".format(it / 1000f) },
                onValueChangeFinished = { viewModel.triggerEdgeLighting(context) }
            )
        }


        // Color Mode section
        Text(
            text = "Color Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier
        ) {
            EdgeLightingColorModePicker(
                selectedMode = viewModel.edgeLightingColorMode.value,
                onModeSelected = { mode ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setEdgeLightingColorMode(mode, context)
                }
            )

            if (viewModel.edgeLightingColorMode.value == EdgeLightingColorMode.CUSTOM) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                        )
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allColors = remember {
                        val colors = mutableListOf<Int>()
                        val totalColumns = 21

                        for (page in 0..2) {

                            val row1 = mutableListOf<Int>()
                            val row2 = mutableListOf<Int>()
                            val row3 = mutableListOf<Int>()

                            for (col in 0..6) {
                                val globalCol = page * 7 + col
                                val hue = (globalCol.toFloat() / totalColumns) * 360f

                                // Row 1: Light
                                row1.add(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.4f, 1.0f)))
                                // Row 2: Regular
                                row2.add(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 1.0f)))
                                // Row 3: Dark
                                row3.add(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1.0f, 0.55f)))
                            }
                            colors.addAll(row1)
                            colors.addAll(row2)
                            colors.addAll(row3)
                        }
                        colors
                    }

                    val pages = allColors.chunked(21)
                    val pagerState = rememberPagerState(pageCount = { pages.size })
                    val currentCustomColor = viewModel.edgeLightingCustomColor.intValue

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) { pageIndex ->
                        val pageColors = pages[pageIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val rows = pageColors.chunked(7)
                            rows.forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowColors.forEach { colorInt ->
                                        ColorCircle(
                                            color = Color(colorInt),
                                            isSelected = currentCustomColor == colorInt,
                                            size = 36.dp,
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                viewModel.setEdgeLightingCustomColor(colorInt, context)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pager Indicator
                    Row(
                        Modifier
                            .height(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pages.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Selection Sheet Button
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                showAppSelectionSheet = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select apps")
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadEdgeLightingSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveEdgeLightingSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateEdgeLightingAppEnabled(ctx, pkg, enabled) },
                context = context
            )
        }
    }
}

@Composable
fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(size * 0.4f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}
