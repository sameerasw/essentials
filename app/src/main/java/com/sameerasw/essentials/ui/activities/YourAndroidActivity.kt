package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.input.pointer.pointerInput
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.DeviceHeroCard
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.DeviceUtils
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import com.sameerasw.essentials.utils.HapticUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel
import com.sameerasw.essentials.ui.components.sheets.GitHubAuthSheet
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.components.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.cards.TrackedRepoCard
import com.sameerasw.essentials.ui.components.AppsActionButtons
import com.sameerasw.essentials.ui.components.ImportExportButtons
import android.widget.Toast
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.fillMaxWidth
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.DeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class YourAndroidViewModel : ViewModel() {
    var hasRunStartupAnimation = false
}

class YourAndroidActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDarkMode =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)

        setContent {
            val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by mainViewModel.isBlurEnabled

            val viewModel: YourAndroidViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val updatesViewModel: AppUpdatesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val gitHubAuthViewModel: GitHubAuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val context = androidx.compose.ui.platform.LocalContext.current
            val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }
            var showGitHubAuthSheet by remember { mutableStateOf(false) }
            var showAddRepoSheet by remember { mutableStateOf(false) }
            var repoToShowReleaseNotesFullName by remember { mutableStateOf<String?>(null) }
            var showFabProfileMenu by remember { mutableStateOf(false) }

            val gitHubToken by mainViewModel.gitHubToken
            val gitHubUser by gitHubAuthViewModel.currentUser

            LaunchedEffect(Unit) {
                gitHubAuthViewModel.loadCachedUser(context)
                updatesViewModel.loadTrackedRepos(context)
            }

            LaunchedEffect(gitHubToken) {
                if (gitHubToken != null && gitHubUser == null) {
                    gitHubAuthViewModel.loadUser(gitHubToken!!, context)
                }
            }

            val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        updatesViewModel.exportTrackedRepos(context, outputStream)
                        Toast.makeText(
                            context,
                            getString(R.string.msg_export_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    contentResolver.openInputStream(it)?.use { inputStream ->
                        if (updatesViewModel.importTrackedRepos(context, inputStream)) {
                            updatesViewModel.loadTrackedRepos(context)
                            Toast.makeText(
                                context,
                                getString(R.string.msg_import_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.msg_import_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            val yourAndroidFeature = remember {
                object : com.sameerasw.essentials.domain.model.Feature(
                    id = "Your Android",
                    title = R.string.tab_your_android,
                    iconRes = R.drawable.rounded_android_24,
                    category = R.string.cat_system,
                    description = R.string.about_desc_your_android,
                    aboutDescription = R.string.about_desc_your_android,
                    showToggle = false,
                    hasMoreSettings = false
                ) {
                    override fun isEnabled(viewModel: com.sameerasw.essentials.viewmodels.MainViewModel) =
                        true

                    override fun onToggle(
                        viewModel: com.sameerasw.essentials.viewmodels.MainViewModel,
                        context: android.content.Context,
                        enabled: Boolean
                    ) {
                    }
                }
            }

            LaunchedEffect(Unit) {
                mainViewModel.check(context)
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val statusBarHeightPx = with(LocalDensity.current) {
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .progressiveBlur(
                            blurRadius = if (isBlurEnabled) 40f else 0f,
                            height = statusBarHeightPx * 1.15f,
                            direction = BlurDirection.TOP
                        )
                ) {
                    YourAndroidContent(
                        deviceInfo = deviceInfo,
                        updatesViewModel = updatesViewModel,
                        hasRunStartupAnimation = viewModel.hasRunStartupAnimation,
                        onAnimationRun = { viewModel.hasRunStartupAnimation = true },
                        exportLauncher = exportLauncher,
                        importLauncher = importLauncher,
                        onAddRepoClick = { showAddRepoSheet = true },
                        onShowReleaseNotes = { repoToShowReleaseNotesFullName = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    val localView = androidx.compose.ui.platform.LocalView.current
                    EssentialsFloatingToolbar(
                        title = stringResource(R.string.tab_your_android),
                        onBackClick = { finish() },
                        floatingActionButton = {
                            Box {
                                FloatingActionButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(localView)
                                        if (gitHubUser != null) {
                                            showFabProfileMenu = true
                                        } else {
                                            showGitHubAuthSheet = true
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.large,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    if (gitHubUser != null) {
                                        AsyncImage(
                                            model = gitHubUser!!.avatarUrl,
                                            contentDescription = stringResource(R.string.action_profile),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape),
                                            placeholder = painterResource(id = R.drawable.brand_github),
                                            error = painterResource(id = R.drawable.brand_github)
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.brand_github),
                                            contentDescription = stringResource(R.string.action_sign_in_github)
                                        )
                                    }
                                }

                                if (gitHubUser != null) {
                                    SegmentedDropdownMenu(
                                        expanded = showFabProfileMenu,
                                        onDismissRequest = { showFabProfileMenu = false }
                                    ) {
                                        SegmentedDropdownMenuItem(
                                            text = { Text(gitHubUser!!.name ?: gitHubUser!!.login) },
                                            onClick = { showFabProfileMenu = false },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.brand_github),
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        SegmentedDropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_sign_out)) },
                                            onClick = {
                                                gitHubAuthViewModel.signOut(context)
                                                showFabProfileMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.rounded_logout_24),
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                    )

                    if (showGitHubAuthSheet) {
                        GitHubAuthSheet(
                            viewModel = gitHubAuthViewModel,
                            onDismissRequest = { showGitHubAuthSheet = false }
                        )
                    }

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
                        val trackedRepos by updatesViewModel.trackedRepos
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
                }
            }
        }
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
    onAddRepoClick: () -> Unit,
    onShowReleaseNotes: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val scrollState = rememberScrollState()
    val overscrollOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < 0 && overscrollOffset.value > 0) {
                    val toConsume =
                        if (overscrollOffset.value + available.y >= 0) available.y else -overscrollOffset.value
                    scope.launch {
                        overscrollOffset.snapTo(overscrollOffset.value + toConsume)
                    }
                    return androidx.compose.ui.geometry.Offset(0f, toConsume)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    val prevValue = overscrollOffset.value
                    if (prevValue >= 350f) return androidx.compose.ui.geometry.Offset.Zero

                    val newValue = (prevValue + available.y * 0.5f).coerceAtMost(350f)
                    scope.launch {
                        overscrollOffset.snapTo(newValue)
                    }

                    // Haptic feedback when stretched significantly
                    if (prevValue < 300f && newValue >= 300f) {
                        HapticUtil.performUIHaptic(view)
                    }

                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }


    var isStartupAnimationRunning by remember { mutableStateOf(hasRunStartupAnimation) }

    LaunchedEffect(hasRunStartupAnimation) {
        if (!hasRunStartupAnimation) {
            isStartupAnimationRunning = true
            onAnimationRun()
        }
    }

    val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isBlurEnabled by mainViewModel.isBlurEnabled


    val contentAlphaState = animateFloatAsState(
        targetValue = if (isStartupAnimationRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = EaseOut),
        label = "contentAlpha"
    )

    val contentOffsetState = animateDpAsState(
        targetValue = if (isStartupAnimationRunning) 0.dp else 40.dp,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 0,
            easing = FastOutSlowInEasing
        ),
        label = "contentOffset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .progressiveBlur(
                blurRadius = if (isBlurEnabled) 40f else 0f,
                height = with(LocalDensity.current) { 150.dp.toPx() },
                direction = BlurDirection.BOTTOM
            )
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Release && overscrollOffset.value > 0) {
                            scope.launch {
                                overscrollOffset.animateTo(
                                    0f,
                                    androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    }
                }
            }
            .verticalScroll(scrollState)
            .padding(
                top = contentPadding.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding(),
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeviceHeroCard(
            deviceInfo = deviceInfo,
            contentAlpha = { contentAlphaState.value },
            contentOffset = { contentOffsetState.value },
            overscrollOffset = overscrollOffset.value
        )

        // Apps Section
        val trackedRepos by updatesViewModel.trackedRepos
        val isLoading by updatesViewModel.isLoading
        val refreshingRepoIds by updatesViewModel.refreshingRepoIds
        val context = androidx.compose.ui.platform.LocalContext.current

        Text(
            text = stringResource(R.string.label_apps),
            modifier = Modifier
                .padding(start = 8.dp, top = 16.dp, bottom = 4.dp)
                .graphicsLayer {
                    alpha = contentAlphaState.value
                    translationY = contentOffsetState.value.toPx()
                },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val pending = trackedRepos.filter { it.isUpdateAvailable && it.mappedPackageName != null }
            .sortedByDescending { it.publishedAt }
        val upToDate = trackedRepos.filter { !it.isUpdateAvailable && it.mappedPackageName != null }
            .sortedByDescending { it.publishedAt }
        val notInstalled = trackedRepos.filter { it.mappedPackageName == null }

        if (isLoading && trackedRepos.isEmpty()) {
            androidx.compose.material3.LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else if (trackedRepos.isEmpty()) {
            androidx.compose.material3.OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    },
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.msg_no_repos_tracked),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onAddRepoClick) {
                        Text(stringResource(R.string.action_add_repository))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    ImportExportButtons(
                        view = view,
                        exportLauncher = exportLauncher,
                        importLauncher = importLauncher,
                        showExport = false
                    )
                }
            }
        } else {
            RoundedCardContainer(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlphaState.value
                    translationY = contentOffsetState.value.toPx()
                }
            ) {
                AppsActionButtons(
                    onAddClick = onAddRepoClick,
                    onRefreshAllClick = { updatesViewModel.checkForUpdates(context) },
                    isRefreshing = refreshingRepoIds.isNotEmpty(),
                    progress = { updatesViewModel.updateProgress.value }
                )
            }

            // Pending Section
            if (pending.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_pending)} (${pending.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        .graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.sameerasw.essentials.ui.components.containers.RoundedCardContainer(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    }
                ) {
                    pending.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes
                        )
                    }
                }
            }

            // Up-to-date Section
            if (upToDate.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_up_to_date)} (${upToDate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        .graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.sameerasw.essentials.ui.components.containers.RoundedCardContainer(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    }
                ) {
                    upToDate.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes
                        )
                    }
                }
            }

            // Not Installed Section
            if (notInstalled.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.label_not_installed)} (${notInstalled.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                        .graphicsLayer {
                            alpha = contentAlphaState.value
                            translationY = contentOffsetState.value.toPx()
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.sameerasw.essentials.ui.components.containers.RoundedCardContainer(
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlphaState.value
                        translationY = contentOffsetState.value.toPx()
                    }
                ) {
                    notInstalled.forEach { repo ->
                        RepoItem(
                            repo = repo,
                            updatesViewModel = updatesViewModel,
                            refreshingRepoIds = refreshingRepoIds,
                            context = context,
                            onAddRepoClick = onAddRepoClick,
                            onShowReleaseNotes = onShowReleaseNotes
                        )
                    }
                }
            }

            ImportExportButtons(
                view = view,
                exportLauncher = exportLauncher,
                importLauncher = importLauncher,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlphaState.value
                    translationY = contentOffsetState.value.toPx()
                }
            )
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
    onShowReleaseNotes: (String) -> Unit
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
        }
    )
}
