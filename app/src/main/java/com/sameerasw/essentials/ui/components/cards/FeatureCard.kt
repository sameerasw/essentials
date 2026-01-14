package com.sameerasw.essentials.ui.components.cards

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun FeatureCard(
    title: Any, // Can be Int (Resource ID) or String
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    hasMoreSettings: Boolean = true,
    isToggleEnabled: Boolean = true,
    showToggle: Boolean = true,
    onDisabledToggleClick: (() -> Unit)? = null,
    description: Any? = null, // Can be Int or String
    descriptionOverride: String? = null, // For cases where we search and prepend parent feature name
    isBeta: Boolean = false
) {
    val view = LocalView.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier.clickable {
            HapticUtil.performVirtualKeyHaptic(view)
            onClick()
        }) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            val resolvedTitle = when (title) {
                is Int -> stringResource(id = title)
                is String -> title
                else -> ""
            }

            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (iconRes != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = com.sameerasw.essentials.utils.ColorUtil.getPastelColorFor(resolvedTitle),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = resolvedTitle,
                            modifier = Modifier.size(24.dp), // Slightly smaller to fit nicely in the circle
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = resolvedTitle)
                        if (isBeta) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = stringResource(R.string.label_beta),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (descriptionOverride != null) {
                        Text(
                            text = descriptionOverride,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (description != null) {
                        val resolvedDescription = when (description) {
                            is Int -> stringResource(id = description)
                            is String -> description
                            else -> ""
                        }
                        Text(
                            text = resolvedDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (hasMoreSettings) {
                    Icon(
                        modifier = Modifier.padding(end = 12.dp).size(24.dp),
                        painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                        contentDescription = "More settings"
                    )
                }

                if (showToggle) {
                    Box {
                        Switch(
                            checked = if (isToggleEnabled) isEnabled else false,
                            onCheckedChange = { checked ->
                                if (isToggleEnabled) {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onToggle(checked)
                                }
                            },
                            enabled = isToggleEnabled
                        )

                        if (!isToggleEnabled && onDisabledToggleClick != null) {
                            // Invisible overlay catches taps even if the child consumes them
                            Box(modifier = Modifier.matchParentSize().clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onDisabledToggleClick()
                            })
                        }
                    }
                }
            }
        }
    }
}
