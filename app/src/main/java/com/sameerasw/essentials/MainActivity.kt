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
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.composables.SetupFeatures
import com.sameerasw.essentials.ui.composables.ComingSoonDIYScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : FragmentActivity() {
    val viewModel: MainViewModel by viewModels()
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
                val splashIcon = splashScreenViewProvider.iconView

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

        // Initialize HapticUtil with saved preferences
        HapticUtil.initialize(this)
        // initialize permission registry
        initPermissionRegistry()
        setContent {
            EssentialsTheme {
                val context = LocalContext.current
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (_: Exception) {
                    "Unknown"
                }

                var searchRequested by remember { mutableStateOf(false) }
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                var showUpdateSheet by remember { mutableStateOf(false) }
                val isUpdateAvailable by viewModel.isUpdateAvailable
                val updateInfo by viewModel.updateInfo

                LaunchedEffect(Unit) {
                    viewModel.check(context)
                    // Request notification permission if not granted (Android 13+)
                    if (!viewModel.isPostNotificationsEnabled.value) {
                        viewModel.requestNotificationPermission(this@MainActivity)
                    }
                    viewModel.checkForUpdates(context)
                }

                val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled
                val tabs = DIYTabs.entries
                val pagerState = rememberPagerState(pageCount = { tabs.size })
                val scope = rememberCoroutineScope()
                val exitAlwaysScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection = Bottom)

                if (showUpdateSheet) {
                    UpdateBottomSheet(
                        updateInfo = updateInfo,
                        onDismissRequest = { showUpdateSheet = false }
                    )
                }
                Scaffold(
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.nestedScroll(if (isDeveloperModeEnabled) exitAlwaysScrollBehavior else scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = if (isDeveloperModeEnabled) tabs[pagerState.currentPage].title else "Essentials",
                            hasBack = false,
                            hasSearch = true,
                            hasSettings = true,
                            onSearchClick = { searchRequested = true },
                            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                            onUpdateClick = { showUpdateSheet = true },
                            hasUpdateAvailable = isUpdateAvailable,
                            scrollBehavior = scrollBehavior,
                            subtitle = "v$versionName"
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isDeveloperModeEnabled) {
                            HorizontalFloatingToolbar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = -ScreenOffset)
                                    .zIndex(1f),
                                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                                expanded = true,
                                scrollBehavior = exitAlwaysScrollBehavior,
                                content = {
                                    tabs.forEachIndexed { index, tab ->
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                            colors = if (pagerState.currentPage == index) IconButtonDefaults.filledTonalIconButtonColors() else  IconButtonDefaults.iconButtonColors(),
                                            modifier = Modifier.width(64.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = tab.iconRes),
                                                contentDescription = tab.title,
                                                tint = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                            )
                                        }
                                    }
                                }
                            )

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                when (tabs[page]) {
                                    DIYTabs.ESSENTIALS -> {
                                        SetupFeatures(
                                            viewModel = viewModel,
                                            modifier = Modifier.padding(innerPadding),
                                            searchRequested = searchRequested,
                                            onSearchHandled = { searchRequested = false }
                                        )
                                    }
                                    DIYTabs.DIY -> {
                                        ComingSoonDIYScreen(
                                            modifier = Modifier.padding(innerPadding)
                                        )
                                    }
                                }
                            }
                        } else {
                            SetupFeatures(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding),
                                searchRequested = searchRequested,
                                onSearchHandled = { searchRequested = false }
                            )
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
        Log.d("MainActivity", "onNewIntent with action: ${intent.action}")
    }
}
