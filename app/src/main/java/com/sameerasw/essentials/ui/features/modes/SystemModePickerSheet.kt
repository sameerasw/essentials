/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: SystemModePickerSheet.kt
 * Description: Picks an existing system mode for an Essentials mode to link to.
 */

package com.sameerasw.essentials.ui.features.modes

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.painter.Painter
import com.sameerasw.essentials.domain.modes.SystemMode
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LinkableSystemModes(
    val isLoading: Boolean = true,
    val modes: List<SystemMode>? = null,
)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
internal fun rememberLinkableSystemModes(
    excludedIds: Set<String>,
    refreshKey: Any? = Unit,
): LinkableSystemModes {
    val context = LocalContext.current
    val state by produceState(LinkableSystemModes(), excludedIds, refreshKey) {
        value = LinkableSystemModes(isLoading = true, modes = value.modes)
        val modes =
            withContext(Dispatchers.IO) {
                ModesController.systemModes(context.applicationContext)?.filterNot { it.id in excludedIds }
            }
        value = LinkableSystemModes(isLoading = false, modes = modes)
    }
    return state
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SystemModePickerSheet(
    selectedId: String?,
    excludedIds: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (SystemMode) -> Unit,
) {
    val linkable = rememberLinkableSystemModes(excludedIds)
    val isLoading = linkable.isLoading
    val systemModes = linkable.modes

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.mode_link_title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer(
                spacing = 2.dp,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                systemModes?.forEach { mode ->
                    PickerRow(
                        icon = rememberSystemModeIconPainter(mode),
                        title = mode.name,
                        subtitle = if (mode.isEnabled) null else stringResource(R.string.diy_set_mode_disabled),
                        selected = selectedId == mode.id,
                        onClick = { onSelect(mode) },
                    )
                }
            }

            when {
                isLoading ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }

                systemModes.isNullOrEmpty() ->
                    TranslatableText(
                        stringResId = R.string.diy_set_mode_empty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
            }
        }
    }
}

@Composable
private fun PickerRow(
    icon: Painter,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = {
                HapticUtil.performUIHaptic(view)
                onClick()
            },
        )
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
