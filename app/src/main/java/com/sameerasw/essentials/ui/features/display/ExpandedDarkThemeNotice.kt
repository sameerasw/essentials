package com.sameerasw.essentials.ui.features.display

import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ForceInvertUtils

@Composable
fun ExpandedDarkThemeNotice(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    var dismissed by remember { mutableStateOf(settings.isIslandExpandedDarkNoticeDismissed()) }
    var enabled by remember { mutableStateOf(ForceInvertUtils.isEnabled(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = ForceInvertUtils.isEnabled(context)
            }
        }
        context.contentResolver.registerContentObserver(Settings.Secure.getUriFor(ForceInvertUtils.KEY), false, observer)
        enabled = ForceInvertUtils.isEnabled(context)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    if (!enabled || dismissed) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.island_expanded_dark_theme_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.island_expanded_dark_theme_action))
                }
                TextButton(
                    onClick = {
                        settings.setIslandExpandedDarkNoticeDismissed(true)
                        dismissed = true
                    },
                ) {
                    Text(stringResource(R.string.action_dismiss), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}
