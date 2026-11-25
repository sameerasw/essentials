/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeEditorActivity.kt
 * Description: Creates and edits Essentials modes, and opens from the system mode settings page.
 */

package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.modes.Mode
import com.sameerasw.essentials.domain.modes.ModeIcons
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.FreezeTagColorPicker
import com.sameerasw.essentials.ui.components.FreezeTagIconPicker
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PRESET_PASTEL_COLORS
import com.sameerasw.essentials.ui.navigation.AppRoute
import com.sameerasw.essentials.ui.navigation.AppRoutes
import com.sameerasw.essentials.ui.navigation.BottomBarSpec
import com.sameerasw.essentials.ui.navigation.ProvideBottomBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ModeEditorScreen(
    route: AppRoute.ModeEditor,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()

    val existing = remember(route.modeId) {
        ModesRepository.init(appContext)
        route.modeId?.let(ModesRepository::getMode)
    }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: PRESET_PASTEL_COLORS.random()) }
    var iconName by remember { mutableStateOf(existing?.iconName ?: ModeIcons.DEFAULT) }

    val isValid = name.isNotBlank()

    fun save() {
        val mode = (existing ?: Mode()).copy(name = name.trim(), colorHex = colorHex, iconName = iconName)
        ModesRepository.upsert(mode)
        scope.launch(Dispatchers.IO) { ModesController.sync(appContext) }
        onBack()
    }

    fun delete(mode: Mode) {
        ModesRepository.remove(mode.id)
        scope.launch(Dispatchers.IO) { ModesController.sync(appContext) }
        onBack()
    }

    val previewColor by animateColorAsState(
        targetValue = runCatching { Color(colorHex.toColorInt()) }.getOrDefault(MaterialTheme.colorScheme.primaryContainer),
        label = "mode_preview_color",
    )
    val previewSize by animateDpAsState(if (isValid) 96.dp else 80.dp, label = "mode_preview_size")

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(previewSize)
                        .background(previewColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(ModeIcons.resolve(iconName)),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }

            RoundedCardContainer(spacing = 2.dp) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { TranslatableText(R.string.mode_name_hint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(16.dp),
                ) {
                    FreezeTagColorPicker(
                        selectedColorHex = colorHex,
                        onColorSelected = { colorHex = it },
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(16.dp),
                ) {
                    FreezeTagIconPicker(
                        selectedIconName = iconName,
                        onIconSelected = { iconName = it },
                        icons = ModeIcons.names,
                    )
                }
            }

            if (existing != null) {
                RoundedCardContainer(spacing = 2.dp) {
                    val settingsIntent = remember(existing.id) { ModesController.systemSettingsIntent(appContext, existing.id) }
                    if (settingsIntent != null) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_open_in_new_24,
                            title = stringResource(R.string.mode_system_settings),
                            showToggle = false,
                            onClick = { runCatching { context.startActivity(settingsIntent) } },
                        )
                    }
                    IconToggleItem(
                        iconRes = R.drawable.rounded_delete_24,
                        title = stringResource(R.string.action_delete),
                        showToggle = false,
                        onClick = { delete(existing) },
                    )
                }
            }
        }

        ProvideBottomBar(
            route = route,
            spec =
                BottomBarSpec(
                    title = stringResource(if (existing == null) R.string.action_new_mode else R.string.mode_editor_edit_title),
                    onBack = onBack,
                    fabAction = if (isValid) { { save() } } else null,
                    fabIconRes = R.drawable.rounded_check_24,
                    fabContentDescription = stringResource(R.string.action_save),
                ),
        )
    }
}

class ModeEditorActivity : Activity() {
    companion object {
        const val EXTRA_MODE_ID = "mode_id"

        fun createIntent(
            context: Context,
            modeId: String?,
        ): Intent =
            Intent(context, ModeEditorActivity::class.java).apply {
                modeId?.let { putExtra(EXTRA_MODE_ID, it) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The system mode settings page launches this with the rule id rather than our mode id
        if (!intent.hasExtra(EXTRA_MODE_ID)) {
            intent.getStringExtra(NotificationManager.EXTRA_AUTOMATIC_RULE_ID)
                ?.let { ModesController.modeIdForRule(this, it) }
                ?.let { intent.putExtra(EXTRA_MODE_ID, it) }
        }
        AppRoutes.redirectToMain(this)
    }
}
