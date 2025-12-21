package com.sameerasw.essentials

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.composables.SetupFeatures
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install and configure the splash screen
        val splashScreen = installSplashScreen()

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

        enableEdgeToEdge()

        Log.d("MainActivity", "onCreate with action: ${intent?.action}")

        // Check if this is a QS tile long-press intent for the SoundModeTileService
        if (intent?.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            val componentName = intent?.getParcelableExtra<android.content.ComponentName>("android.intent.extra.COMPONENT_NAME")
            Log.d("MainActivity", "QS_TILE_PREFERENCES received, component: ${componentName?.className}")
            if (componentName?.className == "com.sameerasw.essentials.services.SoundModeTileService") {
                Log.d("MainActivity", "Launching volume panel")
                // Launch the volume settings panel
                val volumeIntent = Intent("android.settings.panel.action.VOLUME")
                startActivity(volumeIntent)
                finish()
                return
            }
        }

        // Initialize HapticUtil with saved preferences
        HapticUtil.initialize(this)
        // initialize permission registry
        initPermissionRegistry()
        viewModel.check(this)
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
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = "Essentials",
                            hasBack = false,
                            hasSearch = true,
                            hasSettings = true,
                            onSearchClick = { searchRequested = true },
                            onSettingsClick = { startActivity(Intent(this, SettingsActivity::class.java)) },
                            scrollBehavior = scrollBehavior,
                            subtitle = "v$versionName"
                        )
                    }
                ) { innerPadding ->
                    SetupFeatures(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        searchRequested = searchRequested,
                        onSearchHandled = { searchRequested = false }
                    )
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

        // Check if this is a QS tile long-press intent for the SoundModeTileService
        if (intent.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            val componentName = intent.getParcelableExtra<android.content.ComponentName>("android.intent.extra.COMPONENT_NAME")
            Log.d("MainActivity", "QS_TILE_PREFERENCES received in onNewIntent, component: ${componentName?.className}")
            if (componentName?.className == "com.sameerasw.essentials.services.SoundModeTileService") {
                Log.d("MainActivity", "Launching volume panel from onNewIntent")
                // Launch the volume settings panel
                val volumeIntent = Intent("android.settings.panel.action.VOLUME")
                startActivity(volumeIntent)
                finish()
                return
            }
        }
    }
}
