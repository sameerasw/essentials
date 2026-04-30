package com.sameerasw.essentials.ui.composables.configs

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService
import com.sameerasw.essentials.services.tiles.AlwaysOnDisplayTileService
import com.sameerasw.essentials.services.tiles.AppFreezingTileService
import com.sameerasw.essentials.services.tiles.AppLockTileService
import com.sameerasw.essentials.services.tiles.BatteryNotificationTileService
import com.sameerasw.essentials.services.tiles.BubblesTileService
import com.sameerasw.essentials.services.tiles.CaffeinateTileService
import com.sameerasw.essentials.services.tiles.ChargeQuickTileService
import com.sameerasw.essentials.services.tiles.DeveloperOptionsTileService
import com.sameerasw.essentials.services.tiles.DynamicNightLightTileService
import com.sameerasw.essentials.services.tiles.FlashlightPulseTileService
import com.sameerasw.essentials.services.tiles.FlashlightTileService
import com.sameerasw.essentials.services.tiles.MapsPowerSavingTileService
import com.sameerasw.essentials.services.tiles.MonoAudioTileService
import com.sameerasw.essentials.services.tiles.NfcTileService
import com.sameerasw.essentials.services.tiles.NotificationLightingTileService
import com.sameerasw.essentials.services.tiles.PrivateDnsTileService
import com.sameerasw.essentials.services.tiles.PrivateNotificationsTileService
import com.sameerasw.essentials.services.tiles.RefreshRateTileService
import com.sameerasw.essentials.services.tiles.ScreenLockedSecurityTileService
import com.sameerasw.essentials.services.tiles.ScaleAnimationsTileService
import com.sameerasw.essentials.services.tiles.SoundModeTileService
import com.sameerasw.essentials.services.tiles.StayAwakeTileService
import com.sameerasw.essentials.services.tiles.TapToWakeTileService
import com.sameerasw.essentials.services.tiles.UiBlurTileService
import com.sameerasw.essentials.services.tiles.UsbDebuggingTileService
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

data class QSTileInfo(
    val titleRes: Int,
    val iconRes: Int,
    val serviceClass: Class<*>,
    val permissionKeys: List<String> = emptyList(),
    val aboutDescription: Int? = null,
    val categoryRes: Int
)

