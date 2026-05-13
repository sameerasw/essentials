package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermissionCard(
    iconRes: Int,
    title: Any, // Can be Int (Resource ID) or String
    dependentFeatures: List<Any>, // List of Int or String
    actionLabel: Any = R.string.perm_action_grant, // Can be Int or String
    isGranted: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: Any? = null, // Can be Int or String
    onSecondaryActionClick: (() -> Unit)? = null
) {
    val grantedGreen = Color(0xFF4CAF50)
    val view = LocalView.current

    val resolvedTitle = when (title) {
        is Int -> stringResource(id = title)
        is String -> title
        else -> ""
    }

    val resolvedActionLabel = when (actionLabel) {
        is Int -> stringResource(id = actionLabel)
        is String -> actionLabel
        else -> ""
    }

    val resolvedSecondaryLabel = when (secondaryActionLabel) {
        is Int -> stringResource(id = secondaryActionLabel as Int)
        is String -> secondaryActionLabel
        else -> null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListItem(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                leadingContent = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (isGranted) grantedGreen else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                supportingContent = {
                    Column {
                        Text(text = "Required for:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        dependentFeatures.forEach { f ->
                            val resolvedFeature = when (f) {
                                is Int -> stringResource(id = f)
                                is String -> f
                                else -> ""
                            }
                            Text(
                                text = "• $resolvedFeature",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                colors = androidx.compose.material3.ListItemDefaults.colors(
                    containerColor = Color.Transparent
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                content = {
                    Text(text = resolvedTitle, style = MaterialTheme.typography.titleMedium)
                }
            )

            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                if (isGranted) {
                    if (resolvedSecondaryLabel != null && onSecondaryActionClick != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onActionClick()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(resolvedActionLabel)
                            }

                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onSecondaryActionClick()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(resolvedSecondaryLabel)
                            }
                        }
                    } else {
                        OutlinedButton(onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onActionClick()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(resolvedActionLabel)
                        }
                    }
                } else {
                    if (resolvedSecondaryLabel != null && onSecondaryActionClick != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onActionClick()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(resolvedActionLabel)
                            }

                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    onSecondaryActionClick()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(resolvedSecondaryLabel)
                            }
                        }
                    } else {
                        Button(onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onActionClick()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(resolvedActionLabel)
                        }
                    }
                }
            }
        }
    }
}
