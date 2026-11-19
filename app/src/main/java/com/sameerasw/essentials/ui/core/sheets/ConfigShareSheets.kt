package com.sameerasw.essentials.ui.core.sheets

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.configshare.ConfigShareDescriptor
import com.sameerasw.essentials.domain.configshare.ConfigShareManager
import com.sameerasw.essentials.domain.configshare.ConfigVersionRelation
import com.sameerasw.essentials.domain.configshare.ParsedConfig
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.CheckboxListItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigShareSheet(
    descriptor: ConfigShareDescriptor,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sections =
        remember {
            val available = ConfigShareManager.availableSections(context, descriptor)
            descriptor.sections.filter { it.id in available }
        }
    var selected by remember { mutableStateOf(sections.filter { it.defaultSelected }.map { it.id }.toSet()) }
    var pendingText by remember { mutableStateOf<String?>(null) }

    val saveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val text = pendingText
            if (uri != null && text != null) {
                val ok =
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
                    } catch (e: Exception) {
                        false
                    }
                Toast.makeText(
                    context,
                    if (ok) R.string.config_share_saved else R.string.config_share_failed,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) onDismissRequest()
            }
        }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TranslatableText(
                stringResId = R.string.config_share_sheet_title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            TranslatableText(
                stringResId = R.string.config_share_sheet_desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                sections.forEachIndexed { index, section ->
                    CheckboxListItem(
                        title = stringResource(section.titleRes),
                        checked = section.id in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + section.id else selected - section.id
                        },
                        index = index,
                        count = sections.size,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        pendingText = ConfigShareManager.export(context, descriptor, selected)
                        saveLauncher.launch(ConfigShareManager.fileName(descriptor))
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_save_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.config_share_save_file)
                }
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        val text = ConfigShareManager.export(context, descriptor, selected)
                        if (!shareConfigText(context, descriptor, text)) {
                            Toast.makeText(context, R.string.config_share_failed, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_share_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.config_share_action_share)
                }
            }
        }
    }
}

private fun shareConfigText(
    context: Context,
    descriptor: ConfigShareDescriptor,
    text: String,
): Boolean =
    try {
        val dir = File(context.cacheDir, "configs").apply { mkdirs() }
        val file = File(dir, ConfigShareManager.fileName(descriptor)).apply { writeText(text) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigImportSheet(
    descriptor: ConfigShareDescriptor,
    config: ParsedConfig,
    onDismissRequest: () -> Unit,
    onApplied: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sections = descriptor.sections
    var selected by remember {
        mutableStateOf(sections.filter { it.id in config.sectionIds && it.defaultSelected }.map { it.id }.toSet())
    }
    var showConfirm by remember { mutableStateOf(false) }
    val relation = remember { ConfigShareManager.versionRelation(config) }
    val mismatch = relation != ConfigVersionRelation.SAME

    if (showConfirm) {
        ConfigImportConfirmSheet(
            onDismissRequest = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                ConfigShareManager.apply(context, config, selected)
                onApplied()
                Toast.makeText(context, R.string.config_import_done, Toast.LENGTH_SHORT).show()
                onDismissRequest()
            },
        )
    }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TranslatableText(
                stringResId = R.string.config_import_sheet_title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.config_import_from_version, config.appVersionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (mismatch) {
                RoundedCardContainer(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text =
                            stringResource(
                                if (relation == ConfigVersionRelation.OLDER) {
                                    R.string.config_import_older_warning
                                } else {
                                    R.string.config_import_newer_warning
                                },
                                config.appVersionName,
                                BuildConfig.VERSION_NAME,
                            ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    )
                }
            }

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                sections.forEachIndexed { index, section ->
                    CheckboxListItem(
                        title = stringResource(section.titleRes),
                        checked = section.id in selected,
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + section.id else selected - section.id
                        },
                        index = index,
                        count = sections.size,
                        enabled = section.id in config.sectionIds,
                    )
                }
            }

            TranslatableText(
                stringResId = R.string.config_import_replace_note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.action_abort)
                }
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showConfirm = true
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(if (mismatch) R.string.config_import_proceed else R.string.config_import_apply)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigImportConfirmSheet(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val view = LocalView.current
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TranslatableText(
                stringResId = R.string.config_import_confirm_title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            TranslatableText(
                stringResId = R.string.config_import_confirm_desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.action_abort)
                }
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onConfirm()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.config_import_apply)
                }
            }
        }
    }
}
