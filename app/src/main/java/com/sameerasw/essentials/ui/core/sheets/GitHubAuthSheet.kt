/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: GitHubAuthSheet.kt
 * Description: Reusable core UI component for GitHubAuthSheet.kt.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.AuthState
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GitHubAuthSheet(
    viewModel: GitHubAuthViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val authState by viewModel.authState
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    EssentialsBottomSheet(
        onDismissRequest = {
            viewModel.cancelAuthFlow()
            onDismissRequest()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.action_sign_in_github,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            when (val state = authState) {
                is AuthState.Idle -> {
                    TranslatableText(
                        stringResId = R.string.auth_sign_in_rationale,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.startAuthFlow()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TranslatableText(R.string.action_start_sign_in)
                    }
                }

                is AuthState.Loading -> {
                    LoadingIndicator()
                    TranslatableText(
                        stringResId = R.string.auth_requesting_code,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                is AuthState.CodeReceived -> {
                    TranslatableText(
                        stringResId = R.string.auth_copy_code_instruction,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    SelectionContainer {
                        Text(
                            text = state.userCode,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 4.sp,
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            clipboardManager.setText(AnnotatedString(state.userCode))
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_content_copy_24),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        TranslatableText(R.string.auth_copy_code)
                    }

                    TranslatableText(
                        stringResId = R.string.auth_paste_code_instruction,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            uriHandler.openUri(state.verificationUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TranslatableText(R.string.auth_open_login_page)
                    }

                    TranslatableText(
                        stringResId = R.string.auth_waiting_for_authorization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is AuthState.Authenticated -> {
                    viewModel.saveToken(context, state.token)
                    LaunchedEffect(Unit) {
                        onDismissRequest()
                    }
                }

                is AuthState.Error -> {
                    TranslatableText(
                        stringResId = state.messageRes,
                        formatArgs = listOf(state.formatArg.orEmpty()),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.startAuthFlow() // Retry
                        },
                    ) {
                        TranslatableText(R.string.action_retry)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
