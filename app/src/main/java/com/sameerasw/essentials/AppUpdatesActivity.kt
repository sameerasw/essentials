package com.sameerasw.essentials

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.domain.model.UpdateInfo
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.sameerasw.essentials.utils.HapticUtil
import coil.compose.AsyncImage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.ui.components.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.viewmodels.MainViewModel

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

            var showAddRepoSheet by remember { mutableStateOf(false) }
            val errorMessage by updatesViewModel.errorMessage
            
            LaunchedEffect(errorMessage) {
                if (errorMessage != null && !showAddRepoSheet) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    updatesViewModel.clearError()
                }
            }

            var repoToShowReleaseNotes by remember { mutableStateOf<com.sameerasw.essentials.domain.model.TrackedRepo?>(null) }

            LaunchedEffect(Unit) {
                viewModel.check(context)
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                
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

                if (repoToShowReleaseNotes != null) {
                    UpdateBottomSheet(
                        updateInfo = com.sameerasw.essentials.domain.model.UpdateInfo(
                            versionName = repoToShowReleaseNotes!!.latestTagName,
                            releaseNotes = if (!repoToShowReleaseNotes!!.latestReleaseBody.isNullOrBlank()) 
                                repoToShowReleaseNotes!!.latestReleaseBody!! 
                            else 
                                "Loading release notes...",
                            downloadUrl = repoToShowReleaseNotes!!.downloadUrl ?: "",
                            releaseUrl = repoToShowReleaseNotes!!.latestReleaseUrl ?: "",
                            isUpdateAvailable = repoToShowReleaseNotes!!.isUpdateAvailable
                        ),
                        isChecking = false,
                        onDismissRequest = { repoToShowReleaseNotes = null }
                    )
                }

                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = stringResource(R.string.tab_app_updates_title),
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior
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
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_add_24),
                                contentDescription = stringResource(R.string.action_add_repo)
                            )
                        }
                    }
                ) { innerPadding ->
                    val trackedRepos by updatesViewModel.trackedRepos
                    val isLoading by updatesViewModel.isLoading
                    
                    val pending = trackedRepos.filter { it.isUpdateAvailable && it.mappedPackageName != null }
                        .sortedByDescending { it.publishedAt }
                    val upToDate = trackedRepos.filter { !it.isUpdateAvailable && it.mappedPackageName != null }
                        .sortedByDescending { it.publishedAt }
                    val notInstalled = trackedRepos.filter { it.mappedPackageName == null }

                    LaunchedEffect(Unit) {
                        updatesViewModel.loadTrackedRepos(context)
                    }

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isLoading) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LoadingIndicator()
                            }
                        }

                        // Pending Section
                        if (pending.isNotEmpty()) {
                            CategorySection(
                                title = "Pending",
                                count = pending.size,
                                repos = pending,
                                onRepoClick = { repo ->
                                    updatesViewModel.prepareEdit(context, repo)
                                    showAddRepoSheet = true
                                },
                                onShowReleaseNotes = { repo ->
                                    repoToShowReleaseNotes = repo
                                    updatesViewModel.fetchReleaseNotesIfNeeded(context, repo)
                                }
                            )
                        }

                        // Up-to-date Section
                        if (upToDate.isNotEmpty()) {
                            CategorySection(
                                title = "Up-to-date",
                                count = upToDate.size,
                                repos = upToDate,
                                onRepoClick = { repo ->
                                    updatesViewModel.prepareEdit(context, repo)
                                    showAddRepoSheet = true
                                },
                                onShowReleaseNotes = { repo ->
                                    repoToShowReleaseNotes = repo
                                    updatesViewModel.fetchReleaseNotesIfNeeded(context, repo)
                                }
                            )
                        }

                        // Not Installed Section
                        if (notInstalled.isNotEmpty()) {
                            CategorySection(
                                title = "Not installed",
                                count = notInstalled.size,
                                repos = notInstalled,
                                onRepoClick = { repo ->
                                    updatesViewModel.prepareEdit(context, repo)
                                    showAddRepoSheet = true
                                },
                                onShowReleaseNotes = { repo ->
                                    repoToShowReleaseNotes = repo
                                    updatesViewModel.fetchReleaseNotesIfNeeded(context, repo)
                                }
                            )
                        }
                        
                        if (trackedRepos.isEmpty() && !isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    painter = painterResource(id = R.drawable.rounded_apps_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                androidx.compose.material3.Text(
                                    text = "No repositories tracked yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    title: String,
    count: Int,
    repos: List<com.sameerasw.essentials.domain.model.TrackedRepo>,
    onRepoClick: (com.sameerasw.essentials.domain.model.TrackedRepo) -> Unit,
    onShowReleaseNotes: (com.sameerasw.essentials.domain.model.TrackedRepo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            repos.forEach { repo ->
                TrackedRepoCard(
                    repo = repo,
                    onClick = { onRepoClick(repo) },
                    onShowReleaseNotes = { onShowReleaseNotes(repo) }
                )
            }
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

@Composable
fun TrackedRepoCard(
    repo: com.sameerasw.essentials.domain.model.TrackedRepo,
    onClick: () -> Unit,
    onShowReleaseNotes: () -> Unit = {}
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.material3.Card(
        onClick = {
            com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
            onClick()
        },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Icon + Badge
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(56.dp)
            ) {
                // Main Image (App Icon or Android Icon)
                if (repo.mappedPackageName != null) {
                    val appIcon = remember(repo.mappedPackageName) {
                        try {
                            context.packageManager.getApplicationIcon(repo.mappedPackageName)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (appIcon != null) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.graphics.painter.BitmapPainter(
                                appIcon.toBitmap().asImageBitmap()
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            painter = painterResource(id = R.drawable.rounded_adb_24),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.rounded_adb_24),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Avatar Badge (User Profile)
                Surface(
                    modifier = Modifier
                        .size(24.dp)
                        .align(androidx.compose.ui.Alignment.BottomEnd),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceBright)
                ) {
                    AsyncImage(
                        model = repo.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
            
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                val isInstalled = repo.mappedPackageName != null
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = if (isInstalled) repo.mappedAppName ?: repo.name else repo.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isInstalled) {
                        androidx.compose.material3.Text(
                            text = " ${repo.latestTagName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                androidx.compose.material3.Text(
                    text = if (isInstalled) repo.fullName else "No app linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isInstalled) {
                    androidx.compose.material3.Text(
                        text = "Updated ${formatRelativeDate(repo.publishedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            if (repo.isUpdateAvailable || repo.mappedPackageName == null) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil.performMediumHaptic(view)
                        // Action for download/update would go here
                    }
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(
                            id = if (repo.isUpdateAvailable) R.drawable.rounded_downloading_24 else R.drawable.rounded_download_24
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                androidx.compose.material3.IconButton(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil.performUIHaptic(view)
                        onShowReleaseNotes()
                    }
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.rounded_release_alert_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}