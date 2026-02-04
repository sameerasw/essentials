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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.NotificationLightingColorModePicker
import com.sameerasw.essentials.ui.components.pickers.NotificationLightingStylePicker
import com.sameerasw.essentials.ui.components.pickers.GlowSidesPicker
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationLightingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    // App selection Logic
    var showAppSelectionSheet by remember { mutableStateOf(false) }

    // Corner radius state

    // Corner radius state
    var cornerRadiusDp by remember { mutableFloatStateOf(viewModel.loadNotificationLightingCornerRadius(context)) }
    var strokeThicknessDp by remember { mutableFloatStateOf(viewModel.loadNotificationLightingStrokeThickness(context)) }
    
    var indicatorX by remember { mutableFloatStateOf(viewModel.notificationLightingIndicatorX.value) }
    var indicatorY by remember { mutableFloatStateOf(viewModel.notificationLightingIndicatorY.value) }
    var indicatorScale by remember { mutableFloatStateOf(viewModel.notificationLightingIndicatorScale.value) }
    
    val coroutineScope = rememberCoroutineScope()

    // Cleanup overlay when composable is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.removePreviewOverlay(context)
        }
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {


        RoundedCardContainer{
            IconToggleItem(
                iconRes = R.drawable.rounded_power_settings_new_24,
                title = stringResource(R.string.notification_lighting_screen_off_title),
                isChecked = viewModel.onlyShowWhenScreenOff.value,
                onCheckedChange = { checked ->
                    viewModel.setOnlyShowWhenScreenOff(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "only_screen_off")
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_off_24,
                title = stringResource(R.string.notification_lighting_skip_silent_title),
                isChecked = viewModel.skipSilentNotifications.value,
                onCheckedChange = { checked ->
                    viewModel.setSkipSilentNotifications(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "skip_silent_notifications")
            )
            IconToggleItem(
                iconRes = R.drawable.outline_circle_notifications_24,
                title = stringResource(R.string.notification_lighting_skip_persistent_title),
                isChecked = viewModel.skipPersistentNotifications.value,
                onCheckedChange = { checked ->
                    viewModel.setSkipPersistentNotifications(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "skip_persistent_notifications")
            )
        }


        // Style Picker
        Text(
            text = stringResource(R.string.settings_section_style),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        NotificationLightingStylePicker(
            selectedStyle = viewModel.notificationLightingStyle.value,
            onStyleSelected = { style ->
                viewModel.setNotificationLightingStyle(style, context)
                viewModel.triggerNotificationLighting(context)
            },
        )

        // Stroke Adjustment Section (For STROKE style)
        val style = viewModel.notificationLightingStyle.value
        if (style == NotificationLightingStyle.STROKE) {
            Text(
                text = stringResource(R.string.notification_lighting_stroke_adjustment_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_corner_radius_title),
                    value = cornerRadiusDp,
                    onValueChange = { newValue ->
                        cornerRadiusDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        // Show preview overlay while dragging
                        viewModel.triggerNotificationLightingWithRadiusAndThickness(context, newValue, strokeThicknessDp)
                    },
                    valueRange = 0f..50f,
                    valueFormatter = { "%.1f".format(it) },
                    onValueChangeFinished = {
                        // Save the corner radius
                        viewModel.saveNotificationLightingCornerRadius(context, cornerRadiusDp)
                        // Wait 5 seconds then remove preview overlay
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "corner_radius")
                )
                
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_stroke_thickness_title),
                    value = strokeThicknessDp,
                    onValueChange = { newValue ->
                        strokeThicknessDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        // Show preview overlay while dragging
                        viewModel.triggerNotificationLightingWithRadiusAndThickness(context, cornerRadiusDp, newValue)
                    },
                    modifier = Modifier.highlight(highlightSetting == "stroke_thickness"),
                    valueRange = 1f..20f,
                    valueFormatter = { "%.1f".format(it) },
                    onValueChangeFinished = {
                        // Save the stroke thickness
                        viewModel.saveNotificationLightingStrokeThickness(context, strokeThicknessDp)
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
        if (style == NotificationLightingStyle.GLOW) {
            Text(
                text = stringResource(R.string.notification_lighting_glow_adjustment_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier) {
                GlowSidesPicker(
                    selectedSides = viewModel.notificationLightingGlowSides.value,
                    onSideToggled = { side, isChecked ->
                        val current = viewModel.notificationLightingGlowSides.value.toMutableSet()
                        if (isChecked) current.add(side) else current.remove(side)
                        viewModel.setNotificationLightingGlowSides(current, context)
                        viewModel.triggerNotificationLighting(context)
                    }
                )

                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_glow_spread_title),
                    value = strokeThicknessDp,
                    onValueChange = { newValue ->
                        strokeThicknessDp = newValue
                        HapticUtil.performSliderHaptic(view)
                        viewModel.triggerNotificationLightingWithRadiusAndThickness(context, cornerRadiusDp, newValue)
                    },
                    valueRange = 1f..10f,
                    valueFormatter = { "%.1f".format(it) },
                    onValueChangeFinished = {
                        viewModel.saveNotificationLightingStrokeThickness(context, strokeThicknessDp)
                        coroutineScope.launch {
                            delay(2000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
            }
        }
        
        // Indicator Adjustment Section (For INDICATOR style)
        if (style == NotificationLightingStyle.INDICATOR) {
            Text(
                text = stringResource(R.string.notification_lighting_placement_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_h_pos_title),
                    value = indicatorX,
                    onValueChange = { newValue ->
                        indicatorX = newValue
                        HapticUtil.performSliderHaptic(view)
                        viewModel.triggerNotificationLightingForIndicator(context, newValue, indicatorY, indicatorScale)
                    },
                    valueRange = 0f..100f,
                    valueFormatter = { "%.1f%%".format(it) },
                    onValueChangeFinished = {
                        viewModel.saveNotificationLightingIndicatorX(context, indicatorX)
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
                
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_v_pos_title),
                    value = indicatorY,
                    onValueChange = { newValue ->
                        indicatorY = newValue
                        HapticUtil.performSliderHaptic(view)
                        viewModel.triggerNotificationLightingForIndicator(context, indicatorX, newValue, indicatorScale)
                    },
                    valueRange = 0f..100f,
                    valueFormatter = { "%.1f%%".format(it) },
                    onValueChangeFinished = {
                        viewModel.saveNotificationLightingIndicatorY(context, indicatorY)
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )
            }

            Text(
                text = stringResource(R.string.notification_lighting_indicator_adjustment_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_scale_title),
                    value = indicatorScale,
                    onValueChange = { newValue ->
                        indicatorScale = newValue
                        HapticUtil.performSliderHaptic(view)
                        viewModel.triggerNotificationLightingForIndicator(context, indicatorX, indicatorY, newValue)
                    },
                    valueRange = 0.5f..3f,
                    valueFormatter = { "%.1fx".format(it) },
                    onValueChangeFinished = {
                        viewModel.saveNotificationLightingIndicatorScale(context, indicatorScale)
                        coroutineScope.launch {
                            delay(5000)
                            viewModel.removePreviewOverlay(context)
                        }
                    }
                )

                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_duration_title),
                    value = viewModel.notificationLightingPulseDuration.value,
                    onValueChange = { 
                        viewModel.saveNotificationLightingPulseDuration(context, it)
                        HapticUtil.performSliderHaptic(view)
                    },
                    valueRange = 1000f..10000f,
                    increment = 100f,
                    valueFormatter = { "%.1fs".format(it / 1000f) },
                    onValueChangeFinished = { viewModel.triggerNotificationLighting(context) }
                )
            }
        }


        // Animation Settings (Only for STROKE and GLOW)
        if (style == NotificationLightingStyle.STROKE || style == NotificationLightingStyle.GLOW) {
            Text(
                text = stringResource(R.string.settings_section_animation),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            
            RoundedCardContainer(modifier = Modifier) {
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_pulse_count_title),
                    value = viewModel.notificationLightingPulseCount.value,
                    onValueChange = { 
                        viewModel.saveNotificationLightingPulseCount(context, it)
                        HapticUtil.performSliderHaptic(view)
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    increment = 1f,
                    valueFormatter = { "%.1f".format(it) },
                    onValueChangeFinished = { viewModel.triggerNotificationLighting(context) }
                )
                    
                ConfigSliderItem(
                    title = stringResource(R.string.notification_lighting_pulse_duration_title),
                    value = viewModel.notificationLightingPulseDuration.value,
                    onValueChange = { 
                        viewModel.saveNotificationLightingPulseDuration(context, it)
                        HapticUtil.performSliderHaptic(view)
                    },
                    valueRange = 100f..10000f,
                    increment = 100f,
                    valueFormatter = { "%.1fs".format(it / 1000f) },
                    onValueChangeFinished = { viewModel.triggerNotificationLighting(context) }
                )
            }
        }


        // Color Mode section
        Text(
            text = stringResource(R.string.settings_section_color_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier
        ) {
            NotificationLightingColorModePicker(
                selectedMode = viewModel.notificationLightingColorMode.value,
                onModeSelected = { mode ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setNotificationLightingColorMode(mode, context)
                }
            )

            if (viewModel.notificationLightingColorMode.value == NotificationLightingColorMode.CUSTOM) {
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
                    val currentCustomColor = viewModel.notificationLightingCustomColor.intValue

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
                                                viewModel.setNotificationLightingCustomColor(colorInt, context)
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
            Text(stringResource(R.string.action_select_apps))
        }

        Text(
            text = stringResource(R.string.settings_section_ambient_display),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(R.string.ambient_display_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer{
            IconToggleItem(
                iconRes = R.drawable.rounded_nightlight_24,
                title = stringResource(R.string.ambient_display_title),
                description = stringResource(R.string.ambient_display_desc),
                isChecked = viewModel.isAmbientDisplayEnabled.value,
                onCheckedChange = { checked ->
                    viewModel.setAmbientDisplayEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "ambient_display")
            )
            if (viewModel.isAmbientDisplayEnabled.value) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                    title = stringResource(R.string.ambient_show_lock_screen_title),
                    description = stringResource(R.string.ambient_show_lock_screen_desc),
                    isChecked = viewModel.isAmbientShowLockScreenEnabled.value,
                    onCheckedChange = { checked ->
                        viewModel.setAmbientShowLockScreenEnabled(checked, context)
                    },
                    modifier = Modifier.highlight(highlightSetting == "ambient_show_lock_screen")
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadNotificationLightingSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveNotificationLightingSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateNotificationLightingAppEnabled(ctx, pkg, enabled) },
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
