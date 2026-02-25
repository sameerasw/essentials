package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.domain.DIYTabs

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
    // Persistent visibility
    var expanded by remember { mutableStateOf(true) }

    HorizontalFloatingToolbar(
        modifier = modifier
            .windowInsetsPadding(
                androidx.compose.foundation.layout.WindowInsets.navigationBars
            ),
        expanded = expanded,
//        scrollBehavior = scrollBehavior,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContentColor = MaterialTheme.colorScheme.onSurface,
            toolbarContainerColor = MaterialTheme.colorScheme.primary,
        ),
        content = {
            // FIXED ORDER LOOP to prevent shifting
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentPage == index

                // Animate width for spacing
                val itemWidth by animateDpAsState(
                    targetValue = if (expanded || isSelected) 48.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "item_width_$index"
                )

                // Animate label width for active tab
                val labelWidth by animateDpAsState(
                    targetValue = if (isSelected) 80.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "label_width_$index"
                )

                // Animate spacer width
                val spacerWidth by animateDpAsState(
                    targetValue = if (index < tabs.size - 1) 8.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "spacer_width_$index"
                )

                // Always render the button, but animate its visibility
                if (itemWidth > 0.dp || isSelected) {
                    IconButton(
                        onClick = {
                            onTabSelected(index)
                        },
                        modifier = Modifier
                            .width(itemWidth + labelWidth)
                            .height(48.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
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
                                            color = Color.Red,
                                        )
                                    }
                                }
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = tab.title),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
