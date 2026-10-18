package com.sameerasw.essentials.ui.features.system

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBackupPickerSheet(
    onDismissRequest: () -> Unit,
    onBackupSelected: (Uri) -> Unit,
    onPickCustom: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var backups by remember { mutableStateOf<List<AutoBackupManager.BackupEntry>?>(null) }

    LaunchedEffect(Unit) {
        backups = withContext(Dispatchers.IO) { AutoBackupManager.listBackups(context) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.import_backup_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
            val list = backups
            if (list != null && list.isEmpty()) {
                Text(
                    text = stringResource(R.string.import_backup_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            list?.forEachIndexed { index, entry ->
                val top = if (index == 0) 24.dp else 4.dp
                val bottom = if (index == list.size - 1) 24.dp else 4.dp
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                            ).clickable { onBackupSelected(entry.uri) }
                            .padding(16.dp),
                ) {
                    Text(
                        text =
                            DateUtils.formatDateTime(
                                context,
                                entry.modified,
                                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onPickCustom,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.import_backup_pick_custom))
            }
        }
    }
}
