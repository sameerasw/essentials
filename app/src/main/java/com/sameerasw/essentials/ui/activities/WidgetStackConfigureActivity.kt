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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.services.widgets.StackHostService
import com.sameerasw.essentials.services.widgets.StackWidgetProvider
import com.sameerasw.essentials.ui.theme.EssentialsTheme

class WidgetStackConfigureActivity : ComponentActivity() {
    private lateinit var repository: WidgetStackRepository
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var awm: AppWidgetManager
    private var stackWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var config by mutableStateOf(WidgetStackConfig(AppWidgetManager.INVALID_APPWIDGET_ID))
    private var overlayGranted by mutableStateOf(true)
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val pickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val widgetId =
                result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
            if (result.resultCode != RESULT_OK || widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                discardPending()
                return@registerForActivityResult
            }
            pendingWidgetId = widgetId
            // The system picker binds the widget itself, so a picked widget already has its info.
            val info = awm.getAppWidgetInfo(widgetId)
            if (info != null) onWidgetBound(info) else discardPending()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        widgetHost = AppWidgetHost(this, StackHostService.HOST_ID)
        awm = AppWidgetManager.getInstance(this)
        config = repository.get(stackWidgetId) ?: WidgetStackConfig(stackWidgetId).also { repository.save(it) }
        StackHostService.refresh(this)

        setContent {
            EssentialsTheme {
                StackSettingsScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        if (overlayGranted) StackHostService.refresh(this)
    }

    private fun addWidget() {
        if (config.hostedWidgetIds.size >= WidgetStackConfig.MAX_WIDGETS) {
            Toast
                .makeText(
                    this,
                    getString(R.string.widget_stack_max_reached, WidgetStackConfig.MAX_WIDGETS),
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        pickLauncher.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId),
        )
    }

    private fun onWidgetBound(info: AppWidgetProviderInfo) {
        val widgetId = pendingWidgetId
        val configurationOptional =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL != 0
        if (info.configure != null && !configurationOptional) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, widgetId, 0, REQUEST_CONFIGURE, null)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not open the configuration screen of widget $widgetId", e)
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
        StackHostService.contents.remove(widgetId)
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
        repository.save(updated)
        StackWidgetProvider.render(this, stackWidgetId)
        StackHostService.refresh(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StackSettingsScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.widget_stack_settings_title)) },
                    actions = {
                        TextButton(onClick = { finish() }) { Text(stringResource(R.string.widget_stack_done)) }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!overlayGranted) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.widget_stack_overlay_needed))
                            Button(onClick = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName"),
                                    ),
                                )
                            }) { Text(stringResource(R.string.widget_stack_grant)) }
                        }
                    }
                }

                Text(stringResource(R.string.widget_stack_widgets_title), style = MaterialTheme.typography.titleMedium)
                config.hostedWidgetIds.forEachIndexed { index, widgetId ->
                    HostedWidgetRow(
                        widgetId = widgetId,
                        canMoveUp = index > 0,
                        canMoveDown = index < config.hostedWidgetIds.lastIndex,
                        onMoveUp = { moveWidget(index, -1) },
                        onMoveDown = { moveWidget(index, 1) },
                        onRemove = { removeWidget(widgetId) },
                    )
                }
                Button(
                    onClick = { addWidget() },
                    enabled = config.hostedWidgetIds.size < WidgetStackConfig.MAX_WIDGETS,
                ) { Text(stringResource(R.string.widget_stack_add_widget)) }

                Text(stringResource(R.string.widget_stack_interval_title), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetStackConfig.INTERVAL_OPTIONS.forEach { seconds ->
                        FilterChip(
                            selected = config.intervalSeconds == seconds,
                            onClick = { updateConfig(config.copy(intervalSeconds = seconds)) },
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.widget_stack_show_controls), modifier = Modifier.weight(1f))
                    Switch(
                        checked = config.showControls,
                        onCheckedChange = { updateConfig(config.copy(showControls = it)) },
                    )
                }
            }
        }
    }

    @Composable
    private fun HostedWidgetRow(
        widgetId: Int,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        onRemove: () -> Unit,
    ) {
        val info = remember(widgetId) { awm.getAppWidgetInfo(widgetId) }
        val label =
            remember(widgetId) { info?.loadLabel(packageManager) ?: getString(R.string.widget_stack_unknown_widget) }
        val icon =
            remember(widgetId) {
                info?.loadIcon(this, resources.displayMetrics.densityDpi)?.toBitmap(96, 96)?.asImageBitmap()
            }
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.size(12.dp))
                }
                Text(label, modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        painterResource(R.drawable.rounded_arrow_back_24),
                        contentDescription = stringResource(R.string.widget_stack_move_up),
                        modifier = Modifier.rotate(90f),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        painterResource(R.drawable.rounded_arrow_back_24),
                        contentDescription = stringResource(R.string.widget_stack_move_down),
                        modifier = Modifier.rotate(-90f),
                    )
                }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.widget_stack_remove_widget)) }
            }
        }
    }

    companion object {
        private const val TAG = "WidgetStackConfigure"
        private const val REQUEST_CONFIGURE = 4201
    }
}
