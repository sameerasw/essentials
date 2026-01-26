package com.sameerasw.essentials

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.registry.initPermissionRegistry
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.DIYFloatingToolbar
import com.sameerasw.essentials.ui.composables.SetupFeatures
import com.sameerasw.essentials.ui.composables.DIYScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.components.sheets.InstructionsBottomSheet
import com.sameerasw.essentials.ui.components.sheets.GitHubAuthSheet
import com.sameerasw.essentials.viewmodels.GitHubAuthViewModel
import com.sameerasw.essentials.ui.composables.configs.FreezeSettingsUI
import com.sameerasw.essentials.ui.composables.FreezeGridUI
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : FragmentActivity() {
    val viewModel: MainViewModel by viewModels()
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
                val splashIcon = try { splashScreenViewProvider.iconView } catch (e: Exception) { null }

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

                        val scaleUpY = ObjectAnimator.ofFloat(splashIcon, "scaleY", 1f, 0.5f).apply {
                            interpolator = AnticipateInterpolator()
                            duration = 750
                        }

                        // rotate
                        val rotate360 = ObjectAnimator.ofFloat(splashIcon, "rotation", 0f, -90f).apply {
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
                    Log.w("SplashScreen", "NullPointerException on iconView animation - likely OEM device", e)
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
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val context = LocalContext.current
                val view = LocalView.current
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    stringResource(R.string.label_unknown)
                }

                var searchRequested by remember { mutableStateOf(false) }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                var showUpdateSheet by remember { mutableStateOf(false) }
                var showInstructionsSheet by remember { mutableStateOf(false) }
                val isUpdateAvailable by viewModel.isUpdateAvailable
                val updateInfo by viewModel.updateInfo

                var showGitHubAuthSheet by remember { mutableStateOf(false) }
                val gitHubToken by viewModel.gitHubToken
                val gitHubUser by gitHubAuthViewModel.currentUser

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
                }

                val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
                
                // Dynamic tabs configuration
                val tabs = remember(isDeveloperModeEnabled) {
                    if (isDeveloperModeEnabled) DIYTabs.entries else listOf(DIYTabs.ESSENTIALS, DIYTabs.FREEZE, DIYTabs.DIY)
                }
                
                val defaultTab by viewModel.defaultTab
                val initialPage = remember(tabs) {
                    val index = tabs.indexOf(defaultTab)
                    if (index != -1) index else 0
                }
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabs.size })
                val scope = rememberCoroutineScope()

                // Gracefully handle tab removal (e.g. disabling Developer Mode)
                LaunchedEffect(tabs) {
                    if (pagerState.currentPage >= tabs.size) {
                        pagerState.scrollToPage(0)
                    }
                }
                val exitAlwaysScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

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
                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .nestedScroll(exitAlwaysScrollBehavior),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        val currentTab = remember(tabs, pagerState.currentPage) {
                            tabs.getOrNull(pagerState.currentPage) ?: tabs.firstOrNull() ?: DIYTabs.ESSENTIALS
                        }
                        ReusableTopAppBar(
                            title = currentTab.title,
                            subtitle = currentTab.subtitle,
                            hasBack = false,
                            hasSearch = true,
                            hasSettings = true,
                            hasHelp = true,
                            helpIconRes = if (currentTab == DIYTabs.APPS) R.drawable.rounded_downloading_24 else R.drawable.rounded_help_24,
                            helpContentDescription = if (currentTab == DIYTabs.APPS) R.string.tab_app_updates_title else R.string.action_help_guide,
                            onSearchClick = { searchRequested = true },
                            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                            onUpdateClick = { showUpdateSheet = true },
                            onGitHubClick = { showGitHubAuthSheet = true },
                            hasGitHub = currentTab == DIYTabs.APPS,
                            gitHubUser = gitHubUser,
                            onSignOutClick = { gitHubAuthViewModel.signOut(context) },
                            onHelpClick = { 
                                if (currentTab == DIYTabs.APPS) {
                                    startActivity(Intent(this, AppUpdatesActivity::class.java))
                                } else {
                                    showInstructionsSheet = true
                                }
                            },
                            hasUpdateAvailable = isUpdateAvailable,
                            hasHelpBadge = viewModel.hasPendingUpdates.value && currentTab == DIYTabs.APPS,
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        DIYFloatingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset - 12.dp)
                                .zIndex(1f),
                            currentPage = pagerState.currentPage,
                            tabs = tabs,
                            onTabSelected = { index ->
                                HapticUtil.performUIHaptic(view)
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            scrollBehavior = exitAlwaysScrollBehavior,
                            badges = mapOf(DIYTabs.APPS to viewModel.hasPendingUpdates.value)
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (tabs[page]) {
                                DIYTabs.ESSENTIALS -> {
                                    SetupFeatures(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding),
                                        searchRequested = searchRequested,
                                        onSearchHandled = { searchRequested = false },
                                        onHelpClick = { showInstructionsSheet = true }
                                    )
                                }
                                DIYTabs.FREEZE -> {
                                    FreezeGridUI(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding),
                                        contentPadding = innerPadding
                                    )
                                }
                                DIYTabs.DIY -> {
                                    DIYScreen(
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                                DIYTabs.APPS -> {
                                    val hasUpdates by viewModel.hasPendingUpdates
                                    
                                    Column(
                                        modifier = Modifier
                                            .padding(innerPadding)
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        if (hasUpdates) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                onClick = {
                                                    HapticUtil.performMediumHaptic(view)
                                                    startActivity(Intent(this@MainActivity, AppUpdatesActivity::class.java))
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.rounded_info_24),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = "App updates available",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Essential Apps - Coming Soon",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                // Mark app as ready after composing (happens very quickly)
                LaunchedEffect(Unit) {
                    isAppReady = true
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
