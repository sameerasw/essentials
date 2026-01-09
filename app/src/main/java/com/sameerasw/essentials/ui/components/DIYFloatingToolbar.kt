package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
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
    scrollBehavior: FloatingToolbarScrollBehavior
) {
    var expanded by remember { mutableStateOf(true) }
    var interactionCount by remember { mutableStateOf(0) }

    // Auto-collapse after 5 seconds
    LaunchedEffect(expanded, interactionCount, currentPage) {
        if (expanded) {
            delay(5000)
            expanded = false
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
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        content = {
            // FIXED ORDER LOOP to prevent shifting
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentPage == index
                
                // Only show if expanded OR if this is the currently selected tab
                if (expanded || isSelected) {

                    IconButton(
                        onClick = {
                            interactionCount++
                            if (!expanded) {
                                expanded = true
                            } else {
                                onTabSelected(index)
                            }
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .scale(1.2f),
                        colors = if (isSelected) {
                            IconButtonDefaults.filledTonalIconButtonColors()
                        } else {
                            IconButtonDefaults.iconButtonColors()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Add spacing between buttons when expanded
                    if (expanded && index < tabs.size - 1) {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    )
}
