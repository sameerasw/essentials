package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

enum class PermissionModule {
    HIDE_GESTURE_BAR,
    SHOW_ON_LAUNCHER,
    CIRCLE_TO_SEARCH,
    DISABLE_ROTATION_SUGGESTION,
    NONE
}

@Composable
fun OtherCustomizationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    var requestingPermissionFor by remember { mutableStateOf(PermissionModule.NONE) }

    if (requestingPermissionFor != PermissionModule.NONE) {
        val isShizukuAvailable = viewModel.isShizukuAvailable.value
        val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
        val isRootAvailable = viewModel.isRootAvailable.value
        val isRootGranted = viewModel.isRootPermissionGranted.value
        val isShellGranted =
            (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)
        val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
        val isUsageStatsGranted = viewModel.isUsageStatsPermissionGranted.value
        val useUsageAccess = viewModel.isUseUsageAccess.value

        val shizukuPermission = PermissionItem(
            iconRes = R.drawable.rounded_adb_24,
            title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
            description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
            dependentFeatures = listOf(
                R.string.feat_hide_gesture_bar_title,
                R.string.feat_hide_gesture_bar_on_launcher_title,
                R.string.feat_circle_to_search_gesture_title
            ),
            actionLabel = if (!isShizukuAvailable) R.string.perm_shizuku_install_action else if (isShellGranted) R.string.perm_action_granted else R.string.perm_action_grant,
            action = {
                if (!isShizukuAvailable) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } else {
                    viewModel.requestShizukuPermission()
                }
            },
            isGranted = isShellGranted
        )

        val accessibilityPermission = PermissionItem(
            iconRes = R.drawable.rounded_accessibility_new_24,
            title = R.string.perm_accessibility_title,
            description = R.string.perm_accessibility_desc_common,
            dependentFeatures = listOf(
                R.string.feat_hide_gesture_bar_on_launcher_title,
                R.string.feat_circle_to_search_gesture_title
            ),
            actionLabel = if (isAccessibilityEnabled) R.string.label_enabled else R.string.perm_action_enable,
            action = {
                com.sameerasw.essentials.utils.PermissionUtils.openAccessibilitySettings(
                    context
                )
            },
            isGranted = isAccessibilityEnabled
        )

        val usageStatsPermission = PermissionItem(
            iconRes = R.drawable.rounded_data_usage_24,
            title = R.string.perm_usage_stats_title,
            description = R.string.perm_usage_stats_desc_app_lock,
            dependentFeatures = listOf(R.string.feat_hide_gesture_bar_on_launcher_title),
            actionLabel = if (isUsageStatsGranted) R.string.perm_action_granted else R.string.perm_action_enable,
            action = { com.sameerasw.essentials.utils.PermissionUtils.openUsageStatsSettings(context) },
            isGranted = isUsageStatsGranted
        )

        val permissionsToShow = when (requestingPermissionFor) {
            PermissionModule.HIDE_GESTURE_BAR -> listOf(shizukuPermission)
            PermissionModule.SHOW_ON_LAUNCHER -> {
                val appDetectionPermission =
                    if (useUsageAccess) usageStatsPermission else accessibilityPermission
                listOf(shizukuPermission, appDetectionPermission)
            }

            PermissionModule.CIRCLE_TO_SEARCH -> listOf(shizukuPermission, accessibilityPermission)
            PermissionModule.DISABLE_ROTATION_SUGGESTION -> listOf(shizukuPermission)
            else -> emptyList()
        }

        PermissionsBottomSheet(
            onDismissRequest = { requestingPermissionFor = PermissionModule.NONE },
            featureTitle = R.string.feat_other_customizations_title,
            permissions = permissionsToShow
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isShizukuGranted =
            viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
        val isRootGranted =
            viewModel.isRootAvailable.value && viewModel.isRootPermissionGranted.value
        val isShellGranted = isShizukuGranted || isRootGranted
        val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
        val isUsageStatsGranted = viewModel.isUsageStatsPermissionGranted.value
        val isAppDetectionGranted =
            if (viewModel.isUseUsageAccess.value) isUsageStatsGranted else isAccessibilityEnabled

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, viewModel.isCircleToSearchGestureEnabled.value) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.setCircleToSearchPreviewEnabled(viewModel.isCircleToSearchGestureEnabled.value)
                    } else if (event == Lifecycle.Event.ON_PAUSE) {
                        viewModel.setCircleToSearchPreviewEnabled(false)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    viewModel.setCircleToSearchPreviewEnabled(false)
                }
            }

            IconToggleItem(
                title = stringResource(R.string.feat_hide_gesture_bar_title),
                description = stringResource(R.string.feat_hide_gesture_bar_desc),
                isChecked = viewModel.isHideGestureBarEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellGranted) {
                        viewModel.setHideGestureBarEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.HIDE_GESTURE_BAR
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted) {
                        requestingPermissionFor = PermissionModule.HIDE_GESTURE_BAR
                    }
                },
                iconRes = R.drawable.rounded_home_24,
                modifier = Modifier.highlight(highlightSetting == "hide_gesture_bar_toggle")
            )

            IconToggleItem(
                title = stringResource(R.string.feat_hide_gesture_bar_on_launcher_title),
                description = stringResource(R.string.feat_hide_gesture_bar_on_launcher_desc),
                isChecked = viewModel.isHideGestureBarOnLauncherEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellGranted && isAppDetectionGranted) {
                        viewModel.setHideGestureBarOnLauncherEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.SHOW_ON_LAUNCHER
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted || !isAppDetectionGranted) {
                        requestingPermissionFor = PermissionModule.SHOW_ON_LAUNCHER
                    }
                },
                iconRes = R.drawable.rounded_home_health_24,
                modifier = Modifier.highlight(highlightSetting == "hide_gesture_bar_launcher_toggle")
            )

            IconToggleItem(
                title = stringResource(R.string.feat_circle_to_search_gesture_title),
                description = stringResource(R.string.feat_circle_to_search_gesture_desc),
                isChecked = viewModel.isCircleToSearchGestureEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellGranted && isAccessibilityEnabled) {
                        viewModel.setCircleToSearchGestureEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.CIRCLE_TO_SEARCH
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted || !isAccessibilityEnabled) {
                        requestingPermissionFor = PermissionModule.CIRCLE_TO_SEARCH
                    }
                },
                iconRes = R.drawable.rounded_touch_app_24,
                modifier = Modifier.highlight(highlightSetting == "circle_to_search_gesture_toggle")
            )

            IconToggleItem(
                title = stringResource(R.string.feat_disable_rotation_suggestion_title),
                description = stringResource(R.string.feat_disable_rotation_suggestion_desc),
                isChecked = viewModel.isDisableRotationSuggestionEnabled.value,
                onCheckedChange = { enabled ->
                    if (viewModel.isWriteSecureSettingsEnabled.value || viewModel.isShizukuPermissionGranted.value || viewModel.isRootPermissionGranted.value) {
                        viewModel.setDisableRotationSuggestionEnabled(enabled, context)
                    } else {
                        requestingPermissionFor = PermissionModule.DISABLE_ROTATION_SUGGESTION
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!viewModel.isWriteSecureSettingsEnabled.value && !viewModel.isShizukuPermissionGranted.value && !viewModel.isRootPermissionGranted.value) {
                        requestingPermissionFor = PermissionModule.DISABLE_ROTATION_SUGGESTION
                    }
                },
                iconRes = R.drawable.rounded_mobile_rotate_24,
                modifier = Modifier.highlight(highlightSetting == "disable_rotation_suggestion_toggle")
            )

            AnimatedVisibility(
                visible = viewModel.isCircleToSearchGestureEnabled.value,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.feat_circle_to_search_gesture_height_title),
                    value = viewModel.circleToSearchGestureHeight.floatValue,
                    onValueChange = { viewModel.setCircleToSearchGestureHeight(it) },
                    valueRange = 24f..120f,
                    increment = 4f,
                    iconRes = R.drawable.rounded_border_bottom_24,
                    description = stringResource(R.string.feat_circle_to_search_gesture_height_desc),
                    valueFormatter = { "${it.toInt()} dp" }
                )
            }
        }
    }
}
