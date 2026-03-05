package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsFloatingToolbar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: (@Composable SettingsMenuScope.() -> Unit)? = null,
    fabAction: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current

    if (fabAction != null && fabIconRes != null) {
        HorizontalFloatingToolbar(
            modifier = modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            expanded = true,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        fabAction()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = fabIconRes),
                        contentDescription = fabContentDescription
                    )
                }
            },
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
            ),
            content = {
                ToolbarContent(title, onBackClick)
            }
        )
    } else if (menuContent != null) {
        HorizontalFloatingToolbar(
            modifier = modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 0.dp),
            expanded = true,
            floatingActionButton = {
                Box {
                    FloatingActionButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            menuExpanded = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.large,
                        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_more_vert_24),
                            contentDescription = stringResource(R.string.content_desc_more_options)
                        )
                    }

                    SegmentedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        val scope = SettingsMenuScope(dismissMenu = { menuExpanded = false })
                        scope.menuContent()
                    }
                }
            },
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
            ),
            content = {
                ToolbarContent(title, onBackClick)
            }
        )
    } else {
        // Use the variant without the FAB to avoid reserving space
        HorizontalFloatingToolbar(
            modifier = modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            expanded = true,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
            ),
            content = {
                ToolbarContent(title, onBackClick)
            }
        )
    }
}

@Composable
private fun RowScope.ToolbarContent(
    title: String,
    onBackClick: () -> Unit
) {
    val view = LocalView.current
    IconButton(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            onBackClick()
        },
        modifier = Modifier.align(Alignment.CenterVertically),
        colors = IconButtonDefaults.filledIconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.background
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_arrow_back_24),
            contentDescription = stringResource(R.string.content_desc_back),
            modifier = Modifier.size(24.dp)
        )
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.background,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(min = 100.dp, max = 300.dp)
            .padding(horizontal = 8.dp)
            .align(Alignment.CenterVertically)
    )
}

class SettingsMenuScope(val dismissMenu: () -> Unit) {
    @Composable
    fun MenuItem(
        text: @Composable () -> Unit,
        onClick: () -> Unit,
        leadingIcon: (@Composable () -> Unit)? = null
    ) {
        SegmentedDropdownMenuItem(
            text = text,
            onClick = {
                onClick()
                dismissMenu()
            },
            leadingIcon = leadingIcon
        )
    }
}
