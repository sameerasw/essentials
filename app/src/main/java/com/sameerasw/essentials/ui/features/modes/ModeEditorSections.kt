/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeEditorSections.kt
 * Description: Editor sections for what a mode changes while it is active.
 */

package com.sameerasw.essentials.ui.features.modes

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.modes.ModeFeature
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

private val FREEZE_CATEGORIES =
    listOf(
        ApplicationInfo.CATEGORY_GAME to R.drawable.rounded_videogame_asset_24,
        ApplicationInfo.CATEGORY_SOCIAL to R.drawable.rounded_groups_24,
        ApplicationInfo.CATEGORY_VIDEO to R.drawable.rounded_smart_display_24,
        ApplicationInfo.CATEGORY_AUDIO to R.drawable.rounded_music_note_24,
        ApplicationInfo.CATEGORY_NEWS to R.drawable.rounded_newspaper_24,
        ApplicationInfo.CATEGORY_IMAGE to R.drawable.rounded_image_24,
        ApplicationInfo.CATEGORY_MAPS to R.drawable.rounded_map_24,
        ApplicationInfo.CATEGORY_PRODUCTIVITY to R.drawable.rounded_work_24,
    )

@Composable
fun ModeSectionTitle(titleRes: Int) {
    TranslatableText(
        stringResId = titleRes,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp),
    )
}

@Composable
fun ModeFeaturesSection(
    features: Map<String, Boolean>,
    onFeatureChange: (ModeFeature, Boolean?) -> Unit,
) {
    ModeSectionTitle(R.string.mode_section_features)
    RoundedCardContainer(spacing = 2.dp) {
        ModeFeature.entries.forEach { feature ->
            ModeFeatureRow(
                feature = feature,
                value = features[feature.name],
                onValueChange = { onFeatureChange(feature, it) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModeFeatureRow(
    feature: ModeFeature,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
) {
    val view = LocalView.current
    IconToggleItem(
        iconRes = feature.icon,
        title = stringResource(feature.title),
        showToggle = false,
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ToggleButton(
                    checked = value == false,
                    onCheckedChange = { checked ->
                        HapticUtil.performUIHaptic(view)
                        onValueChange(if (checked) false else null)
                    },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                ) {
                    Text(stringResource(R.string.off))
                }
                ToggleButton(
                    checked = value == true,
                    onCheckedChange = { checked ->
                        HapticUtil.performUIHaptic(view)
                        onValueChange(if (checked) true else null)
                    },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Text(stringResource(R.string.on))
                }
            }
        },
    )
}

@Composable
fun ModeFreezeSection(
    tagIds: Set<String>,
    categories: Set<Int>,
    onTagChange: (String, Boolean) -> Unit,
    onCategoryChange: (Int, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val tags = remember { SettingsRepository(context).getFreezeTags() }

    ModeSectionTitle(R.string.mode_section_freeze)
    RoundedCardContainer(spacing = 2.dp) {
        tags.forEach { tag ->
            val iconRes =
                remember(tag.iconName) {
                    context.resources.getIdentifier(tag.iconName, "drawable", context.packageName)
                }
            IconToggleItem(
                iconRes = if (iconRes != 0) iconRes else R.drawable.rounded_interests_24,
                title = tag.name,
                isChecked = tag.id in tagIds,
                onCheckedChange = { onTagChange(tag.id, it) },
            )
        }
        FREEZE_CATEGORIES.forEach { (category, iconRes) ->
            val title = remember(category) { ApplicationInfo.getCategoryTitle(context, category)?.toString() }
            if (title != null) {
                IconToggleItem(
                    iconRes = iconRes,
                    title = title,
                    isChecked = category in categories,
                    onCheckedChange = { onCategoryChange(category, it) },
                )
            }
        }
    }
}
