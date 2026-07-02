package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.WatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchInstallHelpBottomSheet(
    onDismissRequest: () -> Unit,
    viewModel: WatchViewModel = viewModel()
) {
    LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_watch_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = stringResource(R.string.watch_help_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Description
            RoundedCardContainer(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.watch_help_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // GitHub Download Button
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    uriHandler.openUri("http://github.com/sameerasw/essentials-wear/releases/latest")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_download_from_github),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Watch ADB Permissions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp)
            )

            val context = LocalContext.current

            // Secure Settings Command Card
            AdbCommandCard(
                title = "Write Secure Settings",
                description = "Required on the watch to toggle Wireless Debugging remotely.",
                command = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS",
                context = context,
                view = view
            )

            // DND Access Command Card
            AdbCommandCard(
                title = "Do Not Disturb (DND) Access",
                description = "Required on the watch for DND / Silent sync modes.",
                command = "adb shell cmd notification allow_dnd com.sameerasw.essentials",
                context = context,
                view = view
            )
        }
    }
}

@Composable
private fun AdbCommandCard(
    title: String,
    description: String,
    command: String,
    context: android.content.Context,
    view: android.view.View
) {
    RoundedCardContainer(
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                androidx.compose.material3.IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("adb_command", command)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Command copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_content_copy_24),
                        contentDescription = "Copy command",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
