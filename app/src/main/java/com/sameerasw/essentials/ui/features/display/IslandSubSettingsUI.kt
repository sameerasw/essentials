package com.sameerasw.essentials.ui.features.display

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.consciousgate.CONSCIOUS_GATE_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.actions.GestureActionPickerSheet
import com.sameerasw.essentials.ui.features.display.actions.HorizontalSlideModeSheet
import com.sameerasw.essentials.ui.features.display.actions.horizontalSlideDescription
import com.sameerasw.essentials.ui.features.display.sheets.IslandAlarmOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBorderOutlineOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBriefOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandCallOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandDevicesBatteryBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandPulseShadowOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeBatteryOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimerOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandWeatherOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

const val ISLAND_PLACEMENT_FEATURE_ID = "Island placement"
const val ISLAND_VISUALS_FEATURE_ID = "Island visuals"
const val ISLAND_DURATION_FEATURE_ID = "Island duration"
const val ISLAND_BEHAVIOR_FEATURE_ID = "Island behavior"

val ISLAND_SUB_PAGE_IDS = setOf(ISLAND_PLACEMENT_FEATURE_ID, ISLAND_VISUALS_FEATURE_ID, ISLAND_DURATION_FEATURE_ID, ISLAND_BEHAVIOR_FEATURE_ID)

