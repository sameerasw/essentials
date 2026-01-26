package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.ui.components.text.SimpleMarkdown
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepoBottomSheet(
    viewModel: AppUpdatesViewModel,
    onDismissRequest: () -> Unit,
    onTrackClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchQuery by viewModel.searchQuery
    val isSearching by viewModel.isSearching
    val searchResult by viewModel.searchResult
    val latestRelease by viewModel.latestRelease
    val errorMessage by viewModel.errorMessage
    val readmeContent by viewModel.readmeContent

    var showReleaseNotes by remember { mutableStateOf(false) }
    var showReadme by remember { mutableStateOf(false) }
    
    // APK selection state
    var selectedApkName by remember { mutableStateOf("Auto") }

    if (showReleaseNotes && latestRelease != null) {
        val updateInfo = UpdateInfo(
            versionName = latestRelease!!.tagName,
            releaseNotes = latestRelease!!.body ?: "",
            downloadUrl = latestRelease!!.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl ?: "",
            releaseUrl = latestRelease!!.htmlUrl,
            isUpdateAvailable = false
        )
        UpdateBottomSheet(
            updateInfo = updateInfo,
            isChecking = false,
            onDismissRequest = { showReleaseNotes = false }
        )
    }

    if (showReadme && readmeContent != null) {
        ModalBottomSheet(
            onDismissRequest = { showReadme = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "README",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SimpleMarkdown(content = readmeContent!!)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.action_add_repo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (searchResult == null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.prompt_enter_repo_url)) },
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { viewModel.searchRepo() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSearching && searchQuery.isNotBlank()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.action_search))
                        }
                    }
                }
            } else {
                // Repo Info & latest release Container
                RoundedCardContainer {
                    // Repo Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = searchResult!!.owner.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = searchResult!!.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (searchResult!!.description != null) {
                                Text(
                                    text = searchResult!!.description!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stars in rounded primary container
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.round_star_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.label_stars, searchResult!!.stars),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (readmeContent != null) {
                                    TextButton(
                                        onClick = { showReadme = true }
                                    ) {
                                        Icon(painterResource(id = R.drawable.rounded_mobile_text_2_24), null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.action_view_readme))
                                    }
                                }
                            }
                        }
                    }

                    // Release Card
                    if (latestRelease != null) {
                        Surface(
                            onClick = { showReleaseNotes = true },
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.label_latest_release),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = latestRelease!!.name ?: latestRelease!!.tagName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${latestRelease!!.tagName} • ${formatRelativeDate(latestRelease!!.publishedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                // APKs section
                if (latestRelease != null) {
                    val apkAssets = remember(latestRelease) {
                        latestRelease!!.assets.filter { it.name.endsWith(".apk") }
                    }
                    
                    if (apkAssets.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Found APKs",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, start = 12.dp)
                            )
                            
                            RoundedCardContainer {
                                val options = remember(apkAssets) {
                                    listOf("Auto") + apkAssets.map { it.name }
                                }
                                
                                options.forEach { option ->
                                    Surface(
                                        onClick = { selectedApkName = option },
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedApkName == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedApkName == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            RadioButton(
                                                selected = (selectedApkName == option),
                                                onClick = { selectedApkName = option }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearSearch() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = onTrackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_track))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatRelativeDate(githubDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(githubDate) ?: return githubDate
        val now = System.currentTimeMillis()
        val diff = now - date.time

        when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 2592000000L -> "${diff / 86400000}d ago"
            diff < 31536000000L -> "${diff / 2592000000L}mo ago"
            else -> "${diff / 31536000000L}y ago"
        }
    } catch (e: Exception) {
        githubDate
    }
}
