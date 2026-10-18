package com.sameerasw.essentials.ui.features.system

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.AutoBackupManager

@Composable
fun AutoBackupSetting() {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(AutoBackupManager.getMode(context)) }
    var folder by remember { mutableStateOf(AutoBackupManager.getFolder(context)) }
    var expanded by remember { mutableStateOf(false) }

    val options =
        listOf(
            AutoBackupManager.MODE_NEVER to stringResource(R.string.auto_backup_never),
            AutoBackupManager.MODE_UPDATING to stringResource(R.string.auto_backup_updating),
            AutoBackupManager.MODE_DAILY to stringResource(R.string.auto_backup_daily),
            AutoBackupManager.MODE_WEEKLY to stringResource(R.string.auto_backup_weekly),
        )

    val storagePermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                AutoBackupManager.setFolder(context, uri)
                folder = uri
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, Shapes.extraSmall)
                    .clickable { expanded = true }
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.auto_backup_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Box {
                Text(
                    text = options.first { it.first == mode }.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                mode = value
                                AutoBackupManager.setMode(context, value)
                                if (value != AutoBackupManager.MODE_NEVER &&
                                    AutoBackupManager.needsLegacyStoragePermission() &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            },
                        )
                    }
                }
            }
        }

        if (mode != AutoBackupManager.MODE_NEVER) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright, Shapes.extraSmall)
                        .clickable { folderPicker.launch(null) }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.auto_backup_folder),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = folder?.lastPathSegment?.substringAfterLast(':')?.ifBlank { null }
                        ?: "Downloads/${AutoBackupManager.DEFAULT_FOLDER}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
