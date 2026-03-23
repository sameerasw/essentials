package com.sameerasw.essentials.ui.activities

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.sameerasw.essentials.domain.model.DnsPreset

class PrivateDnsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                PrivateDnsSettingsOverlay(onDismiss = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateDnsSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val PRIVATE_DNS_MODE = "private_dns_mode"
    val PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

    var showAddDialog by remember { mutableStateOf(false) }

    val currentMode = remember {
        Settings.Global.getString(context.contentResolver, PRIVATE_DNS_MODE) ?: "off"
    }
    val currentHostname = remember {
        Settings.Global.getString(context.contentResolver, PRIVATE_DNS_SPECIFIER) ?: ""
    }

    var selectedMode by remember { mutableStateOf(currentMode) }
    var customHostname by remember { mutableStateOf(currentHostname) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                    painter = painterResource(id = R.drawable.router_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.tile_private_dns),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Mode Selection Container
            RoundedCardContainer {
                DnsSegmentedItem(
                    label = stringResource(R.string.tile_private_dns_off),
                    isSelected = selectedMode == "off",
                    onClick = {
                        selectedMode = "off"
                        HapticUtil.performUIHaptic(view)
                    }
                )
                DnsSegmentedItem(
                    label = stringResource(R.string.tile_private_dns_auto),
                    isSelected = selectedMode == "opportunistic",
                    onClick = {
                        selectedMode = "opportunistic"
                        HapticUtil.performUIHaptic(view)
                    }
                )
                DnsSegmentedItem(
                    label = stringResource(R.string.private_dns_custom_title),
                    isSelected = selectedMode == "hostname",
                    onClick = {
                        selectedMode = "hostname"
                        HapticUtil.performUIHaptic(view)
                    }
                )
            }

            if (selectedMode == "hostname") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        OutlinedTextField(
                            value = customHostname,
                            onValueChange = { customHostname = it },
                            label = { Text(stringResource(R.string.private_dns_hostname_label)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.private_dns_presets_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    (viewModel as? com.sameerasw.essentials.viewmodels.MainViewModel)?.resetDnsPresets()
                                    HapticUtil.performUIHaptic(view)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dns_preset_reset_action),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Button(
                                onClick = {
                                    showAddDialog = true
                                    HapticUtil.performUIHaptic(view)
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_add_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_add),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (showAddDialog) {
                        AddDnsPresetDialog(
                            onDismiss = { showAddDialog = false },
                            onConfirm = { name, host ->
                                (viewModel as? com.sameerasw.essentials.viewmodels.MainViewModel)?.addDnsPreset(name, host)
                                showAddDialog = false
                                HapticUtil.performUIHaptic(view)
                            }
                        )
                    }

                    RoundedCardContainer {
                        val presets = (viewModel as? com.sameerasw.essentials.viewmodels.MainViewModel)?.dnsPresets ?: emptyList<DnsPreset>()

                        presets.forEach { preset ->
                            DnsPresetItem(
                                name = preset.name,
                                hostname = preset.hostname,
                                isSelected = customHostname == preset.hostname,
                                onClick = {
                                    customHostname = preset.hostname
                                    HapticUtil.performUIHaptic(view)
                                },
                                onDelete = {
                                    (viewModel as? com.sameerasw.essentials.viewmodels.MainViewModel)?.removeDnsPreset(preset)
                                    HapticUtil.performUIHaptic(view)
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        try {
                            Settings.Global.putString(
                                context.contentResolver,
                                PRIVATE_DNS_MODE,
                                selectedMode
                            )
                            if (selectedMode == "hostname") {
                                Settings.Global.putString(
                                    context.contentResolver,
                                    PRIVATE_DNS_SPECIFIER,
                                    customHostname
                                )
                            }
                            HapticUtil.performHeavyHaptic(view)
                            onDismiss()
                        } catch (e: Exception) {
                            // Handle permission error if any
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
fun DnsSegmentedItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun DnsPresetItem(
    name: String,
    hostname: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = hostname,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Icon(
                    painter = painterResource(id = R.drawable.rounded_delete_24),
                    contentDescription = stringResource(R.string.dns_preset_delete_content_description),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDelete() }
                )
            }
        }
    }
}

@Composable
fun AddDnsPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dns_preset_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dns_preset_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    label = { Text(stringResource(R.string.private_dns_hostname_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && hostname.isNotBlank()) onConfirm(name, hostname) },
                enabled = name.isNotBlank() && hostname.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    )
}