@Composable
fun QuickSettingsTilesSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    LocalView.current
    val viewModel: MainViewModel = viewModel()

    var showPermissionSheet by remember { mutableStateOf(false) }
    var selectedTileForPermissions by remember { mutableStateOf<QSTileInfo?>(null) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var selectedHelpTile by remember { mutableStateOf<QSTileInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }


    val isUseUsageStats by viewModel.isUseUsageAccess
    val tiles = listOf(
        QSTileInfo(
            R.string.tile_ui_blur,
            R.drawable.rounded_blur_on_24,
            UiBlurTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_ui_blur,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_bubbles,
            R.drawable.rounded_bubble_24,
            BubblesTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_bubbles,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_sensitive_content,
            R.drawable.rounded_notifications_off_24,
            PrivateNotificationsTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_sensitive_content,
            R.string.cat_privacy
        ),
        QSTileInfo(
            R.string.tile_tap_to_wake,
            R.drawable.rounded_touch_app_24,
            TapToWakeTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_tap_to_wake,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_aod,
            R.drawable.rounded_mobile_text_2_24,
            AlwaysOnDisplayTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf("ROOT")
            else if (PermissionUtils.canWriteSecureSettings(context)) listOf("WRITE_SECURE_SETTINGS")
            else listOf("SHIZUKU"),
            R.string.about_desc_aod,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_caffeinate,
            R.drawable.rounded_coffee_24,
            CaffeinateTileService::class.java,
            listOf("POST_NOTIFICATIONS"),
            R.string.about_desc_caffeinate,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_sound_mode,
            R.drawable.rounded_volume_up_24,
            SoundModeTileService::class.java,
            listOf("NOTIFICATION_POLICY"),
            R.string.about_desc_sound_mode_tile,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_notification_lighting,
            R.drawable.rounded_blur_linear_24,
            NotificationLightingTileService::class.java,
            listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            R.string.about_desc_notification_lighting,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_dynamic_night_light,
            R.drawable.rounded_nightlight_24,
            DynamicNightLightTileService::class.java,
            listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS"),
            R.string.about_desc_dynamic_night_light,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_locked_security,
            R.drawable.rounded_security_24,
            ScreenLockedSecurityTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU"),
            R.string.about_desc_screen_locked_security,
            R.string.cat_privacy
        ),
        QSTileInfo(
            R.string.tile_app_lock,
            R.drawable.rounded_shield_lock_24,
            AppLockTileService::class.java,
            if (isUseUsageStats) listOf("USAGE_STATS") else listOf("ACCESSIBILITY"),
            R.string.about_desc_app_lock,
            R.string.cat_privacy
        ),
        QSTileInfo(
            R.string.tile_mono_audio,
            R.drawable.rounded_headphones_24,
            MonoAudioTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf("ROOT")
            else if (PermissionUtils.canWriteSecureSettings(context)) listOf("WRITE_SECURE_SETTINGS")
            else listOf("SHIZUKU"),
            R.string.about_desc_mono_audio,
            R.string.cat_accessibility
        ),
        QSTileInfo(
            R.string.tile_flashlight,
            R.drawable.rounded_flashlight_on_24,
            FlashlightTileService::class.java,
            emptyList(),
            R.string.about_desc_flashlight_tile,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_app_freezing,
            R.drawable.rounded_app_badging_24,
            AppFreezingTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf(
                "ROOT",
                "USAGE_STATS",
                "NOTIFICATION_LISTENER"
            ) else listOf("SHIZUKU", "USAGE_STATS", "NOTIFICATION_LISTENER"),
            R.string.about_desc_freeze,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_flashlight_pulse,
            R.drawable.outline_backlight_high_24,
            FlashlightPulseTileService::class.java,
            listOf("NOTIFICATION_LISTENER"),
            R.string.about_desc_flashlight_pulse,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_stay_awake,
            R.drawable.rounded_av_timer_24,
            StayAwakeTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_stay_awake,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.nfc_tile_label,
            R.drawable.rounded_nfc_24,
            NfcTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf("ROOT")
            else if (PermissionUtils.canWriteSecureSettings(context)) listOf("WRITE_SECURE_SETTINGS")
            else listOf("SHIZUKU"),
            R.string.about_desc_nfc,
            R.string.cat_connectivity
        ),
        QSTileInfo(
            R.string.tile_adaptive_brightness,
            R.drawable.rounded_brightness_auto_24,
            AdaptiveBrightnessTileService::class.java,
            listOf("WRITE_SETTINGS"),
            R.string.about_desc_adaptive_brightness,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_scale_animations,
            R.drawable.rounded_front_hand_24,
            ScaleAnimationsTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_scale_animations_tile,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_refresh_rate,
            R.drawable.rounded_shutter_speed_24,
            RefreshRateTileService::class.java,
            listOf("SHIZUKU"),
            R.string.about_desc_refresh_rate_tile,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.feat_maps_power_saving_title,
            R.drawable.rounded_navigation_24,
            MapsPowerSavingTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf(
                "ROOT",
                "NOTIFICATION_LISTENER"
            ) else listOf("SHIZUKU", "NOTIFICATION_LISTENER"),
            R.string.about_desc_maps_power_saving,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_private_dns,
            R.drawable.rounded_dns_24,
            PrivateDnsTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_private_dns,
            R.string.cat_connectivity
        ),
        QSTileInfo(
            R.string.tile_usb_debugging,
            R.drawable.rounded_adb_24,
            UsbDebuggingTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_usb_debugging,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_color_picker,
            R.drawable.rounded_colorize_24,
            com.sameerasw.essentials.services.tiles.ColorPickerTileService::class.java,
            emptyList(),
            R.string.about_desc_color_picker,
            R.string.cat_visuals
        ),
        QSTileInfo(
            R.string.tile_developer_options,
            R.drawable.rounded_mobile_code_24,
            DeveloperOptionsTileService::class.java,
            listOf("WRITE_SECURE_SETTINGS"),
            R.string.about_desc_developer_options,
            R.string.cat_utils
        ),
        QSTileInfo(
            R.string.tile_charge_optimization,
            R.drawable.rounded_battery_android_frame_shield_24,
            ChargeQuickTileService::class.java,
            if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU"),
            R.string.about_desc_charge_optimization,
            R.string.cat_utils
        )
    )

    if (showPermissionSheet && selectedTileForPermissions != null) {
        val permissionItems = PermissionUIHelper.getPermissionItems(
            selectedTileForPermissions!!.permissionKeys,
            context,
            viewModel,
            context as? Activity
        )
        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = {
                    showPermissionSheet = false
                    selectedTileForPermissions = null
                },
                featureTitle = selectedTileForPermissions!!.titleRes,
                permissions = permissionItems
            )
        }
    }

    if (showHelpSheet && selectedHelpTile != null) {
        val tileId = stringResource(selectedHelpTile!!.titleRes)
        val tempFeature = object : com.sameerasw.essentials.domain.model.Feature(
            id = tileId,
            title = selectedHelpTile!!.titleRes,
            iconRes = selectedHelpTile!!.iconRes,
            category = R.string.cat_system,
            description = 0,
            aboutDescription = selectedHelpTile!!.aboutDescription,
            permissionKeys = selectedHelpTile!!.permissionKeys,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(
                viewModel: MainViewModel,
                context: android.content.Context,
                enabled: Boolean
            ) {
            }
        }

        com.sameerasw.essentials.ui.components.sheets.FeatureHelpBottomSheet(
            onDismissRequest = {
                showHelpSheet = false
                selectedHelpTile = null
            },
            feature = tempFeature
        )
    }

    val categoryOrder = listOf(
        R.string.cat_utils,
        R.string.cat_visuals,
        R.string.cat_connectivity,
        R.string.cat_privacy,
        R.string.cat_accessibility
    )

    val categorizedTiles = tiles.groupBy { it.categoryRes }
        .toList()
        .sortedBy { (category, _) ->
            val index = categoryOrder.indexOf(category)
            if (index != -1) index else Int.MAX_VALUE
        }


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
        Spacer(modifier = Modifier.height(16.dp))

        categorizedTiles.forEachIndexed { index, (categoryRes, tilesInSection) ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            val categoryIcon = when (categoryRes) {
                R.string.cat_utils -> R.drawable.rounded_settings_24
                R.string.cat_visuals -> R.drawable.rounded_brightness_6_24
                R.string.cat_connectivity -> R.drawable.rounded_android_wifi_3_bar_24
                R.string.cat_privacy -> R.drawable.rounded_shield_24
                R.string.cat_accessibility -> R.drawable.rounded_accessibility_new_24
                else -> null
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp, start = 12.dp)
            ) {
                if (categoryIcon != null) {
                    Icon(
                        painter = painterResource(id = categoryIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = stringResource(categoryRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            tilesInSection.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTiles.forEach { tile ->
                        // Map permission keys to actual granted state
                        val allPermissionsGranted = tile.permissionKeys.all { key ->
                            PermissionUIHelper.getPermissionItem(
                                key,
                                context,
                                viewModel
                            )?.isGranted == true
                        }

                        val addedTiles by viewModel.addedQSTiles
                        val componentName = ComponentName(context, tile.serviceClass)
                        val isAdded = addedTiles.any { 
                            it.contains(componentName.flattenToString()) || 
                            it.contains(componentName.flattenToShortString()) ||
                            it.contains(tile.serviceClass.name)
                        }

                        QSTileCard(
                            tile = tile,
                            modifier = Modifier
                                .weight(1f)
                                .highlight(
                                    highlightSetting.equals(
                                        context.getString(tile.titleRes),
                                        ignoreCase = true
                                    )
                                ),
                            isMissingPermissions = !allPermissionsGranted,
                            isAdded = isAdded,
                            onClick = {
                                if (!allPermissionsGranted) {
                                    selectedTileForPermissions = tile
                                    showPermissionSheet = true
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val statusBarManager =
                                            context.getSystemService(StatusBarManager::class.java)
                                        val componentName = ComponentName(context, tile.serviceClass)

                                        statusBarManager.requestAddTileService(
                                            componentName,
                                            context.getString(tile.titleRes),
                                            Icon.createWithResource(context, tile.iconRes),
                                            context.mainExecutor
                                        ) { result ->
                                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.qs_tile_already_added),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.qs_tile_requires_android_13),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onHelpClick = if (tile.aboutDescription != null) {
                                {
                                    selectedHelpTile = tile
                                    showHelpSheet = true
                                }
                            } else null
                        )
                    }
                    // Determine if we need a spacer for the last odd item
                    if (rowTiles.size < 2) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Text(
            text = "Long press a tile to see what it does",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

@Composable
fun QSTileCard(
    tile: QSTileInfo,
    isMissingPermissions: Boolean,
    isAdded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onHelpClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    val menuState = com.sameerasw.essentials.ui.state.LocalMenuStateManager.current
    androidx.compose.runtime.DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = tile.titleRes
        } else {
            if (menuState.activeId == tile.titleRes) {
                menuState.activeId = null
            }
        }
        onDispose {
            if (menuState.activeId == tile.titleRes) {
                menuState.activeId = null
            }
        }
    }

    val isBlurred = menuState.activeId != null && menuState.activeId != tile.titleRes
    val blurRadius by animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isMissingPermissions) MaterialTheme.colorScheme.errorContainer
                else if (isAdded) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primary
            )
            .combinedClickable(
                onClick = {
                    com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
                onLongClick = {
                    if (onHelpClick != null) {
                        com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                        showMenu = true
                    }
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .blur(blurRadius),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val contentColor = if (isMissingPermissions) MaterialTheme.colorScheme.onErrorContainer
            else if (isAdded) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onPrimary

            Icon(
                painter = painterResource(id = tile.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(8.dp)
            )

            Column {
                Text(
                    text = stringResource(tile.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (isMissingPermissions) "Grant permission"
                    else if (isAdded) stringResource(R.string.action_added)
                    else stringResource(R.string.action_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        if (onHelpClick != null) {
            com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.action_what_is_this))
                    },
                    onClick = {
                        showMenu = false
                        onHelpClick()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_help_24),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
