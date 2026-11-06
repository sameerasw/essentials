package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.os.Bundle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.composables.wallpaper.WallpaperScreen
import com.sameerasw.essentials.ui.navigation.AppRoute
import com.sameerasw.essentials.ui.navigation.AppRoutes
import com.sameerasw.essentials.viewmodels.MainViewModel

class WallpaperActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRoutes.redirectToMain(this)
    }
}

@Composable
fun WallpaperRouteScreen(
    route: AppRoute.Wallpaper,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    remember(context) { viewModel.check(context) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        WallpaperScreen(
            route = route,
            onBack = onBack,
            viewModel = viewModel,
            initialTab = route.tab,
        )
    }
}
