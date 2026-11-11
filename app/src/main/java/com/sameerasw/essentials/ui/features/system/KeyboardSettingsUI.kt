/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: KeyboardSettingsUI.kt
 * Description: UI component and settings composable for System feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KeyboardSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var text by remember { mutableStateOf("") }
    val isKeyboardEnabled by viewModel.isKeyboardEnabled
    val isKeyboardSelected by viewModel.isKeyboardSelected
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    var showPermissionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.check(context)
            delay(2000)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Keyboard Setup Section
        if (!isKeyboardEnabled || !isKeyboardSelected) {
            TranslatableText(
                stringResId = R.string.label_keyboard_setup,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isKeyboardEnabled) {
                    Button(
                        onClick = { viewModel.openImeSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_settings_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        TranslatableText(R.string.btn_enable_keyboard)
                    }
                } else if (!isKeyboardSelected) {
                    Button(
                        onClick = { viewModel.showImePicker(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_keyboard_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        TranslatableText(R.string.btn_select_keyboard)
                    }
                }
            }
        }

        // Test Field
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { TranslatableText(R.string.test_keyboard_hint) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
        )

        // Customization
        TranslatableText(
            stringResId = R.string.feat_system_keys,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(spacing = 2.dp) {
            ConfigSliderItem(
                title = stringResource(R.string.label_keyboard_height),
                value = viewModel.keyboardHeight.floatValue,
                onValueChange = {
                    viewModel.setKeyboardHeight(it, context)
                    com.sameerasw.essentials.utils.HapticUtil
                        .performSliderHaptic(view)
                },
                valueRange = 200f..600f,
                steps = 0,
                modifier = Modifier.highlight(highlightSetting == "keyboard_height"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_keyboard_bottom_padding),
                value = viewModel.keyboardBottomPadding.floatValue,
                onValueChange = {
                    viewModel.setKeyboardBottomPadding(it, context)
                    com.sameerasw.essentials.utils.HapticUtil
                        .performSliderHaptic(view)
                },
                valueRange = 0f..100f,
                steps = 0,
                modifier = Modifier.highlight(highlightSetting == "keyboard_bottom_padding"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_keyboard_roundness),
                value = viewModel.keyboardRoundness.floatValue,
                onValueChange = {
                    viewModel.setKeyboardRoundness(it, context)
                    com.sameerasw.essentials.utils.HapticUtil
                        .performSliderHaptic(view)
                },
                valueRange = 4f..30f,
                steps = 0,
                modifier = Modifier.highlight(highlightSetting == "keyboard_roundness"),
            )
        }

        RoundedCardContainer(spacing = 2.dp) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TranslatableText(
                    stringResId = R.string.label_keyboard_shape,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(24.dp),
                            ).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val shapes =
                        listOf(
                            R.string.shape_round to 0,
                            R.string.shape_flat to 1,
                            R.string.shape_inverse to 2,
                        )

                    shapes.forEach { (labelRes, value) ->
                        val isSelected = viewModel.keyboardShape.intValue == value
                        androidx.compose.material3.ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { viewModel.setKeyboardShape(value, context) },
                            modifier = Modifier.weight(1f),
                            shapes =
                                when (value) {
                                    0 ->
                                        androidx.compose.material3.ButtonGroupDefaults
                                            .connectedLeadingButtonShapes()
                                    2 ->
                                        androidx.compose.material3.ButtonGroupDefaults
                                            .connectedTrailingButtonShapes()
                                    else ->
                                        androidx.compose.material3.ButtonGroupDefaults
                                            .connectedMiddleButtonShapes()
                                },
                        ) {
                            TranslatableText(labelRes)
                        }
                    }
                }
            }
        }

        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_keyboard_arrow_down_24,
                title = stringResource(R.string.label_keyboard_functions_bottom),
                isChecked = viewModel.isKeyboardFunctionsBottom.value,
                onCheckedChange = { viewModel.setKeyboardFunctionsBottom(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_functions_bottom"),
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_keyboard_functions_padding),
                value = viewModel.keyboardFunctionsPadding.floatValue,
                onValueChange = {
                    viewModel.setKeyboardFunctionsPadding(it, context)
                    com.sameerasw.essentials.utils.HapticUtil
                        .performSliderHaptic(view)
                },
                valueRange = 0f..100f,
                steps = 0,
                modifier = Modifier.highlight(highlightSetting == "keyboard_functions_padding"),
            )
        }

        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_vibrate_24,
                title = stringResource(R.string.label_keyboard_haptics),
                isChecked = viewModel.isKeyboardHapticsEnabled.value,
                onCheckedChange = { viewModel.setKeyboardHapticsEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_haptics"),
            )

            if (viewModel.isKeyboardHapticsEnabled.value) {
                ConfigSliderItem(
                    title = stringResource(R.string.label_keyboard_haptic_strength),
                    value = viewModel.keyboardHapticStrength.floatValue,
                    onValueChange = {
                        viewModel.setKeyboardHapticStrength(it, context)
                    },
                    modifier = Modifier.highlight(highlightSetting == "keyboard_haptic_strength"),
                )
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_nightlight_24,
                title = stringResource(R.string.label_keyboard_always_dark),
                isChecked = viewModel.isKeyboardAlwaysDark.value,
                onCheckedChange = { viewModel.setKeyboardAlwaysDark(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_always_dark"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_invert_colors_24,
                title = stringResource(R.string.label_keyboard_pitch_black),
                isChecked = viewModel.isKeyboardPitchBlack.value,
                onCheckedChange = { viewModel.setKeyboardPitchBlack(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_pitch_black"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_content_paste_24,
                title = stringResource(R.string.label_keyboard_clipboard_enabled),
                isChecked = viewModel.isKeyboardClipboardEnabled.value,
                onCheckedChange = { viewModel.setKeyboardClipboardEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_clipboard_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_keyboard_24,
                title = stringResource(R.string.label_keyboard_long_press_symbols),
                isChecked = viewModel.isLongPressSymbolsEnabled.value,
                onCheckedChange = { viewModel.setLongPressSymbolsEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "keyboard_long_press_symbols"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_keyboard_24,
                title = stringResource(R.string.label_keyboard_accented_characters),
                isChecked = viewModel.isAccentedCharactersEnabled.value,
                onCheckedChange = { viewModel.setAccentedCharactersEnabled(it, context) },
                enabled = viewModel.isLongPressSymbolsEnabled.value,
                modifier = Modifier.highlight(highlightSetting == "keyboard_accented_characters"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_book_2_24,
                title = stringResource(R.string.keyboard_user_dictionary_title),
                isChecked = viewModel.isUserDictionaryEnabled.value,
                onCheckedChange = { viewModel.setUserDictionaryEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "user_dictionary_enabled"),
            )

            if (viewModel.isUserDictionaryEnabled.value) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_settings_24,
                    title = stringResource(R.string.keyboard_manage_learned_words),
                    isChecked = false,
                    showToggle = false,
                    onCheckedChange = { viewModel.isUserDictionarySheetVisible.value = true },
                )
            }
        }

        // Text Snippets Container
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_edit_note_24,
                title = stringResource(R.string.feat_text_snippets_title),
                isChecked = viewModel.isTextSnippetsEnabled.value,
                onCheckedChange = { checked ->
                    if (checked && !isAccessibilityEnabled) {
                        showPermissionSheet = true
                    } else {
                        viewModel.setTextSnippetsEnabled(checked, context)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "keyboard_text_snippets"),
            )

            if (viewModel.isTextSnippetsEnabled.value) {
                com.sameerasw.essentials.ui.core.cards.SelectionCardItem(
                    title = stringResource(R.string.title_manage_snippets),
                    description =
                        stringResource(
                            R.string.desc_snippets_summary,
                            viewModel.snippetPrefix.value.ifEmpty { "␣" },
                            viewModel.textSnippets.value.size,
                        ),
                    iconRes = R.drawable.rounded_settings_24,
                    onClick = {
                        val intent =
                            Intent(context, com.sameerasw.essentials.FeatureSettingsActivity::class.java).apply {
                                putExtra("feature", "Text Snippets")
                            }
                        context.startActivity(intent)
                    },
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (showPermissionSheet) {
            val permissionItems =
                com.sameerasw.essentials.utils.PermissionUIHelper.getPermissionItems(
                    listOf("ACCESSIBILITY"),
                    context,
                    viewModel,
                    context as? android.app.Activity,
                )
            if (permissionItems.isNotEmpty()) {
                com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet(
                    onDismissRequest = { showPermissionSheet = false },
                    featureTitle = stringResource(R.string.feat_text_snippets_title),
                    permissions = permissionItems,
                )
            }
        }

        if (viewModel.isUserDictionarySheetVisible.value) {
            com.sameerasw.essentials.ui.features.apps.sheets.UserDictionaryBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { viewModel.isUserDictionarySheetVisible.value = false },
            )
        }
    }
}
