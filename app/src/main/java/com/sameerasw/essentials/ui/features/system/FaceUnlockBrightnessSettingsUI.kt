/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: FaceUnlockBrightnessSettingsUI.kt
 * Description: Settings for the Face unlock brightness feature.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

private val MAX_BRIGHTNESS_OPTIONS = listOf(50, 75, 100)

@Composable
fun FaceUnlockBrightnessSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var requestingPermissionsFor by remember { mutableStateOf<List<String>?>(null) }

    requestingPermissionsFor?.let { permKeys ->
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(R.string.feat_face_unlock_brightness_title),
            permissions = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_face_24,
                title = stringResource(R.string.feat_face_unlock_brightness_title),
                isChecked = viewModel.isFaceUnlockBrightnessEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !viewModel.isAccessibilityEnabled.value) {
                        requestingPermissionsFor = listOf("ACCESSIBILITY")
                    } else {
                        viewModel.setFaceUnlockBrightnessEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "face_unlock_brightness_enabled"),
            )
        }

        AnimatedVisibility(
            visible = viewModel.isFaceUnlockBrightnessEnabled.value,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(top = 12.dp)
                            .highlight(highlightSetting == "face_unlock_max_brightness"),
                ) {
                    Text(
                        text = stringResource(R.string.flashlight_pulse_max_brightness),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedPicker(
                        items = MAX_BRIGHTNESS_OPTIONS,
                        selectedItem = viewModel.faceUnlockMaxBrightness.intValue,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setFaceUnlockMaxBrightness(it)
                        },
                        labelProvider = { "$it%" },
                        title = R.string.flashlight_pulse_max_brightness,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_lock_24,
                    title = stringResource(R.string.face_unlock_trigger_unlock_title),
                    isChecked = viewModel.isFaceUnlockTriggerUnlock.value && ShellUtils.hasPermission(context),
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (checked && !ShellUtils.hasPermission(context)) {
                            requestingPermissionsFor = listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU")
                        } else {
                            viewModel.setFaceUnlockTriggerUnlock(checked)
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_trigger_unlock"),
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_sunny_24,
                    title = stringResource(R.string.face_unlock_light_tint_title),
                    isChecked = viewModel.isFaceUnlockLightTint.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setFaceUnlockLightTint(checked)
                    },
                    modifier = Modifier.highlight(highlightSetting == "face_unlock_light_tint"),
                )
            }
        }
    }
}