internal val ISLAND_SUB_PAGES = listOf(
    Triple(ISLAND_PLACEMENT_FEATURE_ID, R.string.island_section_placement, R.drawable.rounded_center_focus_strong_24),
    Triple(ISLAND_VISUALS_FEATURE_ID, R.string.island_section_visuals, R.drawable.rounded_palette_24),
    Triple(ISLAND_DURATION_FEATURE_ID, R.string.island_section_duration, R.drawable.rounded_timer_24),
    Triple(ISLAND_BEHAVIOR_FEATURE_ID, R.string.island_section_behavior, R.drawable.rounded_settings_motion_mode_24),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandPlacementSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IslandPreviewControls()

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp)
                    .highlight(highlightSetting == "island_camera_position"),
            ) {
                Text(
                    text = stringResource(R.string.island_camera_position_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val positions = listOf(
                    SettingsRepository.ISLAND_CAMERA_POSITION_LEFT,
                    SettingsRepository.ISLAND_CAMERA_POSITION_CENTER,
                    SettingsRepository.ISLAND_CAMERA_POSITION_RIGHT,
                )
                val positionLabels = mapOf(
                    SettingsRepository.ISLAND_CAMERA_POSITION_LEFT to stringResource(R.string.island_camera_position_left),
                    SettingsRepository.ISLAND_CAMERA_POSITION_CENTER to stringResource(R.string.island_camera_position_center),
                    SettingsRepository.ISLAND_CAMERA_POSITION_RIGHT to stringResource(R.string.island_camera_position_right),
                )
                SegmentedPicker(
                    items = positions,
                    selectedItem = viewModel.islandCameraPosition.value,
                    onItemSelected = { viewModel.setIslandCameraPosition(it) },
                    labelProvider = { positionLabels[it].orEmpty() },
                    title = R.string.island_camera_position_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LaunchedEffect(Unit) {
                if (viewModel.isIslandAutoDetect.value) viewModel.autoAlignIslandWithCamera(context)
            }

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.autoAlignIslandWithCamera(context)
                    Toast.makeText(context, R.string.island_auto_align_toast, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .highlight(highlightSetting == "island_use_auto_detect"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_center_focus_strong_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = stringResource(R.string.island_auto_align_camera))
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_camera_offset_x_title),
                    value = viewModel.islandCameraOffsetX.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandCameraOffsetX(it)
                    },
                    valueRange = 0f..100f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_border_left_24,
                    valueFormatter = { "${it.toInt()}%" },
                )
                ConfigSliderItem(
                    title = stringResource(R.string.island_camera_offset_y_title),
                    value = viewModel.islandCameraOffsetY.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandCameraOffsetY(it)
                    },
                    valueRange = 0f..20f,
                    increment = 0.5f,
                    iconRes = R.drawable.rounded_border_top_24,
                    valueFormatter = { "%.1f%%".format(it) },
                )
            }

            ConfigSliderItem(
                title = stringResource(R.string.island_camera_size_title),
                value = viewModel.islandCameraSize.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandCameraSize(it)
                },
                valueRange = 0.05f..2.0f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "%.2fx".format(it) },
                modifier = Modifier.highlight(highlightSetting == "island_camera_size"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_max_width_title),
                value = viewModel.islandMaxWidth.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandMaxWidth(it)
                },
                valueRange = 150f..500f,
                increment = 10f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_max_width"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_width_title),
                value = viewModel.islandExpandedWidth.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedWidth(it)
                },
                valueRange = 200f..500f,
                increment = 10f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_width"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_cutout_gap_title),
                value = viewModel.islandCutoutGap.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandCutoutGap(it)
                },
                valueRange = 0f..16f,
                increment = 1f,
                iconRes = R.drawable.rounded_arrows_outward_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_cutout_gap"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandVisualsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showBorderOutlineSheet by remember { mutableStateOf(false) }
    var showPulseShadowSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IslandPreviewControls()

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_scale_title),
                description = stringResource(R.string.island_expanded_scale_desc),
                value = viewModel.islandExpandedScale.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedScale(it)
                },
                valueRange = 1f..1.3f,
                increment = 0.02f,
                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                valueFormatter = { "${(it * 100).toInt()}%" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_scale"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_font_scale_title),
                value = viewModel.islandFontScale.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandFontScale(it)
                },
                valueRange = 0.8f..1.3f,
                increment = 0.05f,
                iconRes = R.drawable.rounded_format_size_24,
                valueFormatter = { "${(it * 100).toInt()}%" },
                modifier = Modifier.highlight(highlightSetting == "island_font_scale"),
            )


            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_roundness_title),
                value = viewModel.islandExpandedRoundness.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedRoundness(it)
                },
                valueRange = 0f..80f,
                increment = 2f,
                iconRes = R.drawable.rounded_rounded_corner_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_roundness"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_padding_title),
                value = viewModel.islandExpandedPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedPadding(it)
                },
                valueRange = 8f..48f,
                increment = 2f,
                iconRes = R.drawable.rounded_screenshot_region_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_padding"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_top_padding_title),
                value = viewModel.islandExpandedTopPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedTopPadding(it)
                },
                valueRange = 0f..40f,
                increment = 2f,
                iconRes = R.drawable.rounded_vertical_align_top_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_top_padding"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_bottom_padding_title),
                value = viewModel.islandExpandedBottomPadding.floatValue,
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedBottomPadding(it)
                },
                valueRange = 0f..40f,
                increment = 2f,
                iconRes = R.drawable.rounded_vertical_align_bottom_24,
                valueFormatter = { "${it.toInt()} dp" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_bottom_padding"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_square_24,
                title = stringResource(R.string.island_border_outline_title),
                description = stringResource(R.string.island_border_outline_desc),
                isChecked = viewModel.isIslandBorderOutline.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandBorderOutline(checked)
                },
                onSettingsClick = { showBorderOutlineSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_border_outline_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.island_show_glow_title),
                isChecked = viewModel.isIslandShowGlow.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowGlow(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_glow"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.island_pulse_shadow_title),
                description = stringResource(R.string.island_pulse_shadow_desc),
                isChecked = viewModel.isIslandPulseShadow.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandPulseShadow(checked)
                },
                onSettingsClick = { showPulseShadowSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_pulse_shadow_on_notification"),
            )
        }
    }

    if (showBorderOutlineSheet) {
        IslandBorderOutlineOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showBorderOutlineSheet = false },
        )
    }

    if (showPulseShadowSheet) {
        IslandPulseShadowOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showPulseShadowSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandDurationSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            ConfigSliderItem(
                title = stringResource(R.string.island_timeout_title),
                value = (viewModel.islandTimeoutMs.longValue / 1000f),
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandTimeoutMs((it * 1000).toLong())
                },
                valueRange = 2f..60f,
                increment = 0.5f,
                iconRes = R.drawable.rounded_timer_24,
                valueFormatter = { "%.1fs".format(it) },
            )

            val infinityText = stringResource(R.string.island_timeout_infinity)
            ConfigSliderItem(
                title = stringResource(R.string.island_expanded_timeout_title),
                description = stringResource(R.string.island_expanded_timeout_desc),
                value = (viewModel.islandExpandedTimeoutMs.longValue / 1000f),
                onValueChange = {
                    HapticUtil.performUIHaptic(view)
                    viewModel.setIslandExpandedTimeoutMs((it * 1000).toLong())
                },
                valueRange = 0f..30f,
                increment = 1f,
                iconRes = R.drawable.rounded_schedule_24,
                valueFormatter = { if (it <= 0f) infinityText else "${it.toInt()}s" },
                modifier = Modifier.highlight(highlightSetting == "island_expanded_timeout_ms"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandBehaviorSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val previewSettings = remember { SettingsRepository(context) }
    var showWhen by remember { mutableStateOf(previewSettings.getIslandShowWhen()) }
    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    val hasShellPermission =
        if (ShellUtils.isRootEnabled(context)) {
            viewModel.isRootPermissionGranted.value
        } else {
            viewModel.isShizukuPermissionGranted.value
        }

    requestingPermissionsFor?.let { (titleRes, permKeys) ->
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(titleRes),
            permissions = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                    .padding(top = 12.dp)
                    .highlight(highlightSetting == "island_show_when" || highlightSetting == "island_hide_when_screen_off"),
            ) {
                Text(
                    text = stringResource(R.string.island_show_when_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val showWhenOptions = listOf(
                    SettingsRepository.ISLAND_SHOW_WHEN_UNLOCKED,
                    SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON,
                    SettingsRepository.ISLAND_SHOW_WHEN_ALWAYS,
                )
                val showWhenLabels = mapOf(
                    SettingsRepository.ISLAND_SHOW_WHEN_UNLOCKED to stringResource(R.string.island_show_when_unlocked),
                    SettingsRepository.ISLAND_SHOW_WHEN_SCREEN_ON to stringResource(R.string.island_show_when_screen_on),
                    SettingsRepository.ISLAND_SHOW_WHEN_ALWAYS to stringResource(R.string.island_show_when_always),
                )
                SegmentedPicker(
                    items = showWhenOptions,
                    selectedItem = showWhen,
                    onItemSelected = {
                        showWhen = it
                        previewSettings.setIslandShowWhen(it)
                    },
                    labelProvider = { showWhenLabels[it].orEmpty() },
                    title = R.string.island_show_when_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }


            IconToggleItem(
                iconRes = R.drawable.rounded_motion_play_24,
                title = stringResource(R.string.island_line_peek_title),
                description = stringResource(R.string.island_line_peek_desc),
                isChecked = viewModel.isIslandLineStageEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandLineStageEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_line_stage_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_visibility_off_24,
                title = stringResource(R.string.island_hide_in_owner_app_title),
                isChecked = viewModel.isIslandHideInOwnerApp.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandHideInOwnerApp(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_in_owner_app"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_pinch_24,
                title = stringResource(R.string.island_hide_on_shade_title),
                isChecked = viewModel.isIslandHideOnShade.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandHideOnShade(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_on_shade"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.island_dismiss_on_outside_title),
                isChecked = viewModel.isIslandDismissOnOutside.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandDismissOnOutside(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_dismiss_on_outside"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_off_24,
                title = stringResource(R.string.island_suppress_system_heads_up_title),
                isChecked = viewModel.isIslandSuppressSystemHeadsUp.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !PermissionUtils.canWriteSecureSettings(context) && !ShellUtils.isAvailable(context)) {
                        requestingPermissionsFor = Pair(R.string.island_suppress_system_heads_up_title, listOf("WRITE_SECURE_SETTINGS"))
                    } else {
                        viewModel.setIslandSuppressSystemHeadsUp(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_suppress_system_heads_up"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_visibility_off_24,
                title = stringResource(R.string.island_dynamic_hide_status_bar_title),
                isChecked = viewModel.isIslandDynamicHideStatusBar.value && hasShellPermission,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !hasShellPermission) {
                        requestingPermissionsFor =
                            Pair(
                                R.string.island_dynamic_hide_status_bar_title,
                                listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                            )
                    } else {
                        viewModel.setIslandDynamicHideStatusBar(checked, context)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_dynamic_hide_status_bar"),
            )
        }
    }
}

@Composable
fun IslandPreviewControls(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val previewSettings = remember { SettingsRepository(context) }
    var previewRing by remember { mutableStateOf(false) }
    var previewStage by remember { mutableStateOf(SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO) }
    DisposableEffect(Unit) {
        onDispose {
            previewSettings.setIslandPreviewRingEnabled(false)
            previewSettings.setIslandPreviewStage(SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO)
        }
    }

    RoundedCardContainer(
        modifier = modifier,
        spacing = 2.dp,
        cornerRadius = 24.dp,
    ) {
        IconToggleItem(
            iconRes = R.drawable.rounded_circle_24,
            title = stringResource(R.string.island_preview_ring_title),
            isChecked = previewRing,
            onCheckedChange = { checked ->
                HapticUtil.performVirtualKeyHaptic(view)
                previewRing = checked
                previewSettings.setIslandPreviewRingEnabled(checked)
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                .padding(top = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.island_preview_stage_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            val stages = listOf(
                SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO,
                SettingsRepository.ISLAND_PREVIEW_STAGE_PEEK,
                SettingsRepository.ISLAND_PREVIEW_STAGE_EXPANDED,
            )
            val stageLabels = mapOf(
                SettingsRepository.ISLAND_PREVIEW_STAGE_AUTO to stringResource(R.string.island_preview_stage_auto),
                SettingsRepository.ISLAND_PREVIEW_STAGE_PEEK to stringResource(R.string.island_preview_stage_peek),
                SettingsRepository.ISLAND_PREVIEW_STAGE_EXPANDED to stringResource(R.string.island_preview_stage_expanded),
            )
            SegmentedPicker(
                items = stages,
                selectedItem = previewStage,
                onItemSelected = {
                    previewStage = it
                    previewSettings.setIslandPreviewStage(it)
                },
                labelProvider = { stageLabels[it].orEmpty() },
                title = R.string.island_preview_stage_title,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
