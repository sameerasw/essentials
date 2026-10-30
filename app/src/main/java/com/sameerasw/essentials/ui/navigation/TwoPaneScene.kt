package com.sameerasw.essentials.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.animateDpAsState
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.activity.compose.PredictiveBackHandler
import android.os.Build
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
        val left = (windowWidth * 0.5f).coerceAtLeast(MIN_LIST_WIDTH).coerceAtMost(windowWidth - MIN_DETAIL_WIDTH)
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
    val parentDetailEntry: NavEntry<AppRoute>?,
    val layout: PaneLayout,
    override val previousEntries: List<NavEntry<AppRoute>>,
) : Scene<AppRoute> {
    override val entries: List<NavEntry<AppRoute>> = listOfNotNull(listEntry, parentDetailEntry, detailEntry)

    override val content: @Composable () -> Unit = {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            val fullWidth = layout.leftWidth + layout.gap + layout.rightWidth
            val listWidth by animateDpAsState(
                targetValue = if (detailEntry != null) layout.leftWidth else fullWidth,
                animationSpec = spring(dampingRatio = 0.95f, stiffness = 380f),
                label = "list_pane_width",
            )
            Box(
                modifier =
                    Modifier
                        .width(listWidth)
                        .fillMaxHeight()
                        .clipToBounds(),
            ) {
                CompositionLocalProvider(LocalListPaneWidth provides layout.leftWidth) {
                    listEntry.Content()
                }
            }
            if (detailEntry != null) {
                Box(
                    modifier =
                        Modifier
                            .offset { IntOffset((listWidth + layout.gap).roundToPx(), 0) }
                            .width(layout.rightWidth)
                            .fillMaxHeight(),
                ) {
                    key(detailEntry.contentKey) {
                        DetailPane(detailEntry, parentDetailEntry)
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
        val list = entries.first()
        if (list.route !is AppRoute.Home) return null
        return TwoPaneScene(
            key = list.contentKey,
            listEntry = list,
            detailEntry = if (lastRoute is AppRoute.Home) null else last,
            parentDetailEntry =
                if (lastRoute is AppRoute.Home) null else entries.getOrNull(entries.lastIndex - 1)?.takeIf { it.route !is AppRoute.Home },
            layout = paneLayout,
            previousEntries = entries.dropLast(1),
        )
    }
}

val LocalListPaneWidth = compositionLocalOf<Dp?> { null }

class PaneNavigation(
    val pop: () -> Unit,
    val lastNavWasPop: () -> Boolean,
)

val LocalPaneNavigation = staticCompositionLocalOf<PaneNavigation?> { null }

@Composable
private fun DetailPane(
    entry: NavEntry<AppRoute>,
    parent: NavEntry<AppRoute>?,
) {
    val paneNavigation = LocalPaneNavigation.current
    val barHost = LocalBottomBarHost.current
    val gesture = remember { Animatable(0f) }
    val commit = remember { Animatable(0f) }
    val visibleState =
        remember {
            MutableTransitionState(paneNavigation?.lastNavWasPop?.invoke() == true).apply { targetState = true }
        }

    PredictiveBackHandler(enabled = paneNavigation != null) { events ->
        try {
            events.collect { event ->
                gesture.snapTo(event.progress)
                barHost?.dismissProgress = event.progress * AppNavTransitions.GESTURE_PHASE
            }
            commit.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            paneNavigation?.pop?.invoke()
        } catch (e: CancellationException) {
            barHost?.dismissProgress = 0f
            gesture.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 500f))
        }
    }

    val gestureT = gesture.value * AppNavTransitions.GESTURE_PHASE
    val c = commit.value
    val active = gesture.value > 0f || c > 0f
    val offset = AppNavTransitions.elasticSlide.transform(gestureT).let { it + (1f - it) * c }
    val scale = 1f - 0.18f * AppNavTransitions.gestureScale.transform(gestureT)
    val corner = 32f * cornerEasing.transform(gestureT).coerceAtLeast(c)
    val underlay = (1f - gestureBlurEasing.transform(gestureT)) * (1f - c)

    Box(modifier = Modifier.fillMaxSize()) {
        if (active && parent != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val radius = (12f * underlay).dp.toPx()
                            renderEffect =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.5f) {
                                    BlurEffect(radius, radius, TileMode.Decal)
                                } else {
                                    null
                                }
                        }.drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.25f * underlay))
                        },
            ) {
                parent.Content()
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offset * size.width
                        scaleX = scale
                        scaleY = scale
                        if (active) {
                            shape = RoundedCornerShape(corner.dp)
                            clip = true
                        }
                    },
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter =
                    fadeIn(tween(220)) +
                        slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 6 },
            ) {
                entry.Content()
            }
        }
    }
}
