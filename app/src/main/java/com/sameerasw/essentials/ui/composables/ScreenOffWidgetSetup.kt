package com.sameerasw.essentials.ui.composables

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.MainViewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.EssentialsTheme

@Composable
fun ScreenOffWidgetSetup(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWidgetEnabled by viewModel.isWidgetEnabled
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        if (!isAccessibilityEnabled) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("To use the Screen Off widget, enable accessibility permission for this app.")
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }) {
                        Text("Go to Accessibility Settings")
                    }
                }
            }
        }

        FeatureCard(
            title = "Screen off widget",
            isEnabled = isWidgetEnabled,
            onToggle = {
                viewModel.setWidgetEnabled(it, context)
                // Update all existing widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, com.sameerasw.essentials.ScreenOffWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.screen_off_widget)
                    if (it) {
                        val intent = Intent(context, com.sameerasw.essentials.ScreenOffAccessibilityService::class.java).apply {
                            action = "LOCK_SCREEN"
                        }
                        val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            },
            onClick = { context.startActivity(Intent(context, FeatureSettingsActivity::class.java).apply { putExtra("feature", "Screen off widget") }) },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenOffWidgetSetupPreview() {
    EssentialsTheme {
        // Provide a mock ViewModel for preview
        val mockViewModel = MainViewModel().apply {
            // Set up any necessary state for the preview
            isAccessibilityEnabled.value = false
        }
        ScreenOffWidgetSetup(viewModel = mockViewModel)
    }
}
