/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: YourAndroidActivity.kt
 * Description: Activity component for YourAndroidActivity.kt.
 */

package com.sameerasw.essentials.ui.activities

import com.sameerasw.essentials.ui.navigation.AppRoutes
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.utils.DeviceImageMapper
import com.sameerasw.essentials.ui.components.rememberPullTitleHeaderState
import com.sameerasw.essentials.ui.components.PullTitleHeader
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.gson.GsonBuilder
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppBackupItem
import com.sameerasw.essentials.domain.model.AppListBackup
import com.sameerasw.essentials.domain.model.DeviceBackupInfo
import com.sameerasw.essentials.ui.components.AppsActionButtons
import com.sameerasw.essentials.ui.components.DeviceHeroCard
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.ImportExportButtons
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.TrackedRepoCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.ui.core.sheets.GitHubAuthSheet
import com.sameerasw.essentials.ui.core.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.DeviceInfo
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YourAndroidViewModel : ViewModel() {
    var hasRunStartupAnimation = false
    val isExportingAppList = mutableStateOf(false)

    fun exportAppList(context: Context, uri: Uri) {
        viewModelScope.launch {
            isExportingAppList.value = true
            val success = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                    val downloadedApps = installedPackages.filter { pkgInfo ->
                        val appInfo = pkgInfo.applicationInfo
                        if (appInfo != null) {
                            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                            !isSystem || isUpdatedSystem
                        } else {
                            false
                        }
                    }

                    val backupApps = downloadedApps.map { pkgInfo ->
                        val appInfo = pkgInfo.applicationInfo
                        val appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkgInfo.packageName
                        val installerPackage = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                pm.getInstallSourceInfo(pkgInfo.packageName).installingPackageName
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getInstallerPackageName(pkgInfo.packageName)
                            }
                        } catch (e: Exception) {
                            null
                        }

                        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            appInfo?.minSdkVersion
                        } else {
                            null
                        }
                        val targetSdk = appInfo?.targetSdkVersion

                        AppBackupItem(
                            name = appLabel,
                            packageName = pkgInfo.packageName,
                            versionName = pkgInfo.versionName ?: "",
                            versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
                            firstInstallTime = pkgInfo.firstInstallTime,
                            lastUpdateTime = pkgInfo.lastUpdateTime,
                            installer = installerPackage,
                            minSdkVersion = minSdk,
                            targetSdkVersion = targetSdk,
                        )
                    }.sortedBy { it.name.lowercase(Locale.getDefault()) }

                    val now = System.currentTimeMillis()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val backup = AppListBackup(
                        device = DeviceBackupInfo(
                            manufacturer = Build.MANUFACTURER,
                            brand = Build.BRAND,
                            model = Build.MODEL,
                            device = Build.DEVICE,
                            product = Build.PRODUCT,
                            androidVersion = Build.VERSION.RELEASE,
                            sdkInt = Build.VERSION.SDK_INT,
                            exportTimestamp = now,
                            exportDate = dateFormat.format(Date(now)),
                        ),
                        appCount = backupApps.size,
                        apps = backupApps,
                    )

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                            val gson = GsonBuilder().setPrettyPrinting().create()
                            gson.toJson(backup, writer)
                        }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            isExportingAppList.value = false
            Toast.makeText(
                context,
                if (success) R.string.msg_backup_app_list_success else R.string.msg_backup_app_list_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}

class YourAndroidActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRoutes.redirectToMain(this)
    }
}

