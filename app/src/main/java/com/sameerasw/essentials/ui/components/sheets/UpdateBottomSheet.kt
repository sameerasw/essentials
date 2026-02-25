package com.sameerasw.essentials.ui.components.sheets

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.text.SimpleMarkdown

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateBottomSheet(
    updateInfo: UpdateInfo?,
    isChecking: Boolean,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isChecking || updateInfo == null) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (updateInfo.isUpdateAvailable) R.drawable.rounded_mobile_arrow_down_24 else R.drawable.rounded_mobile_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (updateInfo.isUpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )

                    Text(
                        text = if (updateInfo.isUpdateAvailable) stringResource(R.string.update_available_title) else stringResource(
                            R.string.status_up_to_date
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val isPreRelease = remember(updateInfo.versionName) {
                        val v = updateInfo.versionName.lowercase()
                        v.contains("beta") || v.contains("alpha") || v.contains("rc") || v.contains(
                            "pre"
                        )
                    }

                    if (isPreRelease) {
                        RoundedCardContainer {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_mobile_code_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = stringResource(R.string.warning_pre_release),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (updateInfo.releaseNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.release_notes_format,
                                    updateInfo.versionName
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            RoundedCardContainer {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    SimpleMarkdown(
                                        content = updateInfo.releaseNotes,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (updateInfo.releaseUrl.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_VIEW, updateInfo.releaseUrl.toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.brand_github),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_view_on_github))
                        }
                    }

                    if (updateInfo.isUpdateAvailable && updateInfo.downloadUrl.isNotEmpty()) {
                        Button(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_download_apk))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
