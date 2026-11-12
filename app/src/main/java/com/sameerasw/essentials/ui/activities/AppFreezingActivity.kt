/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: AppFreezingActivity.kt
 * Description: Activity component for AppFreezingActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.composables.FreezeGridUI
import com.sameerasw.essentials.ui.modifiers.AppRipple
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.liquidRipple
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.state.LocalMenuStateManager
import com.sameerasw.essentials.ui.state.MenuStateManager
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

class AppFreezingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current
            val view = LocalView.current

            LaunchedEffect(Unit) {
                viewModel.check(context)
                viewModel.refreshFreezePickedApps(context)
            }

            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                CompositionLocalProvider(
                    LocalMenuStateManager provides remember { MenuStateManager() },
                ) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) { innerPadding ->
                        var searchQuery by remember { mutableStateOf("") }
                        var searchSubmitSignal by remember { mutableIntStateOf(0) }
                        var isSearchActive by remember { mutableStateOf(false) }
                        var fabCenter by remember { mutableStateOf(Offset.Zero) }
                        val focusManager = LocalFocusManager.current
                        val searchFocusRequester = remember { FocusRequester() }
                        val closeSearch = {
                            focusManager.clearFocus()
                            searchQuery = ""
                            isSearchActive = false
                        }
                        BackHandler(enabled = isSearchActive) { closeSearch() }

                        val density = LocalDensity.current
                        val statusBarHeightPx =
                            with(density) {
                                WindowInsets.statusBars
                                    .asPaddingValues()
                                    .calculateTopPadding()
                                    .toPx()
                            }

                        Box(Modifier.fillMaxSize()) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .liquidRipple(
                                            trigger = AppRipple.trigger,
                                            origin = AppRipple.origin,
                                            enabled = viewModel.isRippleEnabled.value,
                                            durationMillis = 2800,
                                            amplitudeDp = 34f,
                                            frequency = 12f,
                                            decay = 4.5f,
                                            speedDp = 1400f,
                                        ).progressiveBlur(
                                            blurRadius = if (isBlurEnabled) 40f else 0f,
                                            height = statusBarHeightPx * 1.15f,
                                            direction = BlurDirection.TOP,
                                        ).progressiveBlur(
                                            blurRadius = if (isBlurEnabled) 40f else 0f,
                                            height = with(density) { 80.dp.toPx() },
                                            direction = BlurDirection.BOTTOM,
                                        ),
                            ) {
                                FreezeGridUI(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding =
                                        PaddingValues(
                                            top =
                                                WindowInsets.statusBars
                                                    .asPaddingValues()
                                                    .calculateTopPadding(),
                                            bottom =
                                                WindowInsets.navigationBars
                                                    .asPaddingValues()
                                                    .calculateBottomPadding() + 16.dp,
                                            start = 0.dp,
                                            end = 0.dp,
                                        ),
                                    searchQuery = searchQuery,
                                    searchSubmitSignal = searchSubmitSignal,
                                    onAppLaunched = {
                                        finish()
                                    },
                                    onSettingsClick = {
                                        val intent =
                                            Intent(context, FeatureSettingsActivity::class.java).apply {
                                                putExtra("feature", "Freeze")
                                            }
                                        context.startActivity(intent)
                                    },
                                )
                            }

                            AnimatedContent(
                                targetState = isSearchActive,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                contentAlignment = Alignment.BottomEnd,
                                label = "freeze_search",
                                modifier = Modifier.align(Alignment.BottomEnd),
                            ) { active ->
                                if (active) {
                                    LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                                    Surface(
                                        modifier =
                                            Modifier
                                                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                                .fillMaxWidth(),
                                        shape = SearchBarDefaults.inputFieldShape,
                                        color = SearchBarDefaults.colors().containerColor,
                                        shadowElevation = 6.dp,
                                    ) {
                                        SearchBarDefaults.InputField(
                                            query = searchQuery,
                                            onQueryChange = { searchQuery = it },
                                            onSearch = {
                                                focusManager.clearFocus()
                                                searchSubmitSignal++
                                            },
                                            expanded = true,
                                            onExpandedChange = {},
                                            modifier = Modifier.focusRequester(searchFocusRequester),
                                            placeholder = {
                                                Text(stringResource(R.string.search_frozen_apps_placeholder), maxLines = 1)
                                            },
                                            leadingIcon = {
                                                IconButton(onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    closeSearch()
                                                }) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                                                        contentDescription = stringResource(R.string.content_desc_back),
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.rounded_close_24),
                                                            contentDescription = stringResource(R.string.action_clear),
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                } else {
                                    FloatingActionButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            AppRipple.fire(fabCenter)
                                            isSearchActive = true
                                        },
                                        modifier =
                                            Modifier
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .padding(end = 16.dp, bottom = 16.dp)
                                                .onGloballyPositioned { fabCenter = it.boundsInRoot().center },
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_search_24),
                                            contentDescription = stringResource(R.string.label_search_content_description),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
