package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.LocationAlarm

@Composable
fun LocationAlarmCard(
    alarm: LocationAlarm,
    isActive: Boolean,
    isAnyTracking: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current
    
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
            .clickable {
                com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                onClick()
            },
        leadingContent = {
            val context = androidx.compose.ui.platform.LocalContext.current
            val iconResId = context.resources.getIdentifier(alarm.iconResName, "drawable", context.packageName)
            Icon(
                painter = painterResource(id = if (iconResId != 0) iconResId else R.drawable.round_navigation_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        headlineContent = {
            Text(
                text = alarm.name.ifEmpty { "Destination" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            val context = androidx.compose.ui.platform.LocalContext.current
            val lastTravelledText = alarm.lastTravelled?.let {
                stringResource(R.string.location_reached_last_travelled, com.sameerasw.essentials.utils.TimeUtil.formatRelativeDate(it, context))
            } ?: stringResource(R.string.location_reached_never)
            
            Text(
                text = lastTravelledText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isActive) {
                IconButton(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                        onStop()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_close_24),
                        contentDescription = "Stop"
                    )
                }
            } else if (!isAnyTracking) {
                IconButton(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                        onStart()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_play_arrow_24),
                        contentDescription = "Start"
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
