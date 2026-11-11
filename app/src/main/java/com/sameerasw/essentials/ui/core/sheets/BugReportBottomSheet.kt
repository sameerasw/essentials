/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: BugReportBottomSheet.kt
 * Description: Bottom sheet for submitting bug reports, attaching crash logs, and exporting diagnostic reports.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.data.repository.BugReportRepository
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.github.BugReportAutoFill
import com.sameerasw.essentials.domain.model.github.BugReportFieldOptions
import com.sameerasw.essentials.domain.model.github.BugReportFormIds
import com.sameerasw.essentials.domain.model.github.BugReportIssueFormatter
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.LogManager
import com.sameerasw.essentials.viewmodels.MainViewModel
import io.sentry.Sentry
import io.sentry.protocol.Feedback
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BugReportBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    sourceFeature: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var deviceInfoString by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    val bugReportRepository = remember { BugReportRepository() }
    val gitHubToken = remember { SettingsRepository(context).getGitHubToken() }
    val coroutineScope = rememberCoroutineScope()
    var fieldOptions by remember { mutableStateOf(BugReportFieldOptions.DEFAULT) }
    var issueFields by remember { mutableStateOf(BugReportAutoFill.initial(context, BugReportFieldOptions.DEFAULT)) }
    var isCreatingIssue by remember { mutableStateOf(false) }

    var isDeviceInfoExpanded by remember { mutableStateOf(false) }
    var isCrashReportsExpanded by remember { mutableStateOf(false) }

    var crashReports by remember {
        mutableStateOf(LogManager.getAllCrashReports(context).take(5))
    }
    var selectedCrashReport by remember { mutableStateOf<File?>(null) }

    val displayDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    LaunchedEffect(Unit) {
        val jsonString = viewModel.generateBugReport(context)
        try {
            val jsonObject = JSONObject(jsonString)
            val deviceInfo = jsonObject.optJSONObject("device_info")
            if (deviceInfo != null) {
                val sb = StringBuilder()
                val keys = deviceInfo.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    sb.append("$key: ${deviceInfo.get(key)}\n")
                }
                deviceInfoString = sb.toString().trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deviceInfoString = "Error parsing device info"
        }
    }

    LaunchedEffect(Unit) {
        val fetched = bugReportRepository.fetchFieldOptions(gitHubToken)
        fieldOptions = fetched
        issueFields = issueFields.reconcileWith(fetched)
    }

    val readSelectedCrashLog: () -> String? = {
        selectedCrashReport?.let { file ->
            try {
                file.readText().trim()
            } catch (e: Exception) {
                "Failed to read crash log: ${e.message}"
            }
        }
    }

    val openPrefilledIssueForm: () -> Unit = {
        val additional =
            BugReportIssueFormatter
                .additionalContext(deviceInfoString, selectedCrashReport?.name, readSelectedCrashLog())
                .let { if (it.length > MAX_PREFILL_CONTEXT_LENGTH) it.take(MAX_PREFILL_CONTEXT_LENGTH) + "\n…" else it }
        val params =
            buildList {
                add("template" to "bug_report.yml")
                if (feedbackMessage.isNotBlank()) {
                    add("title" to BugReportIssueFormatter.title(feedbackMessage, sourceFeature))
                    add(BugReportFormIds.DESCRIPTION to BugReportIssueFormatter.describe(feedbackMessage, sourceFeature))
                }
                issueFields.severity?.let { add(BugReportFormIds.SEVERITY to it) }
                issueFields.androidVersion?.let { add(BugReportFormIds.ANDROID_VERSION to it) }
                issueFields.oem?.let { add(BugReportFormIds.OEM to it) }
                issueFields.area?.let { add(BugReportFormIds.AREA to it) }
                add(BugReportFormIds.DEVICE to issueFields.device.trim())
                add(BugReportFormIds.APP_VERSION to issueFields.appVersion.trim())
                add(BugReportFormIds.ADDITIONAL to additional)
            }
        val uri =
            Uri
                .parse("https://github.com/sameerasw/essentials/issues/new")
                .buildUpon()
                .apply { params.forEach { (key, value) -> appendQueryParameter(key, value) } }
                .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    val reportOnGitHub: () -> Unit = report@{
        val token = gitHubToken
        if (token == null) {
            openPrefilledIssueForm()
            return@report
        }
        if (feedbackMessage.isBlank() || issueFields.severity == null) {
            Toast
                .makeText(context, R.string.bug_report_github_missing_fields, Toast.LENGTH_SHORT)
                .show()
            return@report
        }
        isCreatingIssue = true
        coroutineScope.launch {
            val issueUrl =
                bugReportRepository.createIssue(
                    token = token,
                    title = BugReportIssueFormatter.title(feedbackMessage, sourceFeature),
                    body =
                        BugReportIssueFormatter.body(
                            description = BugReportIssueFormatter.describe(feedbackMessage, sourceFeature),
                            fields = issueFields,
                            additionalContext =
                                BugReportIssueFormatter.additionalContext(
                                    deviceInfoString,
                                    selectedCrashReport?.name,
                                    readSelectedCrashLog(),
                                ),
                        ),
                    fieldValues = issueFields.asIssueFieldValues(),
                    fieldIds = fieldOptions.fieldIds,
                )
            isCreatingIssue = false
            if (issueUrl != null) {
                Toast.makeText(context, R.string.bug_report_github_created, Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issueUrl)))
                onDismissRequest()
            } else {
                Toast.makeText(context, R.string.bug_report_github_failed, Toast.LENGTH_SHORT).show()
                openPrefilledIssueForm()
            }
        }
    }

    val buildFullReportText: () -> String = {
        buildString {
            if (feedbackMessage.isNotBlank()) {
                append("User Feedback:\n")
                append(BugReportIssueFormatter.describe(feedbackMessage, sourceFeature))
                append("\n\n")
            }
            if (contactEmail.isNotBlank()) {
                append("Contact Email: ")
                append(contactEmail.trim())
                append("\n\n")
            }
            append("--- Device Info ---\n")
            append(deviceInfoString)

            if (selectedCrashReport != null) {
                append("\n\n--- Attached Crash Report: ${selectedCrashReport?.name} ---\n")
                try {
                    append(selectedCrashReport?.readText()?.trim())
                } catch (e: Exception) {
                    append("Error reading crash report: ${e.message}")
                }
            }
        }
    }

    val saveReportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain"),
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(buildFullReportText().toByteArray())
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_report_saved),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_save_report),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        edgeToEdge = true,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.bug_report_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                if (sourceFeature != null) {
                    Text(
                        text = stringResource(R.string.bug_report_source_feature, sourceFeature),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // Segmented Input Fields Container (Describe issue + Contact email)
            RoundedCardContainer(spacing = 2.dp) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        label = { Text(stringResource(R.string.bug_report_feedback_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        minLines = 3,
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                ) {
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text(stringResource(R.string.bug_report_contact_email_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                }
            }

            RoundedCardContainer(spacing = 2.dp) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.bug_report_issue_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BugReportDropdownField(
                        label = stringResource(R.string.bug_report_field_severity),
                        value = issueFields.severity,
                        options = fieldOptions.severities,
                        onSelected = { issueFields = issueFields.copy(severity = it) },
                    )
                    BugReportDropdownField(
                        label = stringResource(R.string.bug_report_field_area),
                        value = issueFields.area,
                        options = fieldOptions.areas,
                        onSelected = { issueFields = issueFields.copy(area = it.takeUnless { it == issueFields.area }) },
                    )
                    BugReportDropdownField(
                        label = stringResource(R.string.bug_report_field_android_version),
                        value = issueFields.androidVersion,
                        options = fieldOptions.androidVersions,
                        onSelected = { issueFields = issueFields.copy(androidVersion = it) },
                    )
                    BugReportDropdownField(
                        label = stringResource(R.string.bug_report_field_oem),
                        value = issueFields.oem,
                        options = fieldOptions.oems,
                        onSelected = { issueFields = issueFields.copy(oem = it) },
                    )
                    OutlinedTextField(
                        value = issueFields.device,
                        onValueChange = { issueFields = issueFields.copy(device = it) },
                        label = { Text(stringResource(R.string.bug_report_field_device)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = issueFields.appVersion,
                        onValueChange = { issueFields = issueFields.copy(appVersion = it) },
                        label = { Text(stringResource(R.string.bug_report_field_app_version)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        singleLine = true,
                    )
                }
            }

            RoundedCardContainer(spacing = 2.dp) {
                val deviceInfoRotation by animateFloatAsState(
                    targetValue = if (isDeviceInfoExpanded) 180f else 0f,
                    label = "DeviceInfoChevronRotation",
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).clickable {
                                HapticUtil.performUIHaptic(view)
                                isDeviceInfoExpanded = !isDeviceInfoExpanded
                            }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_devices_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.bug_report_device_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .rotate(deviceInfoRotation),
                    )
                }

                AnimatedVisibility(
                    visible = isDeviceInfoExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = Shapes.extraSmall,
                                ).padding(16.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = deviceInfoString,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            RoundedCardContainer(spacing = 2.dp) {
                val crashReportsRotation by animateFloatAsState(
                    targetValue = if (isCrashReportsExpanded) 180f else 0f,
                    label = "CrashReportsChevronRotation",
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).clickable {
                                HapticUtil.performUIHaptic(view)
                                isCrashReportsExpanded = !isCrashReportsExpanded
                            }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_bug_report_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.crash_logs_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = crashReports.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .rotate(crashReportsRotation),
                    )
                }

                AnimatedVisibility(
                    visible = isCrashReportsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (crashReports.isEmpty()) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceBright,
                                            shape = Shapes.extraSmall,
                                        ).padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.toast_no_crash_logs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            crashReports.forEach { file ->
                                val isSelected = selectedCrashReport == file
                                val formattedDate =
                                    remember(file.lastModified()) {
                                        displayDateFormat.format(Date(file.lastModified()))
                                    }
                                val fileSizeKb =
                                    remember(file.length()) {
                                        "${(file.length() / 1024).coerceAtLeast(1)} KB"
                                    }

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color =
                                                    if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceBright
                                                    },
                                                shape = Shapes.extraSmall,
                                            ).clickable {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                selectedCrashReport =
                                                    if (isSelected) null else file
                                            }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            selectedCrashReport =
                                                if (isSelected) null else file
                                        },
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$formattedDate • $fileSizeKb",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    LogManager.clearAllCrashReports(context)
                                    crashReports = emptyList()
                                    selectedCrashReport = null
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_crash_logs_cleared),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                shape = Shapes.extraSmall,
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_delete_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_clear_all_crash_logs),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        val feedbackText = buildString {
                            append(BugReportIssueFormatter.describe(feedbackMessage, sourceFeature))
                            append("\n\n")
                            issueFields.asIssueFieldValues().forEach { (name, value) ->
                                append("$name: $value\n")
                            }
                            if (issueFields.device.isNotBlank()) append("Device: ${issueFields.device.trim()}\n")
                            if (selectedCrashReport != null) {
                                append("\n\n--- Attached Crash Log (${selectedCrashReport?.name}) ---\n")
                                try {
                                    append(selectedCrashReport?.readText()?.trim())
                                } catch (e: Exception) {
                                    append("Failed to read crash log: ${e.message}")
                                }
                            }
                        }
                        val feedback = Feedback(feedbackText)
                        if (contactEmail.isNotBlank()) {
                            feedback.contactEmail = contactEmail
                        }
                        Sentry.captureFeedback(feedback)
                        Toast
                            .makeText(context, R.string.msg_feedback_sent, Toast.LENGTH_SHORT)
                            .show()
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = feedbackMessage.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_send_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_send_feedback))
                }

                Text(
                    text = stringResource(R.string.label_alternatively),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            reportOnGitHub()
                        },
                        enabled = !isCreatingIssue,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        if (isCreatingIssue) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.brand_github),
                                contentDescription = stringResource(R.string.action_report_github),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val contactLine =
                                if (contactEmail.isNotBlank()) "Contact Email: $contactEmail\n" else ""
                            val body = buildString {
                                if (contactLine.isNotBlank()) {
                                    append(contactLine)
                                }
                                if (feedbackMessage.isNotBlank()) {
                                    append("Feedback:\n${BugReportIssueFormatter.describe(feedbackMessage, sourceFeature)}\n\n")
                                }
                                append("Device Info:\n$deviceInfoString\n\n")
                                if (selectedCrashReport != null) {
                                    append("Crash Log (${selectedCrashReport?.name}):\n")
                                    try {
                                        append(selectedCrashReport?.readText()?.trim())
                                    } catch (e: Exception) {
                                        append("Failed to read crash log: ${e.message}")
                                    }
                                    append("\n\n")
                                }
                            }
                            val intent =
                                Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("mail@sameerasw.com"))
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        context.getString(R.string.bug_report_email_subject),
                                    )
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.error_no_email_app),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = ButtonGroupDefaults.connectedMiddleButtonShapes().shape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_mail_24),
                            contentDescription = stringResource(R.string.action_report_email),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            try {
                                val timeStamp =
                                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val shareDir =
                                    File(context.cacheDir, "reports").apply { mkdirs() }
                                val shareFile =
                                    File(shareDir, "essentials_report_$timeStamp.txt")
                                shareFile.writeText(buildFullReportText())

                                val uri =
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        shareFile,
                                    )
                                val shareIntent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "Essentials Bug Report - $timeStamp",
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            } catch (e: Exception) {
                                Toast
                                    .makeText(
                                        context,
                                        e.localizedMessage ?: context.getString(R.string.bug_report_share_failed),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = ButtonGroupDefaults.connectedMiddleButtonShapes().shape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_share_24),
                            contentDescription = stringResource(R.string.action_share),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val timeStamp =
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            saveReportLauncher.launch("essentials_report_$timeStamp.txt")
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_download_24),
                            contentDescription = stringResource(R.string.action_save_report),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private const val MAX_PREFILL_CONTEXT_LENGTH = 6000

@Composable
private fun BugReportDropdownField(
    label: String,
    value: String?,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "BugReportDropdownChevron",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                )
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        // The read-only field swallows taps, so an overlay opens the menu instead.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.large)
                    .clickable {
                        HapticUtil.performUIHaptic(view)
                        expanded = true
                    },
        )
        SegmentedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                SegmentedDropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    trailingIcon =
                        if (option == value) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_check_24),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}
