/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeEditorHeader.kt
 * Description: Shows a mode as it looks, with an edit toggle that reveals its identity settings.
 */

package com.sameerasw.essentials.ui.features.modes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun ModeEditorHeader(
    icon: Painter,
    color: Color,
    name: String,
    subtitle: String,
    isEditing: Boolean,
    onEditToggle: () -> Unit,
    onOpenSystemSettings: (() -> Unit)?,
    editContent: @Composable () -> Unit,
) {
    val view = LocalView.current
    val circleColor by animateColorAsState(color, label = "mode_header_color")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(104.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                        .background(circleColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }
            FilledIconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onEditToggle()
                },
                modifier =
                    Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp),
            ) {
                AnimatedContent(
                    targetState = isEditing,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f)) },
                    label = "mode_edit_icon",
                ) { editing ->
                    Icon(
                        painter = painterResource(if (editing) R.drawable.rounded_check_24 else R.drawable.rounded_edit_24),
                        contentDescription = stringResource(R.string.mode_editor_edit_title),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isEditing,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.large)
                            .then(
                                if (onOpenSystemSettings != null) {
                                    Modifier.clickable {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        onOpenSystemSettings()
                                    }
                                } else {
                                    Modifier
                                },
                            ).padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee(),
                    )
                    if (onOpenSystemSettings != null) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_chevron_right_24),
                            contentDescription = stringResource(R.string.mode_system_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { editContent() }
        }
    }
}
