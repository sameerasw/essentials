package com.sameerasw.essentials.ui.components.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var fullReportJson by remember { mutableStateOf("") }
    var deviceInfoString by remember { mutableStateOf("") }
    var isShareLogsEnabled by remember { mutableStateOf(true) }
    var isRawReportExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val jsonString = viewModel.generateBugReport(context)
        fullReportJson = jsonString
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

    // Prepare content to share based on checkbox
    val contentToShare: String by remember(fullReportJson, isShareLogsEnabled) {
        mutableStateOf(
            if (isShareLogsEnabled) {
                fullReportJson
            } else {
                try {
                    val jsonObject = JSONObject(fullReportJson)
                    // If unchecked, user requested "only device info"
                    val keys = jsonObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "device_info") {
                            keys.remove()
                        }
                    }
                    jsonObject.toString(4)
                } catch (e: Exception) {
                    "Error filtering report"
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.bug_report_title),
                style = MaterialTheme.typography.headlineMedium
            )

            // Device Info
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.bug_report_device_info),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = deviceInfoString,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Raw Report Collapsible - Only show if sharing logs is enabled
            if (isShareLogsEnabled) {
                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRawReportExpanded = !isRawReportExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.bug_report_raw_json),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                painter = painterResource(
                                    if (isRawReportExpanded) R.drawable.rounded_keyboard_arrow_up_24
                                    else R.drawable.rounded_keyboard_arrow_down_24
                                ),
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(
                            visible = isRawReportExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                )
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(
                                        text = contentToShare,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Options
            RoundedCardContainer {
                IconToggleItem(
                    iconRes = R.drawable.rounded_bug_report_24,
                    title = stringResource(R.string.bug_report_option_share_logs),
                    description = stringResource(R.string.bug_report_option_share_logs_desc),
                    isChecked = isShareLogsEnabled,
                    onCheckedChange = { isShareLogsEnabled = it }
                )
            }

            Text(
                text = stringResource(R.string.bug_report_send_via),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // GitHub
                Button(
                    onClick = {
                        val encodedBody = Uri.encode(contentToShare)
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/sameerasw/essentials/issues/new?body=$encodedBody")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.brand_github),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_report_github))
                }

                // Email
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("mail@sameerasw.com"))
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.bug_report_email_subject)
                            )
                            putExtra(Intent.EXTRA_TEXT, contentToShare)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_no_email_app),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_mail_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_report_email))
                }

                // Clipboard
                Button(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bug Report", contentToShare)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_bug_report_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_content_copy_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_copy_clipboard))
                }
            }
        }
    }
}
