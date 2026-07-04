package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.composables.wallpaper.WallpaperScreen
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.viewmodels.MainViewModel

class WallpaperActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val settingsRepository = SettingsRepository(this)

        setContent {
            val isPitchBlackThemeEnabled by settingsRepository.isPitchBlackThemeEnabled.collectAsState(
                initial = false
            )

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    val context = LocalContext.current
                    val viewModel: MainViewModel = viewModel()
                    viewModel.check(context)

                    val initialTab = intent.getStringExtra("tab") ?: "daily"

                    WallpaperScreen(
                        onBack = { finish() },
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }
}
