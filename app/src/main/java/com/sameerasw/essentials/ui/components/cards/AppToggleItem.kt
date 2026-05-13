package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

private val GOOGLE_SYSTEM_USER_APPS = setOf(
    "com.google.android.apps.scone",
    "com.google.android.marvin.talkback",
    "com.google.android.projection.gearhead",
    "com.google.android.as",
    "com.google.android.contactkeys",
    "com.google.android.safetycore",
    "com.google.android.webview",
    "com.google.android.captiveportallogin",
    "com.google.ambient.streaming",
    "com.google.android.apps.pixel.dcservice",
    "com.google.android.apps.turbo",
    "com.google.android.apps.work.clouddpc",
    "com.google.android.apps.diagnosticstool",
    "com.google.android.apps.wellbeing",
    "com.google.android.documentsui",
    "com.google.android.odad",
    "com.google.android.gms",
    "com.google.ar.core",
    "com.google.vending",
    "com.google.android.apps.carrier.carrierwifi",
    "com.google.android.modulemetadata",
    "com.google.android.networkstack",
    "com.google.android.apps.safetyhub",
    "com.google.intelligence.sense",
    "com.google.android.apps.camera.services",
    "com.google.android.apps.nexuslauncher",
    "com.google.android.apps.pixel.support",
    "com.google.android.as.oss",
    "com.android.settings",
    "com.google.android.settings.intelligence",
    "com.android.stk",
    "com.google.android.soundpicker",
    "com.google.mainline.telemetry",
    "com.google.android.apps.accessibility.voiceaccess",
    "com.google.android.cellbroadcastreceiver"
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppToggleItem(
    icon: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    packageName: String? = null,
    isSystemApp: Boolean = false,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    showToggle: Boolean = true
) {
    val view = LocalView.current
    val shouldShowSystemTag = remember(packageName, isSystemApp) {
        isSystemApp || (packageName != null && GOOGLE_SYSTEM_USER_APPS.contains(packageName))
    }

    val onClickAction = {
        if (enabled) {
            HapticUtil.performVirtualKeyHaptic(view)
            onCheckedChange(!isChecked)
        } else if (onDisabledClick != null) {
            HapticUtil.performVirtualKeyHaptic(view)
            onDisabledClick()
        }
    }

    if (showToggle) {
        androidx.compose.material3.ListItem(
            checked = isChecked && enabled,
            onCheckedChange = { checked ->
                if (enabled) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onCheckedChange(checked)
                } else if (onDisabledClick != null) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDisabledClick()
                }
            },
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            },
            supportingContent = if (description != null) {
                {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            trailingContent = {
                Switch(
                    checked = if (enabled) isChecked else false,
                    onCheckedChange = null, // Handled by ListItem
                    enabled = enabled
                )
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (shouldShowSystemTag) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.round_android_24),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.surfaceBright
                            )
                        }
                    }
                }
            }
        )
    } else {
        androidx.compose.material3.ListItem(
            onClick = onClickAction,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            },
            supportingContent = if (description != null) {
                {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (shouldShowSystemTag) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.round_android_24),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.surfaceBright
                            )
                        }
                    }
                }
            }
        )
    }
}
