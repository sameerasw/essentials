package com.sameerasw.essentials

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.cards.TrackedRepoCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class AppUpdatesActivity : FragmentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val context = LocalContext.current
            val view = androidx.compose.ui.platform.LocalView.current
            val viewModel: MainViewModel = viewModel()
            val updatesViewModel: AppUpdatesViewModel = viewModel()
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val trackedRepos by updatesViewModel.trackedRepos
            val isLoading by updatesViewModel.isLoading
            val refreshingRepoIds by updatesViewModel.refreshingRepoIds
            val updateProgress by updatesViewModel.updateProgress

            val animatedProgress by animateFloatAsState(
                targetValue = if (updateProgress > 0) updateProgress else 0f,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "Progress"
            )

            var showAddRepoSheet by remember { mutableStateOf(false) }
            val errorMessage by updatesViewModel.errorMessage

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            updatesViewModel.exportTrackedRepos(context, outputStream)
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_export_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_export_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            if (updatesViewModel.importTrackedRepos(context, inputStream)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_import_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_import_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_import_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    }
                }
            }

            LaunchedEffect(errorMessage) {
                if (errorMessage != null && !showAddRepoSheet) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    updatesViewModel.clearError()
                }
            }
            var repoToShowReleaseNotesFullName by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val scrollBehavior =
                    TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())


                if (showAddRepoSheet) {
                    AddRepoBottomSheet(
                        viewModel = updatesViewModel,
                        onDismissRequest = {
                            showAddRepoSheet = false
                            updatesViewModel.clearSearch()
                        },
                        onTrackClick = {
                            showAddRepoSheet = false
                            updatesViewModel.clearSearch()
                        }
                    )
                }

                if (repoToShowReleaseNotesFullName != null) {
                    val repo = trackedRepos.find { it.fullName == repoToShowReleaseNotesFullName }
                    if (repo != null) {
                        val isNotesLoading = repo.latestReleaseBody.isNullOrBlank()
                        UpdateBottomSheet(
                            updateInfo = com.sameerasw.essentials.domain.model.UpdateInfo(
                                versionName = repo.latestTagName,
                                releaseNotes = repo.latestReleaseBody ?: "",
                                downloadUrl = repo.downloadUrl ?: "",
                                releaseUrl = repo.latestReleaseUrl ?: "",
                                isUpdateAvailable = repo.isUpdateAvailable
                            ),
                            isChecking = isNotesLoading,
                            onDismissRequest = { repoToShowReleaseNotesFullName = null }
                        )
                    } else {
                        repoToShowReleaseNotesFullName = null
                    }
                }

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(
                        0,
                        0,
                        0,
                        0
                    ),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = stringResource(R.string.tab_app_updates_title),
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        HapticUtil.performMediumHaptic(view)
                                        updatesViewModel.checkForUpdates(context)
                                    },
                                    enabled = refreshingRepoIds.isEmpty(),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    if (refreshingRepoIds.isNotEmpty()) {
                                        CircularWavyProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        androidx.compose.material3.Icon(
                                            painter = painterResource(id = R.drawable.rounded_refresh_24),
                                            contentDescription = stringResource(R.string.action_refresh),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                HapticUtil.performMediumHaptic(view)
                                showAddRepoSheet = true
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            androidx.compose.material3.Icon(
                                painter = painterResource(id = R.drawable.rounded_add_24),
                                contentDescription = stringResource(R.string.action_add_repo)
                            )
                        }
                    }
                ) { innerPadding ->
                    LaunchedEffect(Unit) {
                        updatesViewModel.loadTrackedRepos(context)
                    }

                    val pending =
                        trackedRepos.filter { it.isUpdateAvailable && it.mappedPackageName != null }
                            .sortedByDescending { it.publishedAt }
                    val upToDate =
                        trackedRepos.filter { !it.isUpdateAvailable && it.mappedPackageName != null }
                            .sortedByDescending { it.publishedAt }
                    val notInstalled = trackedRepos.filter { it.mappedPackageName == null }

                    if (isLoading && trackedRepos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            LoadingIndicator()
                        }
                    } else if (trackedRepos.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.msg_no_repos_tracked),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            androidx.compose.material3.Text(
                                text = stringResource(R.string.label_apps),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ImportExportButtons(
                                view = view,
                                exportLauncher = exportLauncher,
                                importLauncher = importLauncher
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                top = innerPadding.calculateTopPadding() + 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 100.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Pending Section
                            if (pending.isNotEmpty()) {
                                item {
                                    androidx.compose.material3.Text(
                                        text = "${stringResource(R.string.label_pending)} (${pending.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    RoundedCardContainer {
                                        pending.forEach { repo ->
                                            val isInstalling =
                                                updatesViewModel.installingRepoId.value == repo.fullName
                                            TrackedRepoCard(
                                                repo = repo,
                                                isLoading = refreshingRepoIds.contains(repo.fullName),
                                                installStatus = if (isInstalling) updatesViewModel.installStatus.value else null,
                                                downloadProgress = if (isInstalling) updatesViewModel.updateProgress.value else 0f,
                                                onClick = {
                                                    updatesViewModel.prepareEdit(context, repo)
                                                    showAddRepoSheet = true
                                                },
                                                onActionClick = {
                                                    updatesViewModel.downloadAndInstall(
                                                        context,
                                                        repo
                                                    )
                                                },
                                                onDeleteClick = {
                                                    updatesViewModel.untrackRepo(
                                                        context,
                                                        repo.fullName
                                                    )
                                                },
                                                onShowReleaseNotes = {
                                                    repoToShowReleaseNotesFullName = repo.fullName
                                                    updatesViewModel.fetchReleaseNotesIfNeeded(
                                                        context,
                                                        repo
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Up-to-date Section
                            if (upToDate.isNotEmpty()) {
                                item {
                                    androidx.compose.material3.Text(
                                        text = "${stringResource(R.string.label_up_to_date)} (${upToDate.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    RoundedCardContainer {
                                        upToDate.forEach { repo ->
                                            val isInstalling =
                                                updatesViewModel.installingRepoId.value == repo.fullName
                                            TrackedRepoCard(
                                                repo = repo,
                                                isLoading = refreshingRepoIds.contains(repo.fullName),
                                                installStatus = if (isInstalling) updatesViewModel.installStatus.value else null,
                                                downloadProgress = if (isInstalling) updatesViewModel.updateProgress.value else 0f,
                                                onClick = {
                                                    updatesViewModel.prepareEdit(context, repo)
                                                    showAddRepoSheet = true
                                                },
                                                onActionClick = {
                                                    if (repo.isUpdateAvailable) {
                                                        updatesViewModel.downloadAndInstall(
                                                            context,
                                                            repo
                                                        )
                                                    } else {
                                                        repoToShowReleaseNotesFullName =
                                                            repo.fullName
                                                        updatesViewModel.fetchReleaseNotesIfNeeded(
                                                            context,
                                                            repo
                                                        )
                                                    }
                                                },
                                                onDeleteClick = {
                                                    updatesViewModel.untrackRepo(
                                                        context,
                                                        repo.fullName
                                                    )
                                                },
                                                onShowReleaseNotes = {
                                                    repoToShowReleaseNotesFullName = repo.fullName
                                                    updatesViewModel.fetchReleaseNotesIfNeeded(
                                                        context,
                                                        repo
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Not Installed Section
                            if (notInstalled.isNotEmpty()) {
                                item {
                                    androidx.compose.material3.Text(
                                        text = "${stringResource(R.string.label_not_installed)} (${notInstalled.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                item {
                                    RoundedCardContainer {
                                        notInstalled.forEach { repo ->
                                            val isInstalling =
                                                updatesViewModel.installingRepoId.value == repo.fullName
                                            TrackedRepoCard(
                                                repo = repo,
                                                isLoading = refreshingRepoIds.contains(repo.fullName),
                                                installStatus = if (isInstalling) updatesViewModel.installStatus.value else null,
                                                downloadProgress = if (isInstalling) updatesViewModel.updateProgress.value else 0f,
                                                onClick = {
                                                    updatesViewModel.prepareEdit(context, repo)
                                                    showAddRepoSheet = true
                                                },
                                                onActionClick = {
                                                    updatesViewModel.downloadAndInstall(
                                                        context,
                                                        repo
                                                    )
                                                },
                                                onDeleteClick = {
                                                    updatesViewModel.untrackRepo(
                                                        context,
                                                        repo.fullName
                                                    )
                                                },
                                                onShowReleaseNotes = {
                                                    repoToShowReleaseNotesFullName = repo.fullName
                                                    updatesViewModel.fetchReleaseNotesIfNeeded(
                                                        context,
                                                        repo
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Apps Section
                            item {
                                androidx.compose.material3.Text(
                                    text = stringResource(R.string.label_apps),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ImportExportButtons(
                                    view = view,
                                    exportLauncher = exportLauncher,
                                    importLauncher = importLauncher
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportExportButtons(
    view: android.view.View,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                HapticUtil.performUIHaptic(view)
                val timeStamp = SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())
                exportLauncher.launch("essentials_updates_$timeStamp.json")
            },
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.rounded_arrow_warm_up_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_export))
        }

        Button(
            onClick = {
                HapticUtil.performUIHaptic(view)
                importLauncher.launch(arrayOf("application/json"))
            },
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.rounded_arrow_cool_down_24),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_import))
        }
    }
}