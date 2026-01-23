package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
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
