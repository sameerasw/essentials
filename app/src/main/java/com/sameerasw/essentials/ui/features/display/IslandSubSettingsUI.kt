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
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationHubOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandNotificationQueueOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandBriefOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandCallOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandDevicesBatteryBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandPrefToggle
import com.sameerasw.essentials.ui.features.display.sheets.IslandPulseShadowOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimeOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.IslandTimerOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.StatusGlanceCalendarOptionsBottomSheet
import com.sameerasw.essentials.ui.features.display.sheets.rememberIslandShowsWhileLocked
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
const val ISLAND_NOTIFICATIONS_FEATURE_ID = "Island notifications"

val ISLAND_SUB_PAGE_IDS = setOf(
    ISLAND_PLACEMENT_FEATURE_ID,
    ISLAND_VISUALS_FEATURE_ID,
    ISLAND_DURATION_FEATURE_ID,
    ISLAND_BEHAVIOR_FEATURE_ID,
    ISLAND_NOTIFICATIONS_FEATURE_ID,
)

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
    val displayConfiguration = androidx.compose.ui.platform.LocalConfiguration.current
    LaunchedEffect(displayConfiguration.screenWidthDp, displayConfiguration.screenHeightDp,
        displayConfiguration.orientation) {
        viewModel.refreshIslandCameraPlacement()
    }
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
            if (viewModel.isFoldableCameraDevice.value) {
                Text(
                    text = stringResource(R.string.foldable_camera_profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_center_focus_strong_24,
                    title = stringResource(R.string.foldable_orientation_profiles_title),
                    isChecked = viewModel.isIslandOrientationProfiles.value,
                    onCheckedChange = { viewModel.setIslandOrientationProfiles(it) },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_visibility_off_24,
                    title = stringResource(R.string.foldable_hide_current_orientation_title),
                    isChecked = viewModel.isIslandHiddenInCurrentOrientation.value,
                    onCheckedChange = { viewModel.setIslandHiddenInCurrentOrientation(it) },
                )
            }
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
                    valueFormatter = { "%.1f%%".format(it) },
                    allowDecimals = true,
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
                    allowDecimals = true,
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
                allowDecimals = true,
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
                valueFormatter = { "%.1f dp".format(it) },
                allowDecimals = true,
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
                valueFormatter = { "%.1f dp".format(it) },
                allowDecimals = true,
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
                valueFormatter = { "%.1f dp".format(it) },
                allowDecimals = true,
                modifier = Modifier.highlight(highlightSetting == "island_cutout_gap"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_rotate_24,
                title = stringResource(R.string.island_keep_on_landscape_title),
                isChecked = viewModel.isIslandKeepOnLandscape.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandKeepOnLandscape(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_keep_on_landscape"),
            )

            if (viewModel.isIslandKeepOnLandscape.value) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_landscape_top_spacing_title),
                    value = viewModel.islandLandscapeTopSpacing.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandLandscapeTopSpacing(it)
                    },
                    valueRange = 0f..60f,
                    increment = 0.5f,
                    iconRes = R.drawable.rounded_vertical_align_top_24,
                    valueFormatter = { "%.1f dp".format(it) },
                    modifier = Modifier.highlight(highlightSetting == "island_landscape_top_spacing"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_vertical_align_top_24,
                    title = stringResource(R.string.island_bond_edge_title),
                    isChecked = viewModel.isIslandBondEdge.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBondEdge(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_bond_edge"),
                )
            }
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
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_secondary_bubble_title),
                isChecked = viewModel.isIslandSecondaryBubble.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandSecondaryBubble(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_secondary_bubble"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_format_align_center_24,
                title = stringResource(R.string.island_balance_compact_title),
                isChecked = viewModel.isIslandBalanceCompact.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandBalanceCompact(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_balance_compact"),
            )

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
                iconRes = R.drawable.rounded_fullscreen_portrait_24,
                title = stringResource(R.string.keep_in_full_screen_title),
                isChecked = viewModel.isIslandKeepInFullscreen.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandKeepInFullscreen(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_keep_in_fullscreen"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandKeepInFullscreen.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_do_not_touch_24,
                    title = stringResource(R.string.disable_interactions_in_full_screen_title),
                    isChecked = viewModel.isIslandDisableInteractionsInFullscreen.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandDisableInteractionsInFullscreen(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_disable_interactions_in_fullscreen"),
                )
            }
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
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
                iconRes = R.drawable.rounded_notifications_off_24,
                title = stringResource(R.string.island_hide_live_updates_title),
                isChecked = viewModel.isIslandHideLiveUpdates.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !hasShellPermission) {
                        requestingPermissionsFor =
                            Pair(
                                R.string.island_hide_live_updates_title,
                                listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                            )
                    } else {
                        viewModel.setIslandHideLiveUpdates(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_hide_live_updates"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_visibility_off_24,
                title = stringResource(R.string.island_compact_hide_status_bar_title),
                isChecked = viewModel.isIslandCompactHideStatusBar.value && hasShellPermission,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !hasShellPermission) {
                        requestingPermissionsFor =
                            Pair(
                                R.string.island_compact_hide_status_bar_title,
                                listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                            )
                    } else {
                        viewModel.setIslandCompactHideStatusBar(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_compact_hide_status_bar"),
            )
        }

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IslandHideIconsToggle(
                viewModel = viewModel,
                hasShellPermission = hasShellPermission,
                onNeedPermission = {
                    requestingPermissionsFor =
                        Pair(
                            R.string.island_hide_icons_title,
                            listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                        )
                },
                highlighted = highlightSetting == "island_hide_icons",
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

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandNotificationSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showFilterAppsSheet by remember { mutableStateOf(false) }
    var showHubSheet by remember { mutableStateOf(false) }
    var showOtpPermissions by remember { mutableStateOf(false) }
    var showQueueSheet by remember {
        mutableStateOf(
            viewModel.isIslandNotifQueue.value &&
                highlightSetting in setOf("island_notif_queue_style", "island_notif_queue_inline"),
        )
    }
    var concealed by remember {
        mutableStateOf(
            SettingsRepository(context).getBoolean(SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED, false)
        )
    }
    // A notification can only peek as a line while Behavior's island-wide Line peek is on.
    val linePeekOn = viewModel.isIslandLineStageEnabled.value
    val compactHeadsUpOn = linePeekOn && viewModel.isIslandNotifCompactHeadsUp.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_notif_show_title),
                isChecked = viewModel.isIslandShowNotifications.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandShowNotifications(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_show_notifications"),
            )

            IconToggleItem(
                iconRes = R.drawable.outline_circle_notifications_24,
                title = stringResource(R.string.island_notif_hub_title),
                isChecked = viewModel.isIslandNotifHub.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifHub(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_hub"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandNotifHub.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_24,
                    title = stringResource(R.string.island_hide_icon_notifications),
                    showToggle = false,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showHubSheet = true
                    },
                )
            }
        }

        IslandSettingsSection(R.string.island_notif_section_sources) {
            IconToggleItem(
                iconRes = R.drawable.rounded_filter_alt_24,
                title = stringResource(R.string.island_notif_filter_apps_title),
                description = stringResource(R.string.island_notif_filter_apps_desc),
                isChecked = viewModel.isIslandNotifFilterApps.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifFilterApps(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_filter_apps"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandNotifFilterApps.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.action_select_apps),
                    showToggle = false,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showFilterAppsSheet = true
                    },
                )
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_off_24,
                title = stringResource(R.string.island_notif_skip_silent_title),
                description = stringResource(R.string.island_notif_skip_silent_desc),
                isChecked = viewModel.isIslandNotifSkipSilent.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifSkipSilent(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_skip_silent"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_do_not_disturb_on_24,
                title = stringResource(R.string.island_notif_follow_dnd_title),
                description = stringResource(R.string.island_notif_follow_dnd_desc),
                isChecked = viewModel.isIslandNotifFollowDnd.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifFollowDnd(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_follow_dnd"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_downloading_24,
                title = stringResource(R.string.island_notif_keep_progress_title),
                description = stringResource(R.string.island_notif_keep_progress_desc),
                isChecked = viewModel.isIslandNotifKeepProgress.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifKeepProgress(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_keep_progress"),
            )
        }

        IslandSettingsSection(R.string.island_notif_section_heads_up) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notification_sound_24,
                title = stringResource(R.string.island_notif_compact_heads_up_title),
                description = stringResource(
                    if (linePeekOn) R.string.island_notif_compact_heads_up_desc
                    else R.string.island_notif_compact_heads_up_needs_line_peek,
                ),
                isChecked = compactHeadsUpOn,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifCompactHeadsUp(checked)
                },
                enabled = linePeekOn,
                onDisabledClick = {
                    context.startActivity(
                        Intent(context, FeatureSettingsActivity::class.java).apply {
                            putExtra("feature", ISLAND_BEHAVIOR_FEATURE_ID)
                            putExtra("highlight_setting", "island_line_stage_enabled")
                        },
                    )
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_compact_heads_up"),
            )

            AnimatedVisibility(
                visible = compactHeadsUpOn,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val peekStyle = viewModel.islandNotifPeekStyle.value
                val fullLabel = stringResource(R.string.island_notif_peek_style_full)
                val compactLabel = stringResource(R.string.island_notif_peek_style_compact)
                val minimalLabel = stringResource(R.string.island_notif_peek_style_minimal)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                        .padding(top = 12.dp)
                        .highlight(highlightSetting == "island_notif_peek_style"),
                ) {
                    Text(
                        text = stringResource(R.string.island_notif_condensed_info_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(
                            when (peekStyle) {
                                SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_COMPACT -> R.string.island_notif_condensed_info_desc
                                SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_MINIMAL ->
                                    if (viewModel.isIslandCatchUpEnabled.value) R.string.island_notif_peek_style_minimal_catch_up_desc
                                    else R.string.island_notif_peek_style_minimal_desc
                                else -> R.string.island_notif_peek_style_full_desc
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedPicker(
                        items = listOf(
                            SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_FULL,
                            SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_COMPACT,
                            SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_MINIMAL,
                        ),
                        selectedItem = peekStyle,
                        onItemSelected = { viewModel.setIslandNotifPeekStyle(it) },
                        labelProvider = {
                            when (it) {
                                SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_COMPACT -> compactLabel
                                SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_MINIMAL -> minimalLabel
                                else -> fullLabel
                            }
                        },
                        title = R.string.island_notif_condensed_info_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        IslandSettingsSection(R.string.island_notif_section_in_island) {
            IconToggleItem(
                iconRes = R.drawable.outline_circle_notifications_24,
                title = stringResource(R.string.island_notif_queue_title),
                description = stringResource(R.string.island_notif_queue_desc),
                isChecked = viewModel.isIslandNotifQueue.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifQueue(checked)
                },
                enabled = !viewModel.isIslandNotifHub.value,
                onSettingsClick = { showQueueSheet = true },
                modifier = Modifier.highlight(highlightSetting == "island_notif_queue"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_touch_app_24,
                title = stringResource(R.string.island_notif_tap_to_open_title),
                description = stringResource(R.string.island_notif_tap_to_open_desc),
                isChecked = viewModel.isIslandNotifTapToOpen.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandNotifTapToOpen(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "island_notif_tap_to_open"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.island_catch_up_title),
                description = stringResource(R.string.island_catch_up_desc),
                isChecked = viewModel.isIslandCatchUpEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setIslandCatchUpEnabled(checked)
                },
                enabled = !viewModel.isIslandNotifHub.value,
                modifier = Modifier.highlight(highlightSetting == "island_catch_up"),
            )

            AnimatedVisibility(
                visible = viewModel.isIslandCatchUpEnabled.value && !viewModel.isIslandNotifHub.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val infinityText = stringResource(R.string.island_timeout_infinity)
                ConfigSliderItem(
                    title = stringResource(R.string.island_catch_up_timeout_title),
                    value = catchUpStepIndex(viewModel.islandCatchUpTimeoutMs.longValue).toFloat(),
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandCatchUpTimeoutMs(
                            CATCH_UP_STEPS_MS[it.toInt().coerceIn(CATCH_UP_STEPS_MS.indices)]
                        )
                    },
                    valueRange = 0f..CATCH_UP_STEPS_MS.lastIndex.toFloat(),
                    increment = 1f,
                    iconRes = R.drawable.rounded_timer_24,
                    valueFormatter = {
                        formatCatchUp(
                            CATCH_UP_STEPS_MS[it.toInt().coerceIn(CATCH_UP_STEPS_MS.indices)],
                            infinityText,
                        )
                    },
                    modifier = Modifier.highlight(highlightSetting == "island_catch_up_timeout"),
                )
            }
        }

        if (showOtpPermissions) {
            PermissionsBottomSheet(
                onDismissRequest = {
                    showOtpPermissions = false
                    viewModel.check(context)
                },
                featureTitle = stringResource(R.string.island_otp_section),
                permissions = PermissionUIHelper.getPermissionItems(listOf("SENSITIVE_NOTIFICATIONS"), context, viewModel),
            )
        }

        IslandSettingsSection(R.string.island_otp_section) {
            val otpEnabled = viewModel.isIslandOtpEnabled.value
            IconToggleItem(
                iconRes = R.drawable.rounded_vpn_key_24,
                title = stringResource(R.string.island_otp_enabled_title),
                isChecked = otpEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isSensitiveNotificationsGranted.value) {
                        showOtpPermissions = true
                    } else {
                        viewModel.setIslandOtpEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "island_otp_enabled"),
            )
            AnimatedVisibility(
                visible = otpEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_OTP_TAP_TO_COPY,
                    iconRes = R.drawable.rounded_content_copy_24,
                    title = stringResource(R.string.island_otp_tap_to_copy_title),
                    defaultValue = true,
                    modifier = Modifier.highlight(highlightSetting == "island_otp_tap_to_copy"),
                )
            }
            AnimatedVisibility(
                visible = otpEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_OTP_DISMISS_AFTER_COPY,
                    iconRes = R.drawable.rounded_check_circle_24,
                    title = stringResource(R.string.island_otp_dismiss_after_copy_title),
                )
            }
            AnimatedVisibility(
                visible = otpEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_otp_timeout_title),
                    value = OTP_TIMEOUT_STEPS_MS.indexOfClosest(viewModel.islandOtpTimeoutMs.longValue).toFloat(),
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandOtpTimeoutMs(OTP_TIMEOUT_STEPS_MS[it.toInt().coerceIn(OTP_TIMEOUT_STEPS_MS.indices)])
                    },
                    valueRange = 0f..OTP_TIMEOUT_STEPS_MS.lastIndex.toFloat(),
                    increment = 1f,
                    iconRes = R.drawable.rounded_timer_24,
                    valueFormatter = { formatCatchUp(OTP_TIMEOUT_STEPS_MS[it.toInt().coerceIn(OTP_TIMEOUT_STEPS_MS.indices)], "") },
                )
            }
            AnimatedVisibility(
                visible = otpEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_OTP_BYPASS_FILTER,
                    iconRes = R.drawable.rounded_filter_alt_24,
                    title = stringResource(R.string.island_otp_bypass_filter_title),
                    defaultValue = true,
                    modifier = Modifier.highlight(highlightSetting == "island_otp_bypass_filter"),
                )
            }
        }

        if (rememberIslandShowsWhileLocked()) {
            IslandSettingsSection(R.string.island_notif_section_lock_screen) {
                IslandPrefToggle(
                    settingKey = SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED,
                    iconRes = R.drawable.rounded_visibility_off_24,
                    title = stringResource(R.string.island_notif_conceal_locked_title),
                    onChanged = { concealed = it },
                )
                AnimatedVisibility(
                    visible = concealed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_CHAT_PICTURES,
                        iconRes = R.drawable.rounded_person_24,
                        title = stringResource(R.string.island_notif_conceal_chat_pictures_title),
                    )
                }
            }
        }
    }

    if (showHubSheet) {
        IslandNotificationHubOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showHubSheet = false },
        )
    }

    if (showQueueSheet) {
        IslandNotificationQueueOptionsBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showQueueSheet = false },
        )
    }

    if (showFilterAppsSheet) {
        AppSelectionSheet(
            title = stringResource(R.string.island_notif_filter_apps_title),
            onDismissRequest = { showFilterAppsSheet = false },
            onLoadApps = { viewModel.loadIslandNotifFilterApps() },
            onSaveApps = { _, apps -> viewModel.saveIslandNotifFilterApps(apps) },
            onAppToggle = { _, pkg, enabled -> viewModel.updateIslandNotifFilterApp(pkg, enabled) },
            context = context,
        )
    }
}

@Composable
private fun IslandSettingsSection(titleRes: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
            content = content,
        )
    }
}

private val CATCH_UP_STEPS_MS =
    longArrayOf(5_000, 10_000, 15_000, 20_000, 30_000, 45_000, 60_000, 120_000, 180_000, 300_000, 600_000, 900_000, 0)

private val OTP_TIMEOUT_STEPS_MS = longArrayOf(15_000, 30_000, 60_000, 120_000, 180_000, 300_000)

private fun LongArray.indexOfClosest(ms: Long): Int = indices.minBy { kotlin.math.abs(this[it] - ms) }

private fun catchUpStepIndex(ms: Long): Int =
    if (ms <= 0L) CATCH_UP_STEPS_MS.lastIndex
    else CATCH_UP_STEPS_MS.indices.filter { CATCH_UP_STEPS_MS[it] > 0 }.minBy { kotlin.math.abs(CATCH_UP_STEPS_MS[it] - ms) }

private fun formatCatchUp(ms: Long, infinityText: String): String =
    when {
        ms <= 0L -> infinityText
        ms < 60_000 -> "${ms / 1000}s"
        else -> "${ms / 60_000}m"
    }
