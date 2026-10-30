package com.sameerasw.essentials.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

data class BottomBarSpec(
    val title: String,
    val isBeta: Boolean = false,
    val onBack: () -> Unit,
    val fabIconRes: Int? = null,
    val fabAction: (() -> Unit)? = null,
    val fabContentDescription: String? = null,
    val onHelpClick: (() -> Unit)? = null,
)

@Stable
class BottomBarHost {
    private val specs = mutableStateMapOf<AppRoute, BottomBarSpec>()

    fun specFor(route: AppRoute): BottomBarSpec? = specs[route]

    internal fun set(
        route: AppRoute,
        spec: BottomBarSpec,
    ) {
        specs[route] = spec
    }

    internal fun clear(route: AppRoute) {
        specs.remove(route)
    }
}

val LocalBottomBarHost = staticCompositionLocalOf<BottomBarHost?> { null }

@Composable
fun ProvideBottomBar(
    route: AppRoute,
    spec: BottomBarSpec,
) {
    val host = LocalBottomBarHost.current ?: return
    SideEffect { host.set(route, spec) }
    DisposableEffect(host, route) {
        onDispose { host.clear(route) }
    }
}
