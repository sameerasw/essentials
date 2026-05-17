package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppsActionButtons(
    onAddClick: () -> Unit,
    onRefreshAllClick: () -> Unit,
    isRefreshing: Boolean,
    progress: () -> Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_add))
        }

        Button(
            onClick = onRefreshAllClick,
            modifier = Modifier.weight(1f),
            enabled = !isRefreshing
        ) {
            if (isRefreshing) {
                @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                CircularWavyProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_refresh_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_refresh))
        }
    }
}

@Composable
fun ImportExportButtons(
    view: android.view.View,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    showExport: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showExport) {
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    val timeStamp = SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                    ).format(Date())
                    exportLauncher.launch("essentials_updates_$timeStamp.json")
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_warm_up_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_export))
            }
        }

        Button(
            onClick = {
                HapticUtil.performUIHaptic(view)
                importLauncher.launch(arrayOf("application/json"))
            },
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_arrow_cool_down_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_import))
        }
    }
}
