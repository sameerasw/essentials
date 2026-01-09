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
import com.sameerasw.essentials.ui.modifiers.highlight

data class QSTileInfo(
    val title: String,
    val iconRes: Int,
    val serviceClass: Class<*>
)

@Composable
fun QuickSettingsTilesSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    LocalView.current

    val tiles = listOf(
        QSTileInfo("UI Blur", R.drawable.rounded_blur_on_24, UiBlurTileService::class.java),
        QSTileInfo("Bubbles", R.drawable.rounded_bubble_24, BubblesTileService::class.java),
        QSTileInfo("Sensitive Content", R.drawable.rounded_notifications_off_24, PrivateNotificationsTileService::class.java),
        QSTileInfo("Tap to Wake", R.drawable.rounded_touch_app_24, TapToWakeTileService::class.java),
        QSTileInfo("AOD", R.drawable.rounded_mobile_text_2_24, AlwaysOnDisplayTileService::class.java),
        QSTileInfo("Caffeinate", R.drawable.rounded_coffee_24, CaffeinateTileService::class.java),
        QSTileInfo("Sound Mode", R.drawable.rounded_volume_up_24, SoundModeTileService::class.java),
        QSTileInfo("Notification Lighting", R.drawable.rounded_blur_linear_24, NotificationLightingTileService::class.java),
        QSTileInfo("Dynamic Night Light", R.drawable.rounded_nightlight_24, DynamicNightLightTileService::class.java),
        QSTileInfo("Locked Security", R.drawable.rounded_security_24, ScreenLockedSecurityTileService::class.java),
        QSTileInfo("App Lock", R.drawable.rounded_shield_lock_24, AppLockTileService::class.java),
        QSTileInfo("Mono Audio", R.drawable.rounded_headphones_24, MonoAudioTileService::class.java),
        QSTileInfo("Flashlight", R.drawable.rounded_flashlight_on_24, FlashlightTileService::class.java),
        QSTileInfo("App Freezing", R.drawable.rounded_mode_cool_24, AppFreezingTileService::class.java),
        QSTileInfo("Flashlight Pulse", R.drawable.outline_backlight_high_24, FlashlightPulseTileService::class.java)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.chunked(2).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTiles.forEach { tile ->
                    QSTileCard(
                        tile = tile,
                        modifier = Modifier
                            .weight(1f)
                            .highlight(highlightSetting.equals(tile.title, ignoreCase = true)),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                val componentName = ComponentName(context, tile.serviceClass)
                                
                                statusBarManager.requestAddTileService(
                                    componentName,
                                    tile.title,
                                    Icon.createWithResource(context, tile.iconRes),
                                    context.mainExecutor
                                ) { result ->
                                     if(result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED){
                                         Toast.makeText(context, "Already added", Toast.LENGTH_SHORT).show()
                                     }
                                }
                            } else {
                                Toast.makeText(context, "Requires Android 13+", Toast.LENGTH_SHORT).show()
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
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
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(8.dp)
        )
        
        Column {
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "Add",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