@Composable
fun YourAndroidContent(
    deviceInfo: DeviceInfo,
    updatesViewModel: AppUpdatesViewModel,
    hasRunStartupAnimation: Boolean,
    onAnimationRun: () -> Unit,
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    backupAppListLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    viewModel: YourAndroidViewModel,
    onAddRepoClick: () -> Unit,
    onShowReleaseNotes: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current
    var isStartupAnimationRunning by remember { mutableStateOf(hasRunStartupAnimation) }
    val headerState = rememberPullTitleHeaderState(minHeight = 320.dp, maxHeight = 480.dp)
    val androidLogoRes = remember(deviceInfo) { DeviceImageMapper.getAndroidLogo(deviceInfo) }

    LaunchedEffect(hasRunStartupAnimation) {
        if (!hasRunStartupAnimation) {
            isStartupAnimationRunning = true
            onAnimationRun()
        }
    }

    val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel()
    val configuration = LocalConfiguration.current
    configuration.screenHeightDp.dp
    val isBlurEnabled by mainViewModel.isBlurEnabled

    val contentAlphaState =
        animateFloatAsState(
            targetValue = if (isStartupAnimationRunning) 1f else 0f,
            animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = EaseOut),
            label = "contentAlpha",
        )

    val contentOffsetState =
        animateDpAsState(
            targetValue = if (isStartupAnimationRunning) 0.dp else 40.dp,
            animationSpec =
                tween(
                    durationMillis = 400,
                    delayMillis = 0,
                    easing = FastOutSlowInEasing,
                ),
            label = "contentOffset",
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled) 40f else 0f,
                    height = with(LocalDensity.current) { 150.dp.toPx() },
                    direction = BlurDirection.BOTTOM,
                )
                .nestedScroll(headerState.nestedScrollConnection)
                .scrollMotionBlur(scrollState, enabled = mainViewModel.isMotionBlurEnabled.value)
                .verticalScroll(scrollState)
                .padding(
                    top =
                        contentPadding.calculateTopPadding() +
                            WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding(),
                    bottom = 150.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PullTitleHeader(
            title = deviceInfo.deviceName,
            state = headerState,
            expandableContent =
                if (androidLogoRes != 0) {
                    {
                        Icon(
                            painter = painterResource(id = androidLogoRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .padding(vertical = 12.dp)
                                    .graphicsLayer {
                                        alpha = contentAlphaState.value
                                        translationY = contentOffsetState.value.toPx()
                                    },
                        )
                    }
                } else {
                    null
                },
        )

        DeviceHeroCard(
            deviceInfo = deviceInfo,
            contentAlpha = { contentAlphaState.value },
            contentOffset = { contentOffsetState.value },
        )

        // Apps Section
        val trackedRepos by updatesViewModel.trackedRepos
        val isLoading by updatesViewModel.isLoading
        val refreshingRepoIds by updatesViewModel.refreshingRepoIds
        val context = androidx.compose.ui.platform.LocalContext.current

        Text(
            text = stringResource(R.string.label_apps),
            modifier =
                Modifier
                    .padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                    .graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val pending =
            trackedRepos
                .filter { it.isUpdateAvailable && it.mappedPackageName != null }
                .sortedByDescending { it.publishedAt }
        val upToDate =
            trackedRepos
                .filter { !it.isUpdateAvailable && it.mappedPackageName != null }
                .sortedByDescending { it.publishedAt }
        val notInstalled = trackedRepos.filter { it.mappedPackageName == null }

        if (isLoading && trackedRepos.isEmpty()) {
            androidx.compose.material3.LoadingIndicator(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(32.dp),
            )
        } else if (trackedRepos.isEmpty()) {
            RoundedCardContainer(
                modifier =
                    Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.msg_no_repos_tracked),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                shape = Shapes.extraSmall,
                            ).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImportExportButtons(
                        modifier =
                            Modifier
                                .wrapContentWidth()
                                .weight(1f),
                        view = view,
                        exportLauncher = exportLauncher,
                        importLauncher = importLauncher,
                        showExport = false,
                    )
                    Button(onClick = onAddRepoClick, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_add_repository))
                    }
                }
            }
        } else {
            RoundedCardContainer(
                modifier =
                    Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    },
            ) {
                AppsActionButtons(
                    onAddClick = onAddRepoClick,
                    onRefreshAllClick = { updatesViewModel.checkForUpdates(context) },
                    isRefreshing = refreshingRepoIds.isNotEmpty(),
                    progress = { updatesViewModel.updateProgress.value },
                )
            }

            // Pending Section
            if (pending.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_pending)} (${pending.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            .graphicsLayer {
                                alpha = contentAlphaState.value
                                translationY = contentOffsetState.value.toPx()
                            },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundedCardContainer(
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                ) {
                    pending.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes,
                        )
                    }
                }
            }

            // Up-to-date Section
            if (upToDate.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_up_to_date)} (${upToDate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            .graphicsLayer {
                                alpha = contentAlphaState.value
                                translationY = contentOffsetState.value.toPx()
                            },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundedCardContainer(
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                ) {
                    upToDate.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes,
                        )
                    }
                }
            }

            // Not Installed Section
            if (notInstalled.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_not_installed)} (${notInstalled.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            .graphicsLayer {
                                alpha = contentAlphaState.value
                                translationY = contentOffsetState.value.toPx()
                            },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundedCardContainer(
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                ) {
                    notInstalled.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes,
                        )
                    }
                }
            }

            ImportExportButtons(
                view = view,
                exportLauncher = exportLauncher,
                importLauncher = importLauncher,
                modifier =
                    Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    },
            )

            val isExportingAppList by viewModel.isExportingAppList
            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    val timeStamp =
                        SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault(),
                        ).format(Date())
                    backupAppListLauncher.launch("app_list_backup_$timeStamp.json")
                },
                enabled = !isExportingAppList,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
            ) {
                if (isExportingAppList) {
                    LinearWavyProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_save_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_backup_app_list))
                }
            }
        }
    }
}

@Composable
private fun RepoItem(
    repo: com.sameerasw.essentials.domain.model.TrackedRepo,
    updatesViewModel: AppUpdatesViewModel,
    refreshingRepoIds: Set<String>,
    context: android.content.Context,
    onAddRepoClick: () -> Unit,
    onShowReleaseNotes: (String) -> Unit,
) {
    val isInstallingThis = updatesViewModel.installingRepoId.value == repo.fullName
    TrackedRepoCard(
        repo = repo,
        isLoading = refreshingRepoIds.contains(repo.fullName),
        installStatus = if (isInstallingThis) updatesViewModel.installStatus.value else null,
        downloadProgress = if (isInstallingThis) updatesViewModel.updateProgress.value else 0f,
        onClick = {
            updatesViewModel.prepareEdit(context, repo)
            onAddRepoClick()
        },
        onActionClick = {
            if (repo.isUpdateAvailable) {
                updatesViewModel.downloadAndInstall(context, repo)
            } else {
                onShowReleaseNotes(repo.fullName)
                updatesViewModel.fetchReleaseNotesIfNeeded(context, repo)
            }
        },
        onDeleteClick = {
            updatesViewModel.untrackRepo(context, repo.fullName)
        },
        onShowReleaseNotes = {
            onShowReleaseNotes(repo.fullName)
            updatesViewModel.fetchReleaseNotesIfNeeded(context, repo)
        },
    )
}
