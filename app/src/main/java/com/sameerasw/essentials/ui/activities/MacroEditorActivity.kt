/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: MacroEditorActivity.kt
 * Description: Activity component for MacroEditorActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.domain.diy.ActionRegistry
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Macro
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.features.system.ActionSequenceEditor
import com.sameerasw.essentials.ui.navigation.AppRoute
import com.sameerasw.essentials.ui.navigation.AppRoutes
import com.sameerasw.essentials.ui.navigation.BottomBarSpec
import com.sameerasw.essentials.ui.navigation.ProvideBottomBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.util.UUID

@Composable
fun MacroEditorScreen(
    route: AppRoute.MacroEditor,
    onBack: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    remember(appContext) { DIYRepository.init(appContext) }

    val existing = remember(route.macroId) { route.macroId?.let { DIYRepository.getMacro(it) } }
    val isEditMode = existing != null

    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
    EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
        var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
        var actions by rememberSaveable(
            stateSaver =
                Saver(
                    save = { ActionGsonAdapter.listToJson(it) },
                    restore = { ActionGsonAdapter.listFromJson(it) },
                ),
        ) {
            mutableStateOf(existing?.actionList.orEmpty())
        }
        var showDiscardDialog by remember { mutableStateOf(false) }

        val hasChanges = name != existing?.name.orEmpty() || actions != existing?.actionList.orEmpty()
        val handleBackClick = {
            if (hasChanges) {
                showDiscardDialog = true
            } else {
                onBack()
            }
        }

        BackHandler {
            handleBackClick()
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.diy_discard_warning_title)) },
                text = { Text(stringResource(R.string.diy_discard_warning_desc)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showDiscardDialog = false
                            onBack()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.translation_discard),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showDiscardDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        val isValid = name.isNotBlank() && actions.isNotEmpty()

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                            ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RoundedCardContainer {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.diy_macro_name)) },
                            singleLine = true,
                        )
                    }

                    ActionSequenceEditor(
                        viewModel = viewModel,
                        actions = actions,
                        onActionsChange = { actions = it },
                        screenOnOnly = false,
                        listKey = "macro",
                        emptyText = stringResource(R.string.diy_no_actions),
                        categories = remember { ActionRegistry.getCategories() },
                        showMacros = false,
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp,
                            ),
                    )
                }

                ProvideBottomBar(
                    route = route,
                    spec =
                        BottomBarSpec(
                            title =
                                stringResource(
                                    if (isEditMode) R.string.diy_edit_macro else R.string.diy_new_macro,
                                ),
                            onBack = handleBackClick,
                            fabAction =
                                if (isValid) {
                                    {
                                        val macro =
                                            Macro(
                                                id = existing?.id ?: UUID.randomUUID().toString(),
                                                name = name.trim(),
                                                actions = actions,
                                            )
                                        if (isEditMode) {
                                            DIYRepository.updateMacro(macro)
                                        } else {
                                            DIYRepository.addMacro(macro)
                                        }
                                        onBack()
                                    }
                                } else {
                                    null
                                },
                            fabIconRes = R.drawable.rounded_check_24,
                            fabContentDescription = stringResource(R.string.action_save),
                        ),
                )
            }
        }
    }
}

class MacroEditorActivity : Activity() {
    companion object {
        const val EXTRA_MACRO_ID = "macro_id"

        fun createIntent(
            context: Context,
            macroId: String?,
        ): Intent =
            Intent(context, MacroEditorActivity::class.java).apply {
                putExtra(EXTRA_MACRO_ID, macroId)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRoutes.redirectToMain(this)
    }
}
