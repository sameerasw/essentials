package com.sameerasw.essentials.ui.navigation

import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.FeatureSettingsActivity

sealed interface AppRoute {
    data object Home : AppRoute

    data class FeatureSettings(
        val featureId: String,
        val highlightSetting: String? = null,
    ) : AppRoute
}

class AppNavigationViewModel : ViewModel() {
    val backStack = mutableStateListOf<AppRoute>(AppRoute.Home)

    fun navigate(route: AppRoute) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

object AppRoutes {
    const val EXTRA_NAV_FEATURE = "nav_feature"

    fun fromIntent(intent: Intent?): AppRoute? {
        if (intent == null) return null
        val isFeatureSettings =
            intent.component?.className == FeatureSettingsActivity::class.java.name ||
                intent.hasExtra(EXTRA_NAV_FEATURE)
        if (!isFeatureSettings) return null
        val featureId =
            intent.getStringExtra(EXTRA_NAV_FEATURE)
                ?: intent.getStringExtra("feature")
                ?: intent.getStringExtra("FEATURE_ID")
                ?: return null
        return AppRoute.FeatureSettings(featureId, intent.getStringExtra("highlight_setting"))
    }
}
