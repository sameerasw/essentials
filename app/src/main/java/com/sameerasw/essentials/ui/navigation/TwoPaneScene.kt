package com.sameerasw.essentials.ui.navigation

import androidx.compose.runtime.key
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

data class PaneLayout(
    val leftWidth: Dp,
    val gap: Dp,
    val rightWidth: Dp,
)

private val MIN_TWO_PANE_WIDTH = 600.dp
private val MIN_LIST_WIDTH = 360.dp
private val MIN_DETAIL_WIDTH = 280.dp

@Composable
fun rememberPaneLayout(): PaneLayout? {
    val windowWidth = LocalConfiguration.current.screenWidthDp.dp
    val hinges = currentWindowAdaptiveInfoV2().windowPosture.hingeList
    val density = LocalDensity.current
    return remember<PaneLayout?>(windowWidth, hinges, density) {
        if (windowWidth < MIN_TWO_PANE_WIDTH) return@remember null
        val hinge = hinges.firstOrNull { it.isVertical }
        if (hinge != null) {
            val left = with(density) { hinge.bounds.left.toDp() }
            val gap = with(density) { hinge.bounds.width.toDp() }
            val right = windowWidth - left - gap
            if (left >= MIN_DETAIL_WIDTH && right >= MIN_DETAIL_WIDTH) {
                return@remember PaneLayout(left, gap, right)
            }
        }
        val left = (windowWidth * 0.4f).coerceAtLeast(MIN_LIST_WIDTH).coerceAtMost(windowWidth - MIN_DETAIL_WIDTH)
        PaneLayout(left, 0.dp, windowWidth - left)
    }
}

const val ROUTE_METADATA_KEY = "essentials_route"

val NavEntry<AppRoute>.route: AppRoute?
    get() = metadata[ROUTE_METADATA_KEY] as? AppRoute

data class TwoPaneScene(
    override val key: Any,
    val listEntry: NavEntry<AppRoute>,
    val detailEntry: NavEntry<AppRoute>?,
    val layout: PaneLayout,
    override val previousEntries: List<NavEntry<AppRoute>>,
) : Scene<AppRoute> {
    override val entries: List<NavEntry<AppRoute>> = listOfNotNull(listEntry, detailEntry)

    override val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(layout.leftWidth)
                        .fillMaxHeight(),
            ) {
                listEntry.Content()
            }
            Spacer(modifier = Modifier.width(layout.gap))
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            ) {
                if (detailEntry != null) {
                    key(detailEntry.contentKey) {
                        AnimatedDetail(detailEntry)
                    }
                }
            }
        }
    }
}

class TwoPaneSceneStrategy(
    private val layout: PaneLayout?,
) : SceneStrategy<AppRoute> {
    override fun SceneStrategyScope<AppRoute>.calculateScene(entries: List<NavEntry<AppRoute>>): Scene<AppRoute>? {
        val paneLayout = layout ?: return null
        val last = entries.lastOrNull() ?: return null
        val lastRoute = last.route ?: return null
        if (lastRoute is AppRoute.Home) return null
        val list = entries.first()
        if (list.route !is AppRoute.Home) return null
        return TwoPaneScene(
            key = list.contentKey,
            listEntry = list,
            detailEntry = last,
            layout = paneLayout,
            previousEntries = entries.dropLast(1),
        )
    }
}

@Composable
private fun AnimatedDetail(entry: NavEntry<AppRoute>) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter =
            fadeIn(tween(220)) +
                slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 6 },
    ) {
        entry.Content()
    }
}
