package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteCarousel(
    pinnedKeys: List<String>,
    onFeatureClick: (Feature) -> Unit,
    onFeatureLongClick: (Feature) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pinnedKeys.isEmpty()) return

    val pinnedFeatures = remember(pinnedKeys) {
        val featuresMap = FeatureRegistry.ALL_FEATURES.associateBy { it.id }
        pinnedKeys.mapNotNull { featuresMap[it] }
    }

    if (pinnedFeatures.isEmpty()) return

    val carouselState = rememberCarouselState { pinnedFeatures.size }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.x != 0f) {
                    Offset(x = available.x, y = 0f)
                } else {
                    Offset.Zero
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection)
    ) {

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 140.dp,
            itemSpacing = 4.dp,
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) { index ->
            val feature = pinnedFeatures[index]
            val view = LocalView.current
            val resolvedTitle = stringResource(id = feature.title)
            var showMenu by remember { mutableStateOf(false) }
            
            val menuState = com.sameerasw.essentials.ui.state.LocalMenuStateManager.current
            LaunchedEffect(showMenu) {
                if (showMenu) {
                    menuState.activeId = feature.id
                } else {
                    if (menuState.activeId == feature.id) {
                        menuState.activeId = null
                    }
                }
            }
            
            val isBlurred = menuState.activeId != null && menuState.activeId != feature.id
            val blurRadius by animateDpAsState(
                targetValue = if (isBlurred) 10.dp else 0.dp,
                animationSpec = tween(durationMillis = 500),
                label = "blur"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isBlurred) 0.5f else 1f,
                animationSpec = tween(durationMillis = 500),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
                    .maskClip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .pointerInput(feature) {
                        detectTapGestures(
                            onLongPress = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                showMenu = true
                            },
                            onTap = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onFeatureClick(feature)
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = ColorUtil.getPastelColorFor(resolvedTitle),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = feature.iconRes),
                            contentDescription = resolvedTitle,
                            modifier = Modifier.size(28.dp),
                            tint = ColorUtil.getVibrantColorFor(resolvedTitle)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resolvedTitle,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SegmentedDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(R.string.action_unpin)) },
                        onClick = {
                            showMenu = false
                            onFeatureLongClick(feature)
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_bookmark_remove_24),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}
