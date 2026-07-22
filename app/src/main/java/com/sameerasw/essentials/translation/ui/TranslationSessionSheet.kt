package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.layout.Spacer
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.GitHubRepository
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.model.TranslationEdit
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSessionSheet(
    onDismissRequest: () -> Unit,
    onNeedLogin: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val settingsRepository = remember { SettingsRepository(context) }
    val gitHubRepository = remember { GitHubRepository() }
    val currentUser = remember { settingsRepository.getGitHubUser() }

    val edits = remember { mutableStateListOf<TranslationEdit>().apply { addAll(TranslationManager.session.edits) } }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSubmitted by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (successSubmitted) {
                TranslationManager.discardSession()
            }
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_translated_texts),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${edits.size} edit(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))


                Row {
                    OutlinedButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            TranslationManager.discardSession()
                            edits.clear()
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.translation_discard))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val token = settingsRepository.getGitHubToken()
                            if (token == null || currentUser == null) {
                                onNeedLogin()
                                return@Button
                            }

                            isSubmitting = true
                            errorMessage = null
                            scope.launch {
                                val jsonPayload = TranslationManager.session.toJsonPayload()
                                val success = gitHubRepository.triggerWorkflowDispatch(
                                    token = token,
                                    owner = "sameerasw",
                                    repo = "essentials",
                                    workflowFile = "apply-translations.yml",
                                    ref = "main",
                                    inputs = mapOf(
                                        "translations_json" to jsonPayload,
                                        "contributor" to currentUser.login,
                                        "contributor_name" to (currentUser.name ?: currentUser.login),
                                        "user_token" to token
                                    )
                                )
                                isSubmitting = false
                                if (success) {
                                    successSubmitted = true
                                    TranslationManager.discardSession()
                                } else {
                                    errorMessage = context.getString(R.string.translation_submit_error)
                                }
                            }
                        },
                        enabled = !isSubmitting && edits.isNotEmpty() && !successSubmitted
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.translation_submit))
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (successSubmitted) {
                Text(
                    text = stringResource(R.string.translation_submitted),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Edits List wrapped in RoundedCardContainer
            if (edits.isNotEmpty()) {
                RoundedCardContainer {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = edits,
                            key = { "${it.key}_${it.locale}" }
                        ) { edit ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Key: ${edit.key} (${edit.locale.uppercase()})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            text = "Original: ${edit.originalValue}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "New: ${edit.newValue}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                trailingContent = if (!successSubmitted) {
                                    {
                                        IconButton(
                                            onClick = {
                                                HapticUtil.performUIHaptic(view)
                                                TranslationManager.removeEdit(edit.key, edit.locale)
                                                edits.remove(edit)
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_delete_24),
                                                contentDescription = "Remove edit",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else null,
                                modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.extraSmall)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No pending edits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
