package com.sameerasw.essentials.ui.composables.configs

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.AlwaysOnDisplayTileService
import com.sameerasw.essentials.services.tiles.BubblesTileService
import com.sameerasw.essentials.services.tiles.CaffeinateTileService
import com.sameerasw.essentials.services.tiles.DynamicNightLightTileService
import com.sameerasw.essentials.services.tiles.NotificationLightingTileService
import com.sameerasw.essentials.services.tiles.MonoAudioTileService
import com.sameerasw.essentials.services.tiles.PrivateNotificationsTileService
import com.sameerasw.essentials.services.tiles.ScreenLockedSecurityTileService
import com.sameerasw.essentials.services.tiles.SoundModeTileService
import com.sameerasw.essentials.services.tiles.TapToWakeTileService
import com.sameerasw.essentials.services.tiles.UiBlurTileService
import com.sameerasw.essentials.services.tiles.FlashlightTileService
import com.sameerasw.essentials.services.tiles.AppFreezingTileService
import com.sameerasw.essentials.services.tiles.AppLockTileService
import com.sameerasw.essentials.services.tiles.FlashlightPulseTileService
import com.sameerasw.essentials.services.tiles.NfcTileService
import com.sameerasw.essentials.services.tiles.StayAwakeTileService
import com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService
import com.sameerasw.essentials.services.tiles.MapsPowerSavingTileService
import com.sameerasw.essentials.services.tiles.PrivateDnsTileService
import com.sameerasw.essentials.services.tiles.UsbDebuggingTileService
import com.sameerasw.essentials.ui.modifiers.highlight

import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import android.app.Activity
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet

data class QSTileInfo(
    val titleRes: Int,
    val iconRes: Int,
    val serviceClass: Class<*>,
    val permissionKeys: List<String> = emptyList()
)

@Composable
fun QuickSettingsTilesSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val viewModel: MainViewModel = viewModel()

    var showPermissionSheet by remember { mutableStateOf(false) }
    var selectedTileForPermissions by remember { mutableStateOf<QSTileInfo?>(null) }

    val tiles = listOf(
        QSTileInfo(R.string.tile_ui_blur, R.drawable.rounded_blur_on_24, UiBlurTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_bubbles, R.drawable.rounded_bubble_24, BubblesTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_sensitive_content, R.drawable.rounded_notifications_off_24, PrivateNotificationsTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_tap_to_wake, R.drawable.rounded_touch_app_24, TapToWakeTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_aod, R.drawable.rounded_mobile_text_2_24, AlwaysOnDisplayTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_caffeinate, R.drawable.rounded_coffee_24, CaffeinateTileService::class.java, listOf("POST_NOTIFICATIONS")),
        QSTileInfo(R.string.tile_sound_mode, R.drawable.rounded_volume_up_24, SoundModeTileService::class.java, listOf("NOTIFICATION_POLICY")),
        QSTileInfo(R.string.tile_notification_lighting, R.drawable.rounded_blur_linear_24, NotificationLightingTileService::class.java, listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER")),
        QSTileInfo(R.string.tile_dynamic_night_light, R.drawable.rounded_nightlight_24, DynamicNightLightTileService::class.java, listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_locked_security, R.drawable.rounded_security_24, ScreenLockedSecurityTileService::class.java, listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS", "DEVICE_ADMIN")),
        QSTileInfo(R.string.tile_app_lock, R.drawable.rounded_shield_lock_24, AppLockTileService::class.java, listOf("ACCESSIBILITY")),
        QSTileInfo(R.string.tile_mono_audio, R.drawable.rounded_headphones_24, MonoAudioTileService::class.java, if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU")),
        QSTileInfo(R.string.tile_flashlight, R.drawable.rounded_flashlight_on_24, FlashlightTileService::class.java),
        QSTileInfo(R.string.tile_app_freezing, R.drawable.rounded_mode_cool_24, AppFreezingTileService::class.java, if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU")),
        QSTileInfo(R.string.tile_flashlight_pulse, R.drawable.outline_backlight_high_24, FlashlightPulseTileService::class.java, listOf("NOTIFICATION_LISTENER")),
        QSTileInfo(R.string.tile_stay_awake, R.drawable.rounded_av_timer_24, StayAwakeTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.nfc_tile_label, R.drawable.rounded_nfc_24, NfcTileService::class.java, if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU")),
        QSTileInfo(R.string.tile_adaptive_brightness, R.drawable.rounded_brightness_auto_24, AdaptiveBrightnessTileService::class.java, listOf("WRITE_SETTINGS")),
        QSTileInfo(R.string.feat_maps_power_saving_title, R.drawable.rounded_navigation_24, MapsPowerSavingTileService::class.java, if (ShellUtils.isRootEnabled(context)) listOf("ROOT", "NOTIFICATION_LISTENER") else listOf("SHIZUKU", "NOTIFICATION_LISTENER")),
        QSTileInfo(R.string.tile_private_dns, R.drawable.rounded_dns_24, PrivateDnsTileService::class.java, listOf("WRITE_SECURE_SETTINGS")),
        QSTileInfo(R.string.tile_usb_debugging, R.drawable.rounded_adb_24, UsbDebuggingTileService::class.java, listOf("WRITE_SECURE_SETTINGS"))
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTiles.forEach { tile ->
                    // Map permission keys to actual granted state
                    val allPermissionsGranted = tile.permissionKeys.all { key ->
                        PermissionUIHelper.getPermissionItem(key, context, viewModel)?.isGranted == true
                    }

                    QSTileCard(
                        tile = tile,
                        modifier = Modifier
                            .weight(1f)
                            .highlight(highlightSetting.equals(context.getString(tile.titleRes), ignoreCase = true)),
                        isMissingPermissions = !allPermissionsGranted,
                        onClick = {
                            if (!allPermissionsGranted) {
                                selectedTileForPermissions = tile
                                showPermissionSheet = true
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                    val componentName = ComponentName(context, tile.serviceClass)
                                    
                                    statusBarManager.requestAddTileService(
                                        componentName,
                                        context.getString(tile.titleRes),
                                        Icon.createWithResource(context, tile.iconRes),
                                        context.mainExecutor
                                    ) { result ->
                                         if(result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED){
                                             Toast.makeText(context, context.getString(R.string.qs_tile_already_added), Toast.LENGTH_SHORT).show()
                                         }
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.qs_tile_requires_android_13), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                // Determine if we need a spacer for the last odd item
                if (rowTiles.size < 2) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QSTileCard(
    tile: QSTileInfo,
    isMissingPermissions: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isMissingPermissions) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary)
            .clickable {
                com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = tile.iconRes),
            contentDescription = null,
            tint = if (isMissingPermissions) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(8.dp)
        )
        
        Column {
            Text(
                text = stringResource(tile.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = if (isMissingPermissions) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = if (isMissingPermissions) "Grant permission" else stringResource(R.string.action_add),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMissingPermissions) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
