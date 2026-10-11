package com.sameerasw.essentials.ui.core.sheets

import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.utils.HiddenDebuggingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sameerasw.essentials.utils.ShellUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenDebuggingHelpBottomSheet(
    onDismissRequest: () -> Unit,
    onAutoDetected: () -> Unit,
) {
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setting_hidden_debugging_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.setting_hidden_debugging_help_1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.setting_hidden_debugging_help_2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.setting_hidden_debugging_help_3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HiddenDebuggingAutoDetectCard(onAutoDetected = onAutoDetected)
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_got_it))
            }
        }
    }
}

@Composable
fun HiddenDebuggingAutoDetectCard(
    onAutoDetected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detecting by remember { mutableStateOf(false) }
    val shizukuReady = remember(detecting) { ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright, RoundedCornerShape(24.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.setting_hidden_debugging_auto_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.setting_hidden_debugging_auto_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = {
                detecting = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { HiddenDebuggingUtils.autoDetect(context) }
                    detecting = false
                    val message =
                        when (result) {
                            HiddenDebuggingUtils.DetectResult.HIDDEN -> {
                                onAutoDetected()
                                R.string.setting_hidden_debugging_detected
                            }
                            HiddenDebuggingUtils.DetectResult.NOT_HIDDEN -> R.string.setting_hidden_debugging_not_hidden
                            HiddenDebuggingUtils.DetectResult.INCONCLUSIVE -> R.string.setting_hidden_debugging_inconclusive
                            HiddenDebuggingUtils.DetectResult.SHELL_UNAVAILABLE -> R.string.setting_hidden_debugging_needs_shizuku
                        }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            enabled = shizukuReady && !detecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (shizukuReady) R.string.setting_hidden_debugging_auto_title else R.string.setting_hidden_debugging_shizuku_required,
                ),
            )
        }
    }
}
