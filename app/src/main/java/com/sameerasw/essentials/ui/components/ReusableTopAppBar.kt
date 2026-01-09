package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReusableTopAppBar(
    title: String,
    hasBack: Boolean = false,
    hasSearch: Boolean = true,
    hasSettings: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onUpdateClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    hasUpdateAvailable: Boolean = false,
    hasHelp: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    collapsedFraction > 0.5f

    LargeFlexibleTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.padding(horizontal = 8.dp),
        expandedHeight = if (subtitle != null) 200.dp else 160.dp,
        collapsedHeight = 64.dp,

        title = {
            if (subtitle != null) {
                // Show title and subtitle
                Column {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Show only title
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (hasBack) {
                val view = LocalView.current
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onBackClick?.invoke()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        actions = {
            actions()
            
            if (hasHelp) {
                val view = LocalView.current
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onHelpClick?.invoke()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_help_24),
                        contentDescription = "Help Guide",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (hasHelp && (hasUpdateAvailable || hasSettings)) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (hasUpdateAvailable) {
                val view = LocalView.current
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onUpdateClick?.invoke()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mobile_arrow_down_24),
                            contentDescription = "Update Available",
                            modifier = Modifier.size(32.dp)
                        )
                        // Red dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red, CircleShape)
                        )
                    }
                }
            }

            if (hasUpdateAvailable && hasSettings) {
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (hasSettings) {
                val view = LocalView.current
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSettingsClick?.invoke()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_settings_heart_24),
                        contentDescription = "Settings",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}
