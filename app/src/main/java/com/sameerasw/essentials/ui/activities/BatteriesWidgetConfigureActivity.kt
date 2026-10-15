package com.sameerasw.essentials.ui.activities

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.widgets.BatteriesWidget
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

class BatteriesWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId =
            intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            EssentialsTheme {
                BatteriesWidgetConfigureSheet(
                    appWidgetId = appWidgetId,
                    onDone = {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatteriesWidgetConfigureSheet(
    appWidgetId: Int,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val key = booleanPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_BACKGROUND_ENABLED)
    val centerKey = booleanPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_CENTER_ITEMS)
    var background by remember { mutableStateOf(true) }
    var centerItems by remember { mutableStateOf(false) }

    LaunchedEffect(appWidgetId) {
        try {
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            background = state[key] ?: true
            centerItems = state[centerKey] ?: false
        } catch (_: Exception) {
        }
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feat_batteries_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp),
            )
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_circles_24,
                    title = stringResource(R.string.widget_background_title),
                    isChecked = background,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        background = it
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_format_align_center_24,
                    title = stringResource(R.string.widget_center_items_title),
                    isChecked = centerItems,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        centerItems = it
                    },
                )
            }
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    scope.launch {
                        try {
                            val manager = GlanceAppWidgetManager(context)
                            val glanceId = manager.getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(context, glanceId) {
                                it[key] = background
                                it[centerKey] = centerItems
                            }
                            BatteriesWidget().update(context, glanceId)
                        } catch (_: Exception) {
                        }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}
