package com.sameerasw.essentials.ui.core.sheets

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.configshare.ConfigVersionRelation
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.AutomationParseError
import com.sameerasw.essentials.domain.diy.AutomationParseResult
import com.sameerasw.essentials.domain.diy.DIYShareManager
import com.sameerasw.essentials.domain.diy.ParsedAutomation
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.navigation.findFragmentActivity
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.io.File

fun automationTypeTitle(type: Automation.Type): Int =
    when (type) {
        Automation.Type.TRIGGER -> R.string.diy_create_trigger_title
        Automation.Type.STATE -> R.string.diy_create_state_title
        Automation.Type.APP -> R.string.diy_create_app_title
        Automation.Type.ACTION_SHORTCUT -> R.string.diy_create_action_shortcut_title
        Automation.Type.PIXEL_SEARCHBAR -> R.string.diy_create_pixel_searchbar_title
        else -> R.string.diy_create_accessibility_shortcut_title
    }

fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

fun parseAutomationOrToast(
    context: Context,
    text: String?,
): ParsedAutomation? {
    if (text.isNullOrBlank()) {
        Toast.makeText(context, R.string.diy_import_clipboard_empty, Toast.LENGTH_SHORT).show()
        return null
    }
    return when (val result = DIYShareManager.parse(context, text)) {
        is AutomationParseResult.Success -> result.parsed
        is AutomationParseResult.Failure -> {
            Toast
                .makeText(
                    context,
                    when (result.error) {
                        AutomationParseError.TOO_LARGE -> R.string.config_import_error_too_large
                        AutomationParseError.NEWER_SCHEMA, AutomationParseError.UNSUPPORTED -> R.string.config_import_error_newer_schema
                        AutomationParseError.CONFLICT -> R.string.diy_import_error_conflict
                        AutomationParseError.EMPTY -> R.string.config_import_error_empty
                        AutomationParseError.INVALID -> R.string.config_import_error_invalid
                    },
                    Toast.LENGTH_LONG,
                ).show()
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationShareSheet(
    automation: Automation,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
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
                stringResId = R.string.diy_share_sheet_title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            TranslatableText(
                stringResId = R.string.diy_share_sheet_desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("automation", DIYShareManager.export(automation)))
                        Toast.makeText(context, R.string.diy_share_copied, Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_content_copy_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.diy_share_copy)
                }
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        if (!shareAutomationFile(context, automation)) {
                            Toast.makeText(context, R.string.config_share_failed, Toast.LENGTH_SHORT).show()
                        }
                    },
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

private fun shareAutomationFile(
    context: Context,
    automation: Automation,
): Boolean =
    try {
        val dir = File(context.cacheDir, "configs").apply { mkdirs() }
        val file = File(dir, DIYShareManager.fileName(automation)).apply { writeText(DIYShareManager.export(automation)) }
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
fun AutomationImportSheet(
    parsed: ParsedAutomation,
    onDismissRequest: () -> Unit,
    onImported: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val mainViewModel: MainViewModel = viewModel()
    val activity: Activity = context.findFragmentActivity()
    var showPermissions by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    val missing = remember(refresh, showPermissions) { DIYShareManager.missingPermissions(context, parsed.automation) }
    val missingNames = missing.joinToString { context.getString(it.titleRes) }
    val relation = remember { DIYShareManager.versionRelation(parsed) }
    val mismatch = relation != ConfigVersionRelation.SAME
    val automation = parsed.automation

    if (showPermissions && missing.isNotEmpty()) {
        PermissionsBottomSheet(
            onDismissRequest = {
                showPermissions = false
                refresh++
            },
            featureTitle = R.string.tab_diy,
            permissions = PermissionUIHelper.getPermissionItems(missing, context, mainViewModel, activity),
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
                stringResId = R.string.diy_import_sheet_title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.config_import_from_version, parsed.appVersionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(automationTypeTitle(automation.type)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    automation.trigger?.let { Text(stringResource(it.title), style = MaterialTheme.typography.bodyMedium) }
                    automation.state?.let { Text(stringResource(it.title), style = MaterialTheme.typography.bodyMedium) }
                    (automation.actionList + automation.entryActionList + automation.exitActionList).forEach {
                        Text(
                            text = stringResource(it.title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (mismatch) {
                WarningCard(
                    stringResource(
                        if (relation == ConfigVersionRelation.OLDER) {
                            R.string.config_import_older_warning
                        } else {
                            R.string.config_import_newer_warning
                        },
                        parsed.appVersionName,
                        BuildConfig.VERSION_NAME,
                    ),
                )
            }
            if (parsed.changesSystemSettings) WarningCard(stringResource(R.string.diy_import_risky))
            if (parsed.needsSetup) WarningCard(stringResource(R.string.diy_import_needs_setup))
            if (missing.isNotEmpty()) {
                WarningCard(
                    stringResource(
                        R.string.diy_import_missing_permissions,
                        missingNames,
                    ),
                )
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showPermissions = true
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_security_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TranslatableText(R.string.diy_import_grant)
                }
            }

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
                        DIYShareManager.import(context, parsed)
                        Toast.makeText(context, R.string.diy_import_done, Toast.LENGTH_SHORT).show()
                        onImported()
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
                    TranslatableText(if (mismatch) R.string.config_import_proceed else R.string.diy_import_action)
                }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    RoundedCardContainer(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = text,
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
