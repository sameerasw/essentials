package com.sameerasw.essentials.ui.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.PermissionRegistry
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
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
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isWidgetEnabled by viewModel.isWidgetEnabled
    val isStatusBarIconControlEnabled by viewModel.isStatusBarIconControlEnabled
    val isCaffeinateActive by viewModel.isCaffeinateActive
    val context = LocalContext.current

    var showSheet by remember { mutableStateOf(false) }
    var currentFeature by remember { mutableStateOf<String?>(null) }

    // Periodic check for Caffeinate status
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.checkCaffeinateActive(context)
            kotlinx.coroutines.delay(2000) // Check every second
        }
    }

    LaunchedEffect(showSheet, isAccessibilityEnabled, isWriteSecureSettingsEnabled, currentFeature) {
        if (showSheet && currentFeature != null) {
            val missing = mutableListOf<PermissionItem>()
            when (currentFeature) {
                "Screen off widget" -> {
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
                }
                "Statusbar icons" -> {
                    if (!isWriteSecureSettingsEnabled) {
                        missing.add(
                            PermissionItem(
                                iconRes = R.drawable.rounded_security_24,
                                title = "Write Secure Settings",
                                description = "Required to change status bar icon visibility",
                                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                                actionLabel = "Copy ADB",
                                action = {
                                    val adbCommand =
                                        "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                                    clipboard.setPrimaryClip(clip)
                                },
                                secondaryActionLabel = "Check",
                                secondaryAction = {
                                    viewModel.isWriteSecureSettingsEnabled.value =
                                        viewModel.canWriteSecureSettings(context)
                                },
                                isGranted = isWriteSecureSettingsEnabled
                            )
                        )
                    }
                }
            }

            if (missing.isEmpty()) {
                showSheet = false
            }
        }
    }

    if (showSheet && currentFeature != null) {
        val permissionItems = when (currentFeature) {
            "Screen off widget" -> listOf(
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
            "Statusbar icons" -> listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_chevron_right_24,
                    title = "Write Secure Settings",
                    description = "Required to change status bar icon visibility",
                    dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                    actionLabel = "Copy ADB",
                    action = {
                        val adbCommand =
                            "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("adb_command", adbCommand)
                        clipboard.setPrimaryClip(clip)
                    },
                    secondaryActionLabel = "Check",
                    secondaryAction = {
                        viewModel.isWriteSecureSettingsEnabled.value =
                            viewModel.canWriteSecureSettings(context)
                    },
                    isGranted = isWriteSecureSettingsEnabled
                )
            )
            else -> emptyList()
        }

        PermissionsBottomSheet(
            onDismissRequest = { showSheet = false },
            featureTitle = currentFeature ?: "",
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
            FeatureItem("Screen off widget", R.drawable.rounded_settings_power_24, "Tools", "Invisible widget to turn the screen off"),
            FeatureItem("Statusbar icons", R.drawable.rounded_interests_24, "Visuals", "Control the visibility of statusbar icons"),
            FeatureItem("Caffeinate", R.drawable.rounded_coffee_24, "Tools", "Keep the screen awake")
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

        // Render filtered features grouped by category
        val categories = filtered.map { it.category }.distinct()
        for (category in categories) {
            val categoryFeatures = filtered.filter { it.category == category }

            // Show category header if there are features in this category
            if (categoryFeatures.isNotEmpty()) {
                Text(
                    text = category,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RoundedCardContainer(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                for (feature in categoryFeatures) {
                    val isEnabled = when (feature.title) {
                        "Screen off widget" -> isWidgetEnabled
                        "Statusbar icons" -> isStatusBarIconControlEnabled
                        "Caffeinate" -> isCaffeinateActive
                        else -> false
                    }

                    val isToggleEnabled = when (feature.title) {
                        "Screen off widget" -> isAccessibilityEnabled
                        "Statusbar icons" -> isWriteSecureSettingsEnabled
                        "Caffeinate" -> true
                        else -> false
                    }

                    FeatureCard(
                        title = feature.title,
                        isEnabled = isEnabled,
                        onToggle = { enabled ->
                            when (feature.title) {
                                "Screen off widget" -> viewModel.setWidgetEnabled(enabled, context)
                                "Statusbar icons" -> viewModel.setStatusBarIconControlEnabled(
                                    enabled,
                                    context
                                )

                                "Caffeinate" -> if (enabled) viewModel.startCaffeinate(context) else viewModel.stopCaffeinate(
                                    context
                                )
                            }
                        },
                        onClick = {
                            context.startActivity(
                                Intent(context, FeatureSettingsActivity::class.java).apply {
                                    putExtra("feature", feature.title)
                                }
                            )
                        },
                        iconRes = feature.iconRes,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
                        isToggleEnabled = isToggleEnabled,
                        onDisabledToggleClick = {
                            currentFeature = feature.title
                            showSheet = true
                        },
                        description = feature.description
                    )
                }
            }
        }
    }
}

private data class FeatureItem(val title: String, val iconRes: Int, val category: String, val description: String)

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
