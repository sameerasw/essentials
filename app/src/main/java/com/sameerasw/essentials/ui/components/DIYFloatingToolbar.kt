package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.domain.DIYTabs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DIYFloatingToolbar(
    modifier: Modifier = Modifier,
    currentPage: Int,
    tabs: List<DIYTabs>,
    onTabSelected: (Int) -> Unit,
    scrollBehavior: FloatingToolbarScrollBehavior,
    badges: Map<DIYTabs, Boolean> = emptyMap()
) {
    var expanded by remember { mutableStateOf(true) }
    var interactionCount by remember { mutableStateOf(0) }

    // Track which tab was just selected for bump animation
    var bumpingTab by remember { mutableIntStateOf(-1) }
    var bumpKey by remember { mutableIntStateOf(0) }

    // Auto-collapse after 5 seconds
    LaunchedEffect(expanded, interactionCount, currentPage) {
        if (expanded) {
            delay(5000)
            expanded = false
        }
    }

    // Reset bump animation after delay
    LaunchedEffect(bumpKey) {
        if (bumpingTab >= 0) {
            delay(200)
            bumpingTab = -1
        }
    }

    // Expand when the page changes (e.g., via swipe)
    LaunchedEffect(currentPage) {
        if (!expanded) expanded = true
    }

    // Animated values for bouncy feel
    val toolbarScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "toolbar_scale"
    )

    HorizontalFloatingToolbar(
        modifier = modifier
            .graphicsLayer {
                scaleX = toolbarScale
                scaleY = toolbarScale
            },
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        ),
        content = {
            // FIXED ORDER LOOP to prevent shifting
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentPage == index
                val isBumping = bumpingTab == index

                // Animate scale for non-selected tabs when collapsing/expanding
                val itemScale by animateFloatAsState(
                    targetValue = when {
                        isBumping -> 1.28f // Subtle bump animation when selected
                        isSelected -> 1.2f
                        expanded -> 1.2f
                        else -> 0f // Scale down to 0 when collapsed
                    },
                    animationSpec = spring(
                        dampingRatio = if (isBumping) Spring.DampingRatioMediumBouncy else Spring.DampingRatioLowBouncy,
                        stiffness = if (isBumping) Spring.StiffnessHigh else Spring.StiffnessLow
                    ),
                    label = "item_scale_$index"
                )

                // Animate alpha for smooth fade
                val itemAlpha by animateFloatAsState(
                    targetValue = if (expanded || isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "item_alpha_$index"
                )

                // Animate width for spacing
                val itemWidth by animateDpAsState(
                    targetValue = if (expanded || isSelected) 48.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "item_width_$index"
                )

                // Animate spacer width
                val spacerWidth by animateDpAsState(
                    targetValue = if (expanded && index < tabs.size - 1) 16.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "spacer_width_$index"
                )

                // Always render the button, but animate its visibility
                if (itemWidth > 0.dp || isSelected) {
                    IconButton(
                        onClick = {
                            interactionCount++
                            if (!expanded) {
                                expanded = true
                            } else {
                                bumpingTab = index
                                bumpKey++ 
                                onTabSelected(index)
                            }
                        },
                        modifier = Modifier
                            .width(itemWidth)
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemAlpha
                            },
                        colors = if (isSelected) {
                            IconButtonDefaults.filledIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        } else {
                            IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.background,
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Box {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = stringResource(id = tab.title),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            if (badges[tab] == true) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    drawCircle(
                                        color = if (isSelected) Color.Red else Color.Red, // Always red for now
                                    )
                                }
                            }
                        }
                    }

                    // Animated spacing between buttons
                    if (index < tabs.size - 1) {
                        Spacer(modifier = Modifier.width(spacerWidth))
                    }
                }
            }
        }
    )
}
