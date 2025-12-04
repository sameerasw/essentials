package com.sameerasw.essentials.ui.composables

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.MainViewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.PermissionRegistry
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val previewMainViewModel = MainViewModel()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenOffWidgetSetup(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    searchRequested: Boolean = false,
    onSearchHandled: () -> Unit = {}
) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWidgetEnabled by viewModel.isWidgetEnabled
    val context = LocalContext.current

    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(showSheet, isAccessibilityEnabled) {
        if (showSheet) {
            val missing = mutableListOf<PermissionItem>()
            if (!isAccessibilityEnabled) {
                missing.add(
                    PermissionItem(
                        iconRes = R.drawable.rounded_settings_accessibility_24,
                        title = "Accessibility",
                        description = "Required to perform screen off actions via widget",
                        dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                        actionLabel = "Open Accessibility Settings",
                        action = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        isGranted = isAccessibilityEnabled
                    )
                )
            }

            if (missing.isEmpty()) {
                showSheet = false
            }
        }
    }

    if (showSheet) {
        val permissionItems = listOf(
            PermissionItem(
                iconRes = R.drawable.rounded_settings_accessibility_24,
                title = "Accessibility",
                description = "Required to perform screen off actions via widget",
                dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                actionLabel = "Open Accessibility Settings",
                action = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                isGranted = isAccessibilityEnabled
            )
        )

        PermissionsBottomSheet(
            onDismissRequest = { showSheet = false },
            featureTitle = "Screen off widget",
            permissions = permissionItems
        )
    }

    val scrollState = rememberScrollState()
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val allFeatures = remember {
        mutableStateListOf(
            FeatureItem("Screen off widget", R.drawable.rounded_power_settings_new_24),
//            FeatureItem("Feature A", R.drawable.rounded_power_settings_new_24),
//            FeatureItem("Feature B", R.drawable.rounded_power_settings_new_24),
//            FeatureItem("Feature C", R.drawable.rounded_power_settings_new_24)
        )
    }

    var filtered by remember { mutableStateOf(allFeatures.toList()) }
    var isLoading by remember { mutableStateOf(false) }
    var debounceJob: Job? by remember { mutableStateOf(null) }

    LaunchedEffect(searchRequested) {
        if (searchRequested) {
            scrollState.animateScrollTo(0)
            delay(100)
            focusRequester.requestFocus()
            onSearchHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { new ->
                query = new
                debounceJob?.cancel()
                isLoading = true
                debounceJob = kotlinx.coroutines.GlobalScope.launch {
                    delay(250)
                    val q = new.trim().lowercase()
                    filtered = if (q.isEmpty()) allFeatures.toList() else allFeatures.filter { it.title.lowercase().contains(q) }
                    isLoading = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            leadingIcon = { Icon(painter = painterResource(id = R.drawable.rounded_search_24), contentDescription = "Search", modifier = Modifier.size(24.dp)) },
            placeholder = { if (!isFocused && query.isEmpty()) Text("Search for Tools, Mods and Tweaks") },
            shape = RoundedCornerShape(64.dp),
            singleLine = true
        )

        // Loading indicator while filtering
        if (isLoading) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator()
            }
        }

        // Render filtered features
        for (feature in filtered) {
            FeatureCard(
                title = feature.title,
                isEnabled = feature.title == "Screen off widget" && isWidgetEnabled,
                onToggle = { enabled -> if (feature.title == "Screen off widget") {
                    viewModel.setWidgetEnabled(enabled, context)
                } },
                onClick = {
                    if (feature.title == "Screen off widget") {
                        context.startActivity(Intent(context, FeatureSettingsActivity::class.java).apply { putExtra("feature", "Screen off widget") })
                    } else {
                        context.startActivity(Intent(context, FeatureSettingsActivity::class.java).apply { putExtra("feature", feature.title) })
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                isToggleEnabled = isAccessibilityEnabled,
                onDisabledToggleClick = { showSheet = true }
            )
        }
    }
}

private data class FeatureItem(val title: String, val iconRes: Int)

@Preview(showBackground = true)
@Composable
fun ScreenOffWidgetSetupPreview() {
    EssentialsTheme {
        val mockViewModel = previewMainViewModel.apply {
            isAccessibilityEnabled.value = false
        }
        ScreenOffWidgetSetup(viewModel = mockViewModel)
    }
}
