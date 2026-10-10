/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: WidgetStackConfigureActivity.kt
 * Description: Chooses which widgets a widget stack holds and how it switches between them.
 */

package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.services.widgets.StackHost
import com.sameerasw.essentials.services.widgets.StackWidgetProvider
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.features.widgets.WidgetPickerSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

class WidgetStackConfigureActivity : ComponentActivity() {
    private lateinit var repository: WidgetStackRepository
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var awm: AppWidgetManager
    private var stackWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var config by mutableStateOf(WidgetStackConfig(AppWidgetManager.INVALID_APPWIDGET_ID))
    private var overlayGranted by mutableStateOf(true)
    private var unsupported by mutableStateOf(emptySet<Int>())
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private var showPicker by mutableStateOf(false)

    // Asks the user to let this app show the chosen widget, the first time a widget from it is added.
    private val bindLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val info = awm.getAppWidgetInfo(pendingWidgetId)
            if (result.resultCode == RESULT_OK && info != null) onWidgetBound(info) else discardPending()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stackWidgetId =
            intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (stackWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Keep the stack on the homescreen even if this screen is closed without changes.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, stackWidgetId))

        repository = WidgetStackRepository(this)
        widgetHost = AppWidgetHost(this, StackHost.HOST_ID)
        awm = AppWidgetManager.getInstance(this)
        config = repository.get(stackWidgetId) ?: WidgetStackConfig(stackWidgetId).also { repository.save(it) }
        StackHost.refresh(this)

        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) { viewModel.check(context) }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                StackSettingsSheet()
                if (showPicker) {
                    WidgetPickerSheet(
                        onPick = { onWidgetPicked(it) },
                        onDismiss = { showPicker = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        unsupported = StackHost.unsupported.toSet()
        if (overlayGranted) StackHost.refresh(this)
    }

    private fun addWidget() {
        if (config.hostedWidgetIds.size >= WidgetStackConfig.MAX_WIDGETS) return
        showPicker = true
    }

    private fun onWidgetPicked(info: AppWidgetProviderInfo) {
        showPicker = false
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        if (awm.bindAppWidgetIdIfAllowed(pendingWidgetId, info.profile, info.provider, null)) {
            onWidgetBound(info)
            return
        }
        bindLauncher.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            },
        )
    }

    private fun onWidgetBound(info: AppWidgetProviderInfo) {
        if (info.provider == ComponentName(this, StackWidgetProvider::class.java)) {
            Toast.makeText(this, R.string.widget_stack_cant_nest, Toast.LENGTH_SHORT).show()
            discardPending()
            return
        }

        val configurationOptional =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        if (info.configure != null && !configurationOptional) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, pendingWidgetId, 0, REQUEST_CONFIGURE, null)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not open the configuration screen of widget $pendingWidgetId", e)
            }
        }
        addPendingToStack()
    }

    @Deprecated("Needed for AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        if (resultCode == Activity.RESULT_OK) addPendingToStack() else discardPending()
    }

    private fun addPendingToStack() {
        val widgetId = pendingWidgetId
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        updateConfig(config.copy(hostedWidgetIds = config.hostedWidgetIds + widgetId))
    }

    private fun discardPending() {
        if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            widgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    private fun removeWidget(widgetId: Int) {
        widgetHost.deleteAppWidgetId(widgetId)
        StackHost.contents.remove(widgetId)
        StackHost.unsupported.remove(widgetId)
        updateConfig(config.copy(hostedWidgetIds = config.hostedWidgetIds - widgetId))
    }

    private fun moveWidget(
        index: Int,
        delta: Int,
    ) {
        val target = index + delta
        val ids = config.hostedWidgetIds.toMutableList()
        if (target !in ids.indices) return
        ids.add(target, ids.removeAt(index))
        updateConfig(config.copy(hostedWidgetIds = ids))
    }

    private fun updateConfig(updated: WidgetStackConfig) {
        config = updated
        // The new set of widgets may fit whole again.
        StackHost.visibleOnlyStacks.remove(stackWidgetId)
        repository.save(updated)
        StackWidgetProvider.render(this, stackWidgetId)
        StackHost.refresh(this)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun StackSettingsSheet() {
        val view = LocalView.current
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val isFull = config.hostedWidgetIds.size >= WidgetStackConfig.MAX_WIDGETS

        ModalBottomSheet(
            onDismissRequest = { finish() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_widgets_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.widget_stack_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = stringResource(R.string.widget_stack_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (!overlayGranted) {
                    RoundedCardContainer {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.widget_stack_overlay_needed)) },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName"),
                                            ),
                                        )
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(),
                                ) { Text(stringResource(R.string.widget_stack_grant)) }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                        )
                    }
                }

                SectionTitle(stringResource(R.string.widget_stack_widgets_title))
                RoundedCardContainer {
                    config.hostedWidgetIds.forEachIndexed { index, widgetId ->
                        HostedWidgetRow(
                            widgetId = widgetId,
                            isUnsupported = widgetId in unsupported,
                            canMoveUp = index > 0,
                            canMoveDown = index < config.hostedWidgetIds.lastIndex,
                            onMove = { delta ->
                                HapticUtil.performUIHaptic(view)
                                moveWidget(index, delta)
                            },
                            onRemove = {
                                HapticUtil.performUIHaptic(view)
                                removeWidget(widgetId)
                            },
                        )
                    }
                    ListItem(
                        modifier =
                            Modifier.clickable(enabled = !isFull) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                addWidget()
                            },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.rounded_add_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.widget_stack_add_widget)) },
                        supportingContent =
                            if (isFull) {
                                { Text(stringResource(R.string.widget_stack_max_reached, WidgetStackConfig.MAX_WIDGETS)) }
                            } else {
                                null
                            },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    )
                }

                SectionTitle(stringResource(R.string.widget_stack_interval_title))
                RoundedCardContainer {
                    ListItem(
                        headlineContent = {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetStackConfig.INTERVAL_OPTIONS.forEach { seconds ->
                                    FilterChip(
                                        selected = config.intervalSeconds == seconds,
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            updateConfig(config.copy(intervalSeconds = seconds))
                                        },
                                        label = {
                                            Text(
                                                if (seconds == 0) {
                                                    stringResource(R.string.widget_stack_interval_off)
                                                } else {
                                                    stringResource(R.string.widget_stack_interval_seconds, seconds)
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    )
                }

                SectionTitle(stringResource(R.string.widget_stack_show_controls))
                RoundedCardContainer {
                    ListItem(
                        headlineContent = {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WidgetStackConfig.ControlsMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = config.controls == mode,
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            updateConfig(config.withControls(mode))
                                        },
                                        label = { Text(stringResource(controlsLabel(mode))) },
                                    )
                                }
                            }
                        },
                        supportingContent =
                            if (config.controls == WidgetStackConfig.ControlsMode.AUTO_HIDE) {
                                { Text(stringResource(R.string.widget_stack_controls_auto_hide_desc)) }
                            } else {
                                null
                            },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        finish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.widget_stack_done)) }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    private fun controlsLabel(mode: WidgetStackConfig.ControlsMode) =
        when (mode) {
            WidgetStackConfig.ControlsMode.ALWAYS -> R.string.widget_stack_controls_always
            WidgetStackConfig.ControlsMode.AUTO_HIDE -> R.string.widget_stack_controls_auto_hide
            WidgetStackConfig.ControlsMode.HIDDEN -> R.string.widget_stack_controls_hidden
        }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }

    @Composable
    private fun HostedWidgetRow(
        widgetId: Int,
        isUnsupported: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onMove: (Int) -> Unit,
        onRemove: () -> Unit,
    ) {
        val info = remember(widgetId) { awm.getAppWidgetInfo(widgetId) }
        val label =
            remember(widgetId) { info?.loadLabel(packageManager) ?: getString(R.string.widget_stack_unknown_widget) }
        val appName =
            remember(widgetId) {
                info?.provider?.packageName?.let { pkg ->
                    try {
                        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        val icon =
            remember(widgetId) {
                info?.loadIcon(this, resources.displayMetrics.densityDpi)?.toBitmap(96, 96)?.asImageBitmap()
            }

        ListItem(
            leadingContent = {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                } else {
                    Icon(painter = painterResource(R.drawable.rounded_widgets_24), contentDescription = null)
                }
            },
            headlineContent = { Text(label) },
            supportingContent = {
                when {
                    isUnsupported ->
                        Text(
                            stringResource(R.string.widget_stack_unsupported_short),
                            color = MaterialTheme.colorScheme.error,
                        )
                    appName != null -> Text(appName)
                }
            },
            trailingContent = {
                Row {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_keyboard_arrow_up_24),
                            contentDescription = stringResource(R.string.widget_stack_move_up),
                        )
                    }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                            contentDescription = stringResource(R.string.widget_stack_move_down),
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_delete_24),
                            contentDescription = stringResource(R.string.widget_stack_remove_widget),
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        )
    }

    companion object {
        private const val TAG = "WidgetStackConfigure"
        private const val REQUEST_CONFIGURE = 4201
    }
}
