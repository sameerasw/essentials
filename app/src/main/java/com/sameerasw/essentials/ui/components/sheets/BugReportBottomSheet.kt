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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import io.sentry.Sentry
import io.sentry.protocol.Feedback
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var deviceInfoString by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

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

            // Feedback Input
            OutlinedTextField(
                value = feedbackMessage,
                onValueChange = { feedbackMessage = it },
                label = { Text(stringResource(R.string.bug_report_feedback_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = MaterialTheme.shapes.large,
                minLines = 3
            )

            // Contact Email Input
            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text(stringResource(R.string.bug_report_contact_email_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sentry Feedback
                Button(
                    onClick = {
                        val feedback = Feedback(feedbackMessage)
                        if (contactEmail.isNotBlank()) {
                            feedback.contactEmail = contactEmail
                        }
                        Sentry.captureFeedback(feedback)
                        Toast.makeText(context, R.string.msg_feedback_sent, Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = feedbackMessage.isNotBlank()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_send_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_send_feedback))
                }

                Text(
                    text = stringResource(R.string.label_alternatively),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)
                )

                // GitHub
                OutlinedButton(
                    onClick = {
                        val body = "Feedback:\n$feedbackMessage\n\nDevice Info:\n$deviceInfoString\n\n"
                        val encodedBody = Uri.encode(body)
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
                OutlinedButton(
                    onClick = {
                        val contactLine = if (contactEmail.isNotBlank()) "Contact Email: $contactEmail\n" else ""
                        val body = "${contactLine}Feedback:\n$feedbackMessage\n\nDevice Info:\n$deviceInfoString\n\n"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("mail@sameerasw.com"))
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.bug_report_email_subject)
                            )
                            putExtra(Intent.EXTRA_TEXT, body)
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
            }
        }
    }
}
