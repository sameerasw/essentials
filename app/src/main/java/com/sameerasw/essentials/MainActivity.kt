package com.sameerasw.essentials

import android.animation.ObjectAnimator
import android.content.Intent
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.registry.initPermissionRegistry
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.ToolbarItem
import com.sameerasw.essentials.ui.components.sheets.AddRepoBottomSheet
import com.sameerasw.essentials.ui.components.sheets.GitHubAuthSheet
import com.sameerasw.essentials.ui.components.sheets.InstructionsBottomSheet
import com.sameerasw.essentials.ui.components.sheets.PrankBottomSheet
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.composables.DIYScreen
import com.sameerasw.essentials.ui.composables.FreezeGridUI
import com.sameerasw.essentials.ui.composables.SetupFeatures
import com.sameerasw.essentials.ui.composables.WelcomeScreen
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
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
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and configure the splash screen
        val splashScreen = installSplashScreen()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                val splashIcon = try {
                    splashScreenViewProvider.iconView
                } catch (e: Exception) {
                    null
                }

                // Animate the splash screen view fade out
                val fadeOut = ObjectAnimator.ofFloat(splashScreenView, "alpha", 1f, 0f).apply {
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
                        val scaleUp = ObjectAnimator.ofFloat(splashIcon, "scaleX", 1f, 0.5f).apply {
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
                        e
                    )
                }

                // Animate the branding icon if it exists
                val brandingViewId =
                    resources.getIdentifier("splashscreen_branding_view", "id", "android")
                val brandingView = if (brandingViewId != 0) {
                    splashScreenView.findViewById<android.view.View>(brandingViewId)
                } else {
                    null
                }

                if (brandingView != null) {
                    ObjectAnimator.ofFloat(
                        brandingView,
                        "translationY",
                        0f,
                        -brandingView.height.toFloat()
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
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.sameerasw.essentials.ui.state.LocalMenuStateManager provides remember { com.sameerasw.essentials.ui.state.MenuStateManager() }
                ) {
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

                    var showGitHubAuthSheet by remember { mutableStateOf(false) }
                    var showNewAutomationSheet by remember { mutableStateOf(false) }
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
                        viewModel.checkForUpdates(context)
                        updatesViewModel.loadTrackedRepos(context)
                    }

                    // Dynamic tabs configuration
                    val tabs = remember { DIYTabs.entries }

                    val defaultTab by viewModel.defaultTab
                    val initialPage = remember(tabs, defaultTab) {
                        val index = tabs.indexOf(defaultTab)
                        if (index != -1) index else 0
                    }
                    var currentPage by remember {
                        androidx.compose.runtime.mutableIntStateOf(
                            initialPage
                        )
                    }
                    val backProgress = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()

                    // Handle predictive back button for tab navigation
                    PredictiveBackHandler(enabled = currentPage != initialPage) { progress ->
                        try {
                            progress.collect { backEvent ->
                                backProgress.snapTo(backEvent.progress)
                            }
                            currentPage = initialPage
                            scope.launch {
                                backProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 400)
                                )
                            }
                        } catch (e: java.util.concurrent.CancellationException) {
                            scope.launch {
                                backProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 300)
                                )
                            }
                        }
                    }

                    // Gracefully handle tab removal (e.g. disabling Developer Mode)
                    LaunchedEffect(tabs) {
                        if (currentPage >= tabs.size) {
                            currentPage = 0
                        }
                    }
                    val exitAlwaysScrollBehavior =
                        FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

                    if (showUpdateSheet) {
                        UpdateBottomSheet(
                            updateInfo = updateInfo,
                            isChecking = viewModel.isCheckingUpdate.value,
                            onDismissRequest = { showUpdateSheet = false }
                        )
                    }

                    if (showInstructionsSheet) {
                        InstructionsBottomSheet(
                            onDismissRequest = { showInstructionsSheet = false }
                        )
                    }

                    if (showGitHubAuthSheet) {
                        GitHubAuthSheet(
                            viewModel = gitHubAuthViewModel,
                            onDismissRequest = { showGitHubAuthSheet = false }
                        )
                    }

                    val isAprilFoolsSheetVisible by viewModel.isAprilFoolsSheetVisible
                    val prankSheetState = androidx.compose.material3.rememberModalBottomSheetState(
                        skipPartiallyExpanded = true
                    )

                    if (isAprilFoolsSheetVisible) {
                        PrankBottomSheet(
                            viewModel = viewModel,
                            sheetState = prankSheetState,
                            onDismissRequest = {
                                viewModel.isAprilFoolsSheetVisible.value = false
                                viewModel.settingsRepository.putBoolean(
                                    SettingsRepository.KEY_APRIL_FOOLS_SHOWN,
                                    true
                                )
                            }
                        )
                    }

                    val updateProgress by updatesViewModel.updateProgress

                    var showAddRepoSheet by remember { mutableStateOf(false) }
                    var repoToShowReleaseNotesFullName by remember { mutableStateOf<String?>(null) }
                    val trackedRepos by updatesViewModel.trackedRepos

                    rememberLauncherForActivityResult(
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

                    rememberLauncherForActivityResult(
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
                        val repo =
                            trackedRepos.find { it.fullName == repoToShowReleaseNotesFullName }
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            contentWindowInsets = WindowInsets(
                                0,
                                0,
                                0,
                                0
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            topBar = {}
                        ) { innerPadding ->
                            val statusBarHeightPx =
                                with(androidx.compose.ui.platform.LocalDensity.current) {
                                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                                        .toPx()
                                }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .progressiveBlur(
                                        blurRadius = if (isBlurEnabled) 40f else 0f,
                                        height = statusBarHeightPx * 1.15f,
                                        direction = BlurDirection.TOP
                                    )
                            ) {
                                val currentTab = remember(tabs, currentPage) {
                                    tabs.getOrNull(currentPage) ?: tabs.firstOrNull()
                                    ?: DIYTabs.ESSENTIALS
                                }

                                EssentialsFloatingToolbar(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .zIndex(1f),
                                    selectedIndex = currentPage,
                                    items = tabs.mapIndexed { index, tab ->
                                        ToolbarItem(
                                            iconRes = tab.iconRes,
                                            labelRes = tab.title,
                                            onClick = {
                                                HapticUtil.performUIHaptic(view)
                                                currentPage = index
                                            },
                                            hasBadge = false
                                        )
                                    },
                                    scrollBehavior = exitAlwaysScrollBehavior,
                                    floatingActionButton = {
                                        Box { // Menu anchor
                                            FloatingActionButton(
                                                onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    when (currentTab) {
                                                        DIYTabs.ESSENTIALS -> {
                                                            startActivity(
                                                                Intent(
                                                                    context,
                                                                    SettingsActivity::class.java
                                                                )
                                                            )
                                                        }

                                                        DIYTabs.FREEZE -> {
                                                            startActivity(
                                                                Intent(
                                                                    context,
                                                                    FeatureSettingsActivity::class.java
                                                                ).apply {
                                                                    putExtra("feature", "Freeze")
                                                                })
                                                        }

                                                        DIYTabs.DIY -> {
                                                            showNewAutomationSheet = true
                                                        }
                                                    }
                                                },
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                shape = MaterialTheme.shapes.large,
                                                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                                                    0.dp,
                                                    0.dp,
                                                    0.dp,
                                                    0.dp
                                                )
                                            ) {
                                                when (currentTab) {
                                                    DIYTabs.ESSENTIALS -> {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.rounded_settings_heart_24),
                                                            contentDescription = stringResource(R.string.content_desc_settings)
                                                        )
                                                    }

                                                    DIYTabs.FREEZE -> {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.rounded_settings_heart_24),
                                                            contentDescription = stringResource(R.string.content_desc_settings)
                                                        )
                                                    }

                                                    DIYTabs.DIY -> {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.rounded_add_24),
                                                            contentDescription = stringResource(R.string.diy_editor_new_title)
                                                        )
                                                    }

                                                }
                                            }

                                        }
                                    }
                                )

                                AnimatedContent(
                                    targetState = currentPage,
                                    transitionSpec = {
                                        val animationSpec = tween<Float>(durationMillis = 400)
                                        val slideOffset = 150

                                        (fadeIn(animationSpec = animationSpec) + slideInVertically(
                                            animationSpec = tween(durationMillis = 400),
                                            initialOffsetY = { slideOffset }
                                        )).togetherWith(
                                            fadeOut(animationSpec = animationSpec) + slideOutVertically(
                                                animationSpec = tween(durationMillis = 400),
                                                targetOffsetY = { slideOffset }
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .scale(1f - (backProgress.value * 0.05f))
                                        .alpha(1f - (backProgress.value * 0.3f))
                                        .progressiveBlur(
                                            blurRadius = if (isBlurEnabled) 40f else 0f,
                                            height = with(androidx.compose.ui.platform.LocalDensity.current) { 130.dp.toPx() },
                                            direction = BlurDirection.BOTTOM
                                        ),
                                    label = "Tab Transition"
                                ) { targetPage ->
                                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues()
                                        .calculateTopPadding()
                                    val topContentPadding = statusBarHeight
                                    val bottomToolbarPadding = 150.dp
                                    val contentPadding = PaddingValues(
                                        top = topContentPadding,
                                        bottom = bottomToolbarPadding,
                                        start = 16.dp,
                                        end = 16.dp
                                    )

                                    when (tabs[targetPage]) {
                                        DIYTabs.ESSENTIALS -> {
                                            SetupFeatures(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = contentPadding,
                                                onHelpClick = { showInstructionsSheet = true }
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
                                                            FeatureSettingsActivity::class.java
                                                        ).apply {
                                                            putExtra("feature", "Freeze")
                                                        })
                                                },
                                                onSettingsClick = {
                                                    startActivity(
                                                        Intent(
                                                            context,
                                                            FeatureSettingsActivity::class.java
                                                        ).apply {
                                                            putExtra("feature", "Freeze")
                                                        })
                                                }
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
                                                }
                                            )
                                        }

                                    }
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isOnboardingCompleted || isWhatsNewVisible,
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
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
                                }
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
        handleLocationIntent(intent)
    }

    private fun handleLocationIntent(intent: Intent?) {
        intent?.let {
            if (locationViewModel.handleIntent(it)) {
                val settingsIntent = Intent(this, FeatureSettingsActivity::class.java).apply {
                    putExtra("feature", "Location reached")
                }
                startActivity(settingsIntent)
            }
        }
    }
}

