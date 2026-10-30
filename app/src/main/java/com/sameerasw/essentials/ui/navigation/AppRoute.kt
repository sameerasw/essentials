package com.sameerasw.essentials.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.MainActivity
import com.sameerasw.essentials.SettingsActivity
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.activities.AutomationEditorActivity
import com.sameerasw.essentials.ui.activities.PixelSearchbarSettingsActivity
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import com.sameerasw.essentials.ui.activities.YourAndroidActivity

sealed interface AppRoute {
    data object Home : AppRoute

    data class HomeTab(
        val tab: DIYTabs,
        val expandPermissions: Boolean = false,
    ) : AppRoute

    data class FeatureSettings(
        val featureId: String,
        val highlightSetting: String? = null,
    ) : AppRoute

    data object PixelSearchbarSettings : AppRoute

    data class Wallpaper(
        val tab: String = "daily",
    ) : AppRoute

    data class AutomationEditor(
        val automationId: String? = null,
        val automationType: String? = null,
    ) : AppRoute
}

class AppNavigationViewModel : ViewModel() {
    val backStack = mutableStateListOf<AppRoute>(AppRoute.Home)
    var pendingTab by mutableStateOf<DIYTabs?>(null)
    var expandSettingsPermissions by mutableStateOf(false)
    var lastNavWasPop by mutableStateOf(false)
    var isTwoPane = false

    fun navigate(route: AppRoute) {
        if (route is AppRoute.HomeTab) {
            if (backStack.size > 1) lastNavWasPop = true
            while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            if (route.expandPermissions) expandSettingsPermissions = true
            pendingTab = route.tab
            return
        }
        if (isTwoPane) {
            val keepUntil =
                if (route is AppRoute.FeatureSettings) {
                    val parentId = FeatureRegistry.ALL_FEATURES.find { it.id == route.featureId }?.parentFeatureId
                    if (parentId == null) {
                        0
                    } else {
                        backStack.indexOfLast { it is AppRoute.FeatureSettings && it.featureId == parentId }.coerceAtLeast(0)
                    }
                } else {
                    0
                }
            while (backStack.size > keepUntil + 1) backStack.removeAt(backStack.lastIndex)
        }
        if (backStack.lastOrNull() != route) {
            lastNavWasPop = false
            backStack.add(route)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            lastNavWasPop = true
            backStack.removeAt(backStack.lastIndex)
        }
    }
}

object AppRoutes {
    const val EXTRA_NAV_FEATURE = "nav_feature"
    const val EXTRA_NAV_TARGET = "nav_target"

    fun fromIntent(intent: Intent?): AppRoute? {
        if (intent == null) return null
        intent.getStringExtra(EXTRA_NAV_FEATURE)?.let {
            return AppRoute.FeatureSettings(it, intent.getStringExtra("highlight_setting"))
        }
        return when (intent.getStringExtra(EXTRA_NAV_TARGET) ?: intent.component?.className) {
            FeatureSettingsActivity::class.java.name -> {
                val featureId = intent.getStringExtra("feature") ?: intent.getStringExtra("FEATURE_ID") ?: return null
                AppRoute.FeatureSettings(featureId, intent.getStringExtra("highlight_setting"))
            }
            PixelSearchbarSettingsActivity::class.java.name -> AppRoute.PixelSearchbarSettings
            WallpaperActivity::class.java.name -> AppRoute.Wallpaper(intent.getStringExtra("tab") ?: "daily")
            AutomationEditorActivity::class.java.name ->
                AppRoute.AutomationEditor(
                    intent.getStringExtra(AutomationEditorActivity.EXTRA_AUTOMATION_ID),
                    intent.getStringExtra(AutomationEditorActivity.EXTRA_AUTOMATION_TYPE),
                )
            YourAndroidActivity::class.java.name -> AppRoute.HomeTab(DIYTabs.YOUR_ANDROID)
            SettingsActivity::class.java.name ->
                AppRoute.HomeTab(DIYTabs.SETTINGS, intent.getBooleanExtra("expand_permissions", false))
            else -> null
        }
    }

    fun redirectToMain(activity: Activity) {
        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                activity.intent?.extras?.let { putExtras(it) }
                putExtra(EXTRA_NAV_TARGET, activity.javaClass.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
        )
        activity.finish()
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
    }
}

fun Context.findFragmentActivity(): FragmentActivity {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    error("Screen must be hosted in a FragmentActivity")
}

val LocalSelectedFeatureIds = androidx.compose.runtime.compositionLocalOf<Set<String>> { emptySet() }

fun List<AppRoute>.openFeatureIds(): Set<String> =
    mapNotNullTo(HashSet()) { route ->
        when (route) {
            is AppRoute.FeatureSettings -> route.featureId
            is AppRoute.PixelSearchbarSettings -> "Pixel Searchbar"
            is AppRoute.Wallpaper -> "Daily Wallpaper"
            else -> null
        }
    }

val LocalOpenAutomationId = androidx.compose.runtime.compositionLocalOf<String?> { null }
