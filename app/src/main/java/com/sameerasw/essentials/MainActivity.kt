/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Application Entrypoint
 * File: MainActivity.kt
 * Description: Main application entrypoint for MainActivity.kt.
 */

package com.sameerasw.essentials

import androidx.compose.foundation.background
import com.sameerasw.essentials.ui.navigation.LocalBottomBarHost
import com.sameerasw.essentials.ui.navigation.BottomBarHost
import com.sameerasw.essentials.ui.navigation.AppRoutes
import com.sameerasw.essentials.ui.navigation.AppRoute
import com.sameerasw.essentials.ui.navigation.AppNavigationViewModel
import com.sameerasw.essentials.ui.navigation.AppNavTransitions
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.ui.activities.YourAndroidContent
import com.sameerasw.essentials.domain.registry.initPermissionRegistry
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.ToolbarItem
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.composables.DIYScreen
import com.sameerasw.essentials.ui.composables.FreezeGridUI
import com.sameerasw.essentials.ui.composables.SetupFeatures
import com.sameerasw.essentials.ui.composables.WelcomeScreen
import com.sameerasw.essentials.ui.core.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.ui.core.sheets.GitHubAuthSheet
import com.sameerasw.essentials.ui.core.sheets.InstructionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PrankBottomSheet
import com.sameerasw.essentials.ui.core.sheets.ShakeForHelp
import com.sameerasw.essentials.ui.core.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.AppUpdatesViewModel
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val viewModel: MainViewModel by viewModels()
    val updatesViewModel: AppUpdatesViewModel by viewModels()
    val locationViewModel: LocationReachedViewModel by viewModels()
    val gitHubAuthViewModel: GitHubAuthViewModel by viewModels()
    private val navViewModel: AppNavigationViewModel by viewModels()
    private val bottomBarHost = BottomBarHost()
    private var isAppReady = false

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and configure the splash screen
        val splashScreen = installSplashScreen()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.sameerasw.essentials.utils.ShortcutUtil
            .updateLauncherDynamicShortcuts(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Keep splash screen visible while app is loading
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        // Customize the exit animation - scale up and fade out
        // Safe implementation for OEM devices that may not provide iconView
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            try {
                val splashScreenView = splashScreenViewProvider.view
                val splashIcon =
                    try {
                        splashScreenViewProvider.iconView
                    } catch (e: Exception) {
                        null
                    }

                // Animate the splash screen view fade out
                val fadeOut =
                    ObjectAnimator.ofFloat(splashScreenView, "alpha", 1f, 0f).apply {
                        interpolator = AnticipateInterpolator()
                        duration = 750
                    }
                fadeOut.doOnEnd {
                    splashScreenViewProvider.remove()
                    // Re-apply edge to edge AFTER the splash screen view is removed
                    // to ensure it's not overridden by splash screen cleanup
                    enableEdgeToEdge()
                }

                // Safely animate the icon if it exists
                // Known issue: Some OEM devices (Samsung One UI 8, Xiaomi on Android 16)
                // may not provide iconView, causing NullPointerException
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    if (splashIcon != null) {
                        // Scale down animation
                        val scaleUp =
                            ObjectAnimator.ofFloat(splashIcon, "scaleX", 1f, 0.5f).apply {
                                interpolator = AnticipateInterpolator()
                                duration = 750
                            }

                        val scaleUpY =
                            ObjectAnimator.ofFloat(splashIcon, "scaleY", 1f, 0.5f).apply {
                                interpolator = AnticipateInterpolator()
                                duration = 750
                            }

                        // rotate
                        val rotate360 =
                            ObjectAnimator.ofFloat(splashIcon, "rotation", 0f, -90f).apply {
                                interpolator = AnticipateInterpolator()
                                duration = 750
                            }

                        scaleUp.start()
                        scaleUpY.start()
                        rotate360.start()
                    } else {
                        Log.w("SplashScreen", "iconView is null - OEM device detected")
                    }
                } catch (e: NullPointerException) {
                    // Handle the edge case where iconView becomes null between check and animation
                    Log.w(
                        "SplashScreen",
                        "NullPointerException on iconView animation - likely OEM device",
                        e,
                    )
                }

                // Animate the branding icon if it exists
                val brandingViewId =
                    resources.getIdentifier("splashscreen_branding_view", "id", "android")
                val brandingView =
                    if (brandingViewId != 0) {
                        splashScreenView.findViewById<android.view.View>(brandingViewId)
                    } else {
                        null
                    }

                if (brandingView != null) {
                    ObjectAnimator
                        .ofFloat(
                            brandingView,
                            "translationY",
                            0f,
                            -brandingView.height.toFloat(),
                        ).apply {
                            interpolator = AnticipateInterpolator()
                            duration = 750
                            start()
                        }
                }

                fadeOut.start()
            } catch (e: Exception) {
                // Fallback for any unexpected exceptions during animation
                Log.e("SplashScreen", "Exception during splash screen animation", e)
                try {
                    splashScreenViewProvider.remove()
                } catch (e2: Exception) {
                    Log.e("SplashScreen", "Exception during splash screen removal", e2)
                }
            }
        }

        Log.d("MainActivity", "onCreate with action: ${intent?.action}")
        if (savedInstanceState == null) {
            AppRoutes.fromIntent(intent)?.let { navViewModel.navigate(it) }
        }
        handleLocationIntent(intent)

        // Initialize HapticUtil with saved preferences
        HapticUtil.initialize(this)
        // initialize permission registry
        initPermissionRegistry()
        // Initialize viewModel state early for correct initial composition
        viewModel.check(this)
        setContent {
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by viewModel.isBlurEnabled
            val isSwipeTabsEnabled by viewModel.isSwipeTabsEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.sameerasw.essentials.ui.state.LocalMenuStateManager provides
                        remember {
                            com.sameerasw.essentials.ui.state
                                .MenuStateManager()
                        },
                    LocalBottomBarHost provides bottomBarHost,
                ) {
                    val backStack = navViewModel.backStack
                    val topRoute = backStack.lastOrNull() ?: AppRoute.Home
                    val isAtHome = backStack.size <= 1
                    val context = LocalContext.current
                    val view = LocalView.current
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (_: Exception) {
                        stringResource(R.string.label_unknown)
                    }

                    var showUpdateSheet by remember { mutableStateOf(false) }
                    var showInstructionsSheet by remember { mutableStateOf(false) }
                    val updateInfo by viewModel.updateInfo
                    val isUpdateAvailable by viewModel.isUpdateAvailable

                    var showGitHubAuthSheet by remember { mutableStateOf(false) }
                    var showNewAutomationSheet by remember { mutableStateOf(false) }
                    var isSearchActive by remember { mutableStateOf(false) }
                    var freezeSearchQuery by remember { mutableStateOf("") }
                    var freezeSearchSubmitSignal by remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    val gitHubToken by viewModel.gitHubToken
                    val gitHubUser by gitHubAuthViewModel.currentUser
                    val isOnboardingCompleted by viewModel.isOnboardingCompleted
                    val isWhatsNewVisible by viewModel.isWhatsNewVisible

                    LaunchedEffect(Unit) {
                        gitHubAuthViewModel.loadCachedUser(context)
                    }

                    LaunchedEffect(gitHubToken) {
                        if (gitHubToken != null && gitHubUser == null) {
                            gitHubAuthViewModel.loadUser(gitHubToken!!, context)
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.check(context)
                        // Request notification permission if not granted (Android 13+)
                        if (!viewModel.isPostNotificationsEnabled.value) {
                            viewModel.requestNotificationPermission(this@MainActivity)
                        }
                        if (!viewModel.isUpdateAvailable.value) {
                            viewModel.checkForUpdates(context, manual = true)
                        }
                        updatesViewModel.loadTrackedRepos(context)
                    }

                    // Dynamic tabs configuration
                    val tabs = remember { DIYTabs.entries }

                    val defaultTab by viewModel.defaultTab
                    val initialPage =
                        remember(tabs, defaultTab) {
                            val index = tabs.indexOf(defaultTab)
                            if (index != -1) index else 0
                        }
                    val pagerState =
                        rememberPagerState(
                            initialPage = initialPage,
                            pageCount = { tabs.size },
                        )
                    val tabSwitchProgress = remember { Animatable(1f) }
                    var tabSwitchDirection by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
                    val jumpToTab: suspend (Int) -> Unit = { target ->
                        val current = pagerState.currentPage
                        if (kotlin.math.abs(target - current) <= 1) {
                            pagerState.animateScrollToPage(target)
                        } else {
                            tabSwitchDirection = if (target > current) 1f else -1f
                            tabSwitchProgress.snapTo(0f)
                            pagerState.scrollToPage(target)
                            tabSwitchProgress.animateTo(
                                1f,
                                androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }

                    LaunchedEffect(intent) {
                        intent?.getStringExtra("target_tab")?.let { tabName ->
                            try {
                                val tab = DIYTabs.valueOf(tabName)
                                val index = tabs.indexOf(tab)
                                if (index != -1) {
                                    pagerState.scrollToPage(index)
                                    intent.removeExtra("target_tab")
                                }
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }

                    // Haptic feedback on tab page change
                    LaunchedEffect(pagerState) {
                        var isFirst = true
                        snapshotFlow { pagerState.currentPage }
                            .collect {
                                if (isFirst) {
                                    isFirst = false
                                } else if (isSwipeTabsEnabled) {
                                    HapticUtil.performHeavyHaptic(view)
                                }
                            }
                    }

                    // Bucketed rumble haptic while swiping between tabs
                    var lastSwipeHapticBucket by androidx.compose.runtime.mutableIntStateOf(0)
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPageOffsetFraction }
                            .collect { offset ->
                                if (!isSwipeTabsEnabled) return@collect
                                val fraction = kotlin.math.abs(offset)
                                val currentBucket = (fraction * 10).toInt()
                                if (currentBucket != lastSwipeHapticBucket) {
                                    if (fraction > 0f) {
                                        HapticUtil.performSliderHaptic(view)
                                    }
                                    lastSwipeHapticBucket = currentBucket
                                }
                            }
                    }
                    val backProgress = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()

                    // Handle predictive back button for tab navigation
                    PredictiveBackHandler(enabled = isAtHome && pagerState.currentPage != initialPage) { progress ->
                        try {
                            progress.collect { backEvent ->
                                backProgress.snapTo(backEvent.progress)
                            }
                            scope.launch {
                                jumpToTab(initialPage)
                            }
                            scope.launch {
                                backProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 400),
                                )
                            }
                        } catch (e: java.util.concurrent.CancellationException) {
                            scope.launch {
                                backProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 300),
                                )
                            }
                        }
                    }

                    // Gracefully handle tab removal (e.g. disabling Developer Mode)
                    LaunchedEffect(tabs) {
                        if (pagerState.currentPage >= tabs.size) {
                            pagerState.scrollToPage(0)
                        }
                    }

                    if (showUpdateSheet) {
                        UpdateBottomSheet(
                            updateInfo = updateInfo,
                            isChecking = viewModel.isCheckingUpdate.value,
                            onDismissRequest = { showUpdateSheet = false },
                        )
                    }

                    if (showInstructionsSheet) {
                        InstructionsBottomSheet(
                            onDismissRequest = { showInstructionsSheet = false },
                        )
                    }

                    ShakeForHelp(viewModel = viewModel)

                    if (showGitHubAuthSheet) {
                        GitHubAuthSheet(
                            viewModel = gitHubAuthViewModel,
                            onDismissRequest = { showGitHubAuthSheet = false },
                        )
                    }

                    val isAprilFoolsSheetVisible by viewModel.isAprilFoolsSheetVisible
                    val prankSheetState =
                        androidx.compose.material3.rememberModalBottomSheetState(
                            skipPartiallyExpanded = true,
                        )

                    if (isAprilFoolsSheetVisible) {
                        PrankBottomSheet(
                            viewModel = viewModel,
                            sheetState = prankSheetState,
                            onDismissRequest = {
                                viewModel.isAprilFoolsSheetVisible.value = false
                                viewModel.settingsRepository.putBoolean(
                                    SettingsRepository.KEY_APRIL_FOOLS_SHOWN,
                                    true,
                                )
                            },
                        )
                    }

                    val updateProgress by updatesViewModel.updateProgress

                    var showAddRepoSheet by remember { mutableStateOf(false) }
                    var repoToShowReleaseNotesFullName by remember { mutableStateOf<String?>(null) }
                    val trackedRepos by updatesViewModel.trackedRepos

                    val yourAndroidViewModel: com.sameerasw.essentials.ui.activities.YourAndroidViewModel =
                        androidx.lifecycle.viewmodel.compose
                            .viewModel()
                    val deviceInfo = remember { com.sameerasw.essentials.utils.DeviceUtils.getDeviceInfo(context) }
                    var showFabProfileMenu by remember { mutableStateOf(false) }
                    var showBugReportSheet by remember { mutableStateOf(false) }

                    if (showBugReportSheet) {
                        com.sameerasw.essentials.ui.core.sheets.BugReportBottomSheet(
                            viewModel = viewModel,
                            onDismissRequest = { showBugReportSheet = false },
                        )
                    }

                    val backupAppListLauncher =
                        rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.CreateDocument("application/json"),
                        ) { uri ->
                            uri?.let { yourAndroidViewModel.exportAppList(context, it) }
                        }

                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        uri?.let {
                            contentResolver.openOutputStream(it)?.use { outputStream ->
                                updatesViewModel.exportTrackedRepos(context, outputStream)
                                Toast
                                    .makeText(
                                        context,
                                        getString(R.string.msg_export_success),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }

                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        uri?.let {
                            contentResolver.openInputStream(it)?.use { inputStream ->
                                if (updatesViewModel.importTrackedRepos(context, inputStream)) {
                                    updatesViewModel.loadTrackedRepos(context)
                                    Toast
                                        .makeText(
                                            context,
                                            getString(R.string.msg_import_success),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                } else {
                                    Toast
                                        .makeText(
                                            context,
                                            getString(R.string.msg_import_failed),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        }
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
                            },
                        )
                    }

                    if (repoToShowReleaseNotesFullName != null) {
                        val repo =
                            trackedRepos.find { it.fullName == repoToShowReleaseNotesFullName }
                        if (repo != null) {
                            val isNotesLoading = repo.latestReleaseBody.isNullOrBlank()
                            UpdateBottomSheet(
                                updateInfo =
                                    com.sameerasw.essentials.domain.model.UpdateInfo(
                                        versionName = repo.latestTagName,
                                        releaseNotes = repo.latestReleaseBody ?: "",
                                        downloadUrl = repo.downloadUrl ?: "",
                                        releaseUrl = repo.latestReleaseUrl ?: "",
                                        isUpdateAvailable = repo.isUpdateAvailable,
                                    ),
                                isChecking = isNotesLoading,
                                onDismissRequest = { repoToShowReleaseNotesFullName = null },
                            )
                        } else {
                            repoToShowReleaseNotesFullName = null
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            contentWindowInsets =
                                WindowInsets(
                                    0,
                                    0,
                                    0,
                                    0,
                                ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            topBar = {},
                        ) { innerPadding ->
                            val statusBarHeightPx =
                                with(androidx.compose.ui.platform.LocalDensity.current) {
                                    WindowInsets.statusBars
                                        .asPaddingValues()
                                        .calculateTopPadding()
                                        .toPx()
                                }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .progressiveBlur(
                                            blurRadius = if (isBlurEnabled) 40f else 0f,
                                            height = statusBarHeightPx * 1.15f,
                                            direction = BlurDirection.TOP,
                                        ),
                            ) {
                                val currentTab =
                                    remember(tabs, pagerState.currentPage) {
                                        tabs.getOrNull(pagerState.currentPage) ?: tabs.firstOrNull()
                                            ?: DIYTabs.ESSENTIALS
                                    }

                                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                                val searchFocusRequester = remember { FocusRequester() }
                                val isFreezeSearch = currentTab == DIYTabs.FREEZE
                                val searchQuery = if (isFreezeSearch) freezeSearchQuery else viewModel.searchQuery.value
                                val onSearchQueryChange: (String) -> Unit = {
                                    if (isFreezeSearch) freezeSearchQuery = it else viewModel.onSearchQueryChanged(it, context)
                                }
                                val closeSearch = {
                                    focusManager.clearFocus()
                                    viewModel.onSearchQueryChanged("", context)
                                    freezeSearchQuery = ""
                                    isSearchActive = false
                                }

                                BackHandler(enabled = isAtHome && isSearchActive) { closeSearch() }

                                LaunchedEffect(pagerState.currentPage) {
                                    if (isSearchActive) closeSearch()
                                }

                                SharedTransitionLayout(
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .zIndex(1f),
                                ) {
                                    val bottomBarState = rememberSharedContentState(key = "bottom_bar")
                                    val isDetail = !isAtHome
                                    val detailSpec = if (isDetail) bottomBarHost.specFor(topRoute) else null
                                    val routeTitle =
                                        (topRoute as? AppRoute.FeatureSettings)?.let { route ->
                                            com.sameerasw.essentials.domain.registry.FeatureRegistry.ALL_FEATURES
                                                .find { it.id == route.featureId }
                                                ?.let { stringResource(it.title) }
                                                ?: route.featureId
                                        }
                                    val barMode = if (isAtHome && isSearchActive) "search" else "bar"
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        AnimatedContent(
                                            targetState = barMode,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "bottom_bar",
                                        ) { mode ->
                                            val morphBounds =
                                                if (transition.isRunning) {
                                                    Modifier.sharedBounds(bottomBarState, this@AnimatedContent)
                                                } else {
                                                    Modifier
                                                }
                                            if (mode == "search") {
                                                LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
                                                Surface(
                                                    modifier =
                                                        Modifier
                                                            .then(morphBounds)
                                                            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                                                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                                            .fillMaxWidth(),
                                                    shape = SearchBarDefaults.inputFieldShape,
                                                    color = SearchBarDefaults.colors().containerColor,
                                                    shadowElevation = 6.dp,
                                                ) {
                                                    SearchBarDefaults.InputField(
                                                        query = searchQuery,
                                                        onQueryChange = onSearchQueryChange,
                                                        onSearch = {
                                                            focusManager.clearFocus()
                                                            if (isFreezeSearch) freezeSearchSubmitSignal++
                                                        },
                                                        expanded = true,
                                                        onExpandedChange = {},
                                                        modifier = Modifier.focusRequester(searchFocusRequester),
                                                        placeholder = {
                                                            Text(
                                                                stringResource(if (isFreezeSearch) R.string.search_frozen_apps_placeholder else R.string.search_placeholder),
                                                                maxLines = 1,
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            IconButton(onClick = {
                                                                HapticUtil.performVirtualKeyHaptic(view)
                                                                closeSearch()
                                                            }) {
                                                                Icon(
                                                                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                                                                    contentDescription = stringResource(R.string.content_desc_back),
                                                                )
                                                            }
                                                        },
                                                        trailingIcon = {
                                                            if (!isFreezeSearch && viewModel.isSearching.value) {
                                                                LoadingIndicator(modifier = Modifier.size(32.dp))
                                                            } else if (searchQuery.isNotEmpty()) {
                                                                IconButton(onClick = { onSearchQueryChange("") }) {
                                                                    Icon(
                                                                        painter = painterResource(id = R.drawable.rounded_close_24),
                                                                        contentDescription = stringResource(R.string.action_clear),
                                                                    )
                                                                }
                                                            }
                                                        },
                                                    )
                                                }
                                            } else {
                                                EssentialsFloatingToolbar(
                                                    modifier = morphBounds,
                                                    selectedIndex = pagerState.currentPage,
                                                    title = if (isDetail) detailSpec?.title ?: routeTitle else null,
                                                    isBeta = isDetail && detailSpec?.isBeta == true,
                                                    onBackClick = if (isDetail) detailSpec?.onBack ?: { navViewModel.pop() } else null,
                                                    items =
                                                        tabs.mapIndexed { index, tab ->
                                                            ToolbarItem(
                                                                iconRes = tab.iconRes,
                                                                labelRes = tab.title,
                                                                onClick = {
                                                                    HapticUtil.performUIHaptic(view)
                                                                    scope.launch {
                                                                        jumpToTab(index)
                                                                    }
                                                                },
                                                                hasBadge = tab == DIYTabs.SETTINGS && isUpdateAvailable && currentTab != DIYTabs.SETTINGS,
                                                                iconScale = if (tab == DIYTabs.ESSENTIALS) 1.2f else 1f,
                                                            )
                                                        },
                                                    floatingActionButton = {
                                                        Box {
                                                            // Menu anchor
                                                            FloatingActionButton(
                                                                onClick = {
                                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                                    if (isDetail) {
                                                                        (detailSpec?.fabAction ?: detailSpec?.onHelpClick)?.invoke()
                                                                    } else when (currentTab) {
                                                                        DIYTabs.ESSENTIALS -> {
                                                                            isSearchActive = true
                                                                        }

                                                                        DIYTabs.YOUR_ANDROID -> {
                                                                            if (gitHubUser != null) {
                                                                                showFabProfileMenu = true
                                                                            } else {
                                                                                showGitHubAuthSheet = true
                                                                            }
                                                                        }

                                                                        DIYTabs.SETTINGS -> {
                                                                            showBugReportSheet = true
                                                                        }

                                                                        DIYTabs.FREEZE -> {
                                                                            isSearchActive = true
                                                                        }

                                                                        DIYTabs.DIY -> {
                                                                            showNewAutomationSheet = true
                                                                        }
                                                                    }
                                                                },
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                shape = MaterialTheme.shapes.large,
                                                                elevation =
                                                                    androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                                                                        0.dp,
                                                                        0.dp,
                                                                        0.dp,
                                                                        0.dp,
                                                                    ),
                                                            ) {
                                                                val fabKey: Any = if (isDetail) "detail:${detailSpec?.fabIconRes}" else currentTab
                                                                AnimatedContent(
                                                                    targetState = fabKey,
                                                                    transitionSpec = {
                                                                        (fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.6f)) togetherWith
                                                                            (fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.6f))
                                                                    },
                                                                    label = "fab_icon",
                                                                ) { key ->
                                                                    if (key !is DIYTabs) {
                                                                        Icon(
                                                                            painter = painterResource(id = detailSpec?.fabIconRes ?: R.drawable.rounded_help_24),
                                                                            contentDescription = detailSpec?.fabContentDescription ?: stringResource(R.string.action_help_guide),
                                                                        )
                                                                    } else {
                                                                        when (key) {
                                                                            DIYTabs.ESSENTIALS -> {
                                                                                Icon(
                                                                                    painter = painterResource(id = R.drawable.rounded_search_24),
                                                                                    contentDescription = stringResource(R.string.label_search_content_description),
                                                                                )
                                                                            }

                                                                            DIYTabs.YOUR_ANDROID -> {
                                                                                if (gitHubUser != null) {
                                                                                    coil.compose.AsyncImage(
                                                                                        model = gitHubUser!!.avatarUrl,
                                                                                        contentDescription = stringResource(R.string.action_profile),
                                                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .size(24.dp)
                                                                                                .clip(androidx.compose.foundation.shape.CircleShape),
                                                                                        placeholder = painterResource(id = R.drawable.brand_github),
                                                                                        error = painterResource(id = R.drawable.brand_github),
                                                                                    )
                                                                                } else {
                                                                                    Icon(
                                                                                        painter = painterResource(id = R.drawable.brand_github),
                                                                                        contentDescription = stringResource(R.string.action_sign_in_github),
                                                                                    )
                                                                                }
                                                                            }

                                                                            DIYTabs.SETTINGS -> {
                                                                                Icon(
                                                                                    painter = painterResource(id = R.drawable.rounded_bug_report_24),
                                                                                    contentDescription = stringResource(R.string.action_report_bug),
                                                                                )
                                                                            }

                                                                            DIYTabs.FREEZE -> {
                                                                                Icon(
                                                                                    painter = painterResource(id = R.drawable.rounded_search_24),
                                                                                    contentDescription = stringResource(R.string.label_search_content_description),
                                                                                )
                                                                            }

                                                                            DIYTabs.DIY -> {
                                                                                Icon(
                                                                                    painter = painterResource(id = R.drawable.rounded_add_24),
                                                                                    contentDescription = stringResource(R.string.diy_editor_new_title),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            if (!isDetail && currentTab == DIYTabs.YOUR_ANDROID && gitHubUser != null) {
                                                                SegmentedDropdownMenu(
                                                                    expanded = showFabProfileMenu,
                                                                    onDismissRequest = { showFabProfileMenu = false },
                                                                ) {
                                                                    SegmentedDropdownMenuItem(
                                                                        text = { Text(gitHubUser!!.name ?: gitHubUser!!.login) },
                                                                        onClick = { showFabProfileMenu = false },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                painter = painterResource(id = R.drawable.brand_github),
                                                                                contentDescription = null,
                                                                            )
                                                                        },
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
                                                                                contentDescription = null,
                                                                            )
                                                                        },
                                                                    )
                                                                }
                                                            }

                                                            val isTranslationModeActive by com.sameerasw.essentials.translation.TranslationManager.isTranslationModeEnabled

                                                            if (isTranslationModeActive &&
                                                                !isDetail && currentTab == DIYTabs.SETTINGS
                                                            ) {
                                                                Badge(
                                                                    modifier =
                                                                        Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .offset(x = 6.dp, y = (-6).dp)
                                                                            .size(28.dp),
                                                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(id = R.drawable.rounded_translate_24),
                                                                        contentDescription = "Translation Mode Active",
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .padding(4.dp),
                                                                    )
                                                                }
                                                            } else if (isUpdateAvailable &&
                                                                !isDetail && currentTab == DIYTabs.SETTINGS
                                                            ) {
                                                                Badge(
                                                                    modifier =
                                                                        Modifier
                                                                            .align(Alignment.TopEnd)
                                                                            .offset(x = 6.dp, y = (-6).dp)
                                                                            .size(28.dp),
                                                                    containerColor = MaterialTheme.colorScheme.error,
                                                                    contentColor = MaterialTheme.colorScheme.onError,
                                                                ) {
                                                                    val composition by rememberLottieComposition(
                                                                        LottieCompositionSpec.RawRes(R.raw.update_motion),
                                                                    )
                                                                    val progress by animateLottieCompositionAsState(
                                                                        composition = composition,
                                                                        iterations = LottieConstants.IterateForever,
                                                                    )
                                                                    val onErrorColor =
                                                                        MaterialTheme.colorScheme.onError
                                                                    val dynamicProperties =
                                                                        rememberLottieDynamicProperties(
                                                                            rememberLottieDynamicProperty(
                                                                                property = LottieProperty.COLOR_FILTER,
                                                                                value =
                                                                                    PorterDuffColorFilter(
                                                                                        onErrorColor.toArgb(),
                                                                                        PorterDuff.Mode.SRC_ATOP,
                                                                                    ),
                                                                                keyPath = arrayOf("**"),
                                                                            ),
                                                                        )

                                                                    LottieAnimation(
                                                                        composition = composition,
                                                                        progress = { progress },
                                                                        dynamicProperties = dynamicProperties,
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .padding(2.dp),
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }

                                val isMotionBlurEnabled by viewModel.isMotionBlurEnabled

                                val bottomBlurHeight by androidx.compose.animation.core.animateDpAsState(
                                    targetValue = if (isAtHome) 130.dp else 150.dp,
                                    label = "bottom_blur_height",
                                )
                                val bottomBlurHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { bottomBlurHeight.toPx() }

                                NavDisplay(
                                    backStack = backStack,
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .progressiveBlur(
                                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                                height = bottomBlurHeightPx,
                                                direction = BlurDirection.BOTTOM,
                                            ),
                                    onBack = { navViewModel.pop() },
                                    entryDecorators =
                                        listOf(
                                            rememberSaveableStateHolderNavEntryDecorator(),
                                            rememberViewModelStoreNavEntryDecorator(),
                                        ),
                                    transitionSpec = { AppNavTransitions.push() },
                                    popTransitionSpec = { AppNavTransitions.pop() },
                                    predictivePopTransitionSpec = { swipeEdge -> AppNavTransitions.predictivePop(swipeEdge) },
                                    entryProvider =
                                        entryProvider<AppRoute> {
                                            entry<AppRoute.Home> {
                                                androidx.compose.runtime.CompositionLocalProvider(
                                                    androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides this@MainActivity,
                                                ) {
                                                    HorizontalPager(
                                                        state = pagerState,
                                                        modifier =
                                                            Modifier
                                                                .fillMaxSize()
                                                                .scale(1f - (backProgress.value * 0.05f))
                                                                .alpha(1f - (backProgress.value * 0.3f))
                                                                .graphicsLayer {
                                                                    val p = tabSwitchProgress.value
                                                                    alpha = p.coerceIn(0f, 1f)
                                                                    translationX = (1f - p) * tabSwitchDirection * 48.dp.toPx()
                                                                    val sc = 0.96f + 0.04f * p
                                                                    scaleX = sc
                                                                    scaleY = sc
                                                                }
                                                                .scrollMotionBlur(pagerState, enabled = isMotionBlurEnabled),
                                                        userScrollEnabled = isSwipeTabsEnabled && !isSearchActive,
                                                    ) { targetPage ->
                                                        val statusBarHeight =
                                                            WindowInsets.statusBars
                                                                .asPaddingValues()
                                                                .calculateTopPadding()
                                                        val topContentPadding = statusBarHeight
                                                        val bottomToolbarPadding = 150.dp
                                                        val contentPadding =
                                                            PaddingValues(
                                                                top = topContentPadding,
                                                                bottom = bottomToolbarPadding,
                                                                start = 16.dp,
                                                                end = 16.dp,
                                                            )

                                                        when (tabs[targetPage]) {
                                                            DIYTabs.ESSENTIALS -> {
                                                                SetupFeatures(
                                                                    viewModel = viewModel,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentPadding = contentPadding,
                                                                    isSearchActive = isSearchActive,
                                                                    onHelpClick = { showInstructionsSheet = true },
                                                                    onNavigateToTab = { targetTab ->
                                                                        val tabIndex = tabs.indexOf(targetTab)
                                                                        if (tabIndex != -1) {
                                                                            scope.launch {
                                                                                jumpToTab(tabIndex)
                                                                            }
                                                                        }
                                                                    },
                                                                )
                                                            }

                                                            DIYTabs.FREEZE -> {
                                                                FreezeGridUI(
                                                                    viewModel = viewModel,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentPadding = contentPadding,
                                                                    onGetStartedClick = {
                                                                        startActivity(
                                                                            Intent(
                                                                                context,
                                                                                FeatureSettingsActivity::class.java,
                                                                            ).apply {
                                                                                putExtra("feature", "Freeze")
                                                                            },
                                                                        )
                                                                    },
                                                                    onSettingsClick = {
                                                                        startActivity(
                                                                            Intent(
                                                                                context,
                                                                                FeatureSettingsActivity::class.java,
                                                                            ).apply {
                                                                                putExtra("feature", "Freeze")
                                                                            },
                                                                        )
                                                                    },
                                                                    searchQuery = freezeSearchQuery,
                                                                    searchSubmitSignal = freezeSearchSubmitSignal,
                                                                )
                                                            }

                                                            DIYTabs.DIY -> {
                                                                DIYScreen(
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentPadding = contentPadding,
                                                                    showNewAutomationSheet = showNewAutomationSheet,
                                                                    onDismissNewAutomationSheet = {
                                                                        showNewAutomationSheet = false
                                                                    },
                                                                    onNewAutomationClick = {
                                                                        showNewAutomationSheet = true
                                                                    },
                                                                )
                                                            }

                                                            DIYTabs.YOUR_ANDROID -> {
                                                                YourAndroidContent(
                                                                    deviceInfo = deviceInfo,
                                                                    updatesViewModel = updatesViewModel,
                                                                    hasRunStartupAnimation = yourAndroidViewModel.hasRunStartupAnimation,
                                                                    onAnimationRun = { yourAndroidViewModel.hasRunStartupAnimation = true },
                                                                    exportLauncher = exportLauncher,
                                                                    importLauncher = importLauncher,
                                                                    backupAppListLauncher = backupAppListLauncher,
                                                                    viewModel = yourAndroidViewModel,
                                                                    onAddRepoClick = { showAddRepoSheet = true },
                                                                    onShowReleaseNotes = { repoToShowReleaseNotesFullName = it },
                                                                    modifier = Modifier.fillMaxSize(),
                                                                )
                                                            }

                                                            DIYTabs.SETTINGS -> {
                                                                SettingsContent(
                                                                    viewModel = viewModel,
                                                                    contentPadding = contentPadding,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            entry<AppRoute.FeatureSettings> { route ->
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                                ) {
                                                    FeatureSettingsScreen(
                                                        route = route,
                                                        onBack = { navViewModel.pop() },
                                                    )
                                                }
                                            }
                                        },
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isOnboardingCompleted || isWhatsNewVisible,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it },
                        ) {
                            WelcomeScreen(
                                viewModel = viewModel,
                                isWhatsNewFlow = isWhatsNewVisible,
                                onBeginClick = {
                                    if (isWhatsNewVisible) {
                                        viewModel.completeWhatsNew()
                                    } else {
                                        viewModel.setOnboardingCompleted(true, context)
                                    }
                                },
                            )
                        }
                    }
                    // Mark app as ready after a short delay to ensure first frame is painted
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(100)
                        isAppReady = true
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("MainActivity", "onNewIntent with action: ${intent.action}")
        AppRoutes.fromIntent(intent)?.let { navViewModel.navigate(it) }
        handleLocationIntent(intent)
    }

    override fun startActivity(
        intent: Intent,
        options: Bundle?,
    ) {
        val route = AppRoutes.fromIntent(intent)
        if (route != null && intent.component?.className == FeatureSettingsActivity::class.java.name) {
            navViewModel.navigate(route)
            return
        }
        super.startActivity(intent, options)
    }

    private fun handleLocationIntent(intent: Intent?) {
        intent?.let {
            if (locationViewModel.handleIntent(it)) {
                val settingsIntent =
                    Intent(this, FeatureSettingsActivity::class.java).apply {
                        putExtra("feature", "Location reached")
                    }
                startActivity(settingsIntent)
            }
        }
    }
}
