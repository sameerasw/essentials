package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.ScreenOffWidgetProvider
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil

class ScreenOffWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        // Find the widget id from the intent.
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an invalid widget ID, finish with cancel.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                ScreenOffWidgetConfigureOverlay(
                    appWidgetId = appWidgetId,
                    onConfigured = { isDoubleTapRequired ->
                        saveWidgetPrefs(isDoubleTapRequired)
                        updateWidget()
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun saveWidgetPrefs(isDoubleTapRequired: Boolean) {
        val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("screen_off_double_tap_$appWidgetId", isDoubleTapRequired).apply()
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        ScreenOffWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenOffWidgetConfigureOverlay(
    appWidgetId: Int,
    onConfigured: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val prefs = remember {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    }

    var isDoubleTapRequired by remember {
        mutableStateOf(prefs.getBoolean("screen_off_double_tap_$appWidgetId", false))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_mobile_lock_portrait_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.screen_off_widget_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.feat_screen_off_widget_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Double Tap Toggle Card
            RoundedCardContainer(
                modifier = Modifier.padding(top = 8.dp),
                spacing = 8.dp,
                cornerRadius = 24.dp
            ) {
                IconToggleItem(
                    title = stringResource(R.string.require_double_tap_title),
                    description = stringResource(R.string.require_double_tap_desc),
                    iconRes = R.drawable.rounded_touch_app_24,
                    isChecked = isDoubleTapRequired,
                    onCheckedChange = {
                        isDoubleTapRequired = it
                        HapticUtil.performUIHaptic(view)
                    },
                    showToggle = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onConfigured(isDoubleTapRequired)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(android.R.string.ok))
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
