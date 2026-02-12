package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.UpdateInfo
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.text.SimpleMarkdown
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.TimeUtil
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddRepoBottomSheet(
    viewModel: AppUpdatesViewModel,
    onDismissRequest: () -> Unit,
    onTrackClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val searchQuery by viewModel.searchQuery
    val isSearching by viewModel.isSearching
    val searchResult by viewModel.searchResult
    val latestRelease by viewModel.latestRelease
    val errorMessage by viewModel.errorMessage
    val readmeContent by viewModel.readmeContent
    val selectedApp by viewModel.selectedApp
    val allowPreReleases by viewModel.allowPreReleases
    val notificationsEnabled by viewModel.notificationsEnabled

    var showReleaseNotes by remember { mutableStateOf(false) }
    var showReadme by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    
    // APK selection state
    // APK selection state
    var selectedApkName by remember { mutableStateOf("Auto") }

    // Search haptic feedback
    LaunchedEffect(isSearching) {
        if (isSearching) {
            while (true) {
                HapticUtil.performLightHaptic(view)
                delay(300)
            }
        }
    }

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
            onDismissRequest = { 
                HapticUtil.performUIHaptic(view)
                showReadme = false 
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.label_readme),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                SimpleMarkdown(content = readmeContent!!)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAppPicker) {
        SingleAppSelectionSheet(
            onDismissRequest = { showAppPicker = false },
            onAppSelected = { app ->
                viewModel.onAppSelected(app)
            }
        )
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
            val isTracked = remember(searchResult, viewModel.trackedRepos.value) {
                viewModel.trackedRepos.value.any { it.fullName == searchResult?.fullName }
            }

            Text(
                text = if (isTracked) stringResource(R.string.action_edit_repo) else stringResource(R.string.action_add_repo),
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
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = {
                            HapticUtil.performMediumHaptic(view)
                            viewModel.searchRepo(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSearching && searchQuery.isNotBlank()
                    ) {
                        if (isSearching) {
                            LoadingIndicator(modifier = Modifier.size(24.dp))
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
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            showReadme = true 
                                        }
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
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showReleaseNotes = true 
                            },
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
                                        text = "${latestRelease!!.tagName} • ${TimeUtil.formatRelativeDate(latestRelease!!.publishedAt, context)}",
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
                                text = stringResource(R.string.label_found_apks),
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
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            selectedApkName = option 
                                        },
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
                                                text = if (option == "Auto") stringResource(R.string.label_auto) else option,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedApkName == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedApkName == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            RadioButton(
                                                selected = (selectedApkName == option),
                                                onClick = {
                                                    HapticUtil.performUIHaptic(view)
                                                    selectedApkName = option 
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Installed app section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_installed_app),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, start = 12.dp)
                    )
                    
                    RoundedCardContainer {
                        // Not installed option
                        Surface(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                viewModel.onAppSelected(null) 
                            },
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
                                    text = stringResource(R.string.label_not_installed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedApp == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedApp == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = selectedApp == null,
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.onAppSelected(null) 
                                    }
                                )
                            }
                        }
                        
                        // Pick app / Selected app option
                        Surface(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showAppPicker = true 
                            },
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (selectedApp != null) {
                                        Image(
                                            bitmap = selectedApp!!.icon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                        )
                                        Column {
                                            Text(
                                                text = selectedApp!!.appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = selectedApp!!.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_apps_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(R.string.action_pick_app),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = selectedApp != null,
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        showAppPicker = true 
                                    }
                                )
                            }
                        }
                    }
                // Options section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_options),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, start = 12.dp)
                    )
                    
                    RoundedCardContainer {
                        // Pre-releases option
                        Surface(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setAllowPreReleases(!allowPreReleases)
                            },
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
                                    text = stringResource(R.string.option_allow_prereleases),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface 
                                )
                                androidx.compose.material3.Switch(
                                    checked = allowPreReleases,
                                    onCheckedChange = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.setAllowPreReleases(it)
                                    }
                                )
                            }
                        }
                        
                        // Notifications option
                        Surface(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                viewModel.setNotificationsEnabled(!notificationsEnabled)
                            },
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
                                    text = stringResource(R.string.option_notifications),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface 
                                )
                                androidx.compose.material3.Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = {
                                        HapticUtil.performUIHaptic(view)
                                        viewModel.setNotificationsEnabled(it)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val isTracked = remember(searchResult, viewModel.trackedRepos.value) {
                    viewModel.trackedRepos.value.any { it.fullName == searchResult?.fullName }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTracked) {
                        OutlinedButton(
                            onClick = {
                                HapticUtil.performMediumHaptic(view)
                                viewModel.untrackRepo(context, searchResult!!.fullName)
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.action_untrack))
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                viewModel.clearSearch() 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                    
                    Button(
                        onClick = {
                            HapticUtil.performMediumHaptic(view)
                            viewModel.trackRepo(context, selectedApkName)
                            onTrackClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTracked) stringResource(R.string.action_save) else stringResource(R.string.action_track))
                    }
                }
            }
        }
    }
}

