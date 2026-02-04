package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.domain.model.github.GitHubUser
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReusableTopAppBar(
    title: Any, // Can be Int (Resource ID) or String
    hasBack: Boolean = false,
    hasSearch: Boolean = true,
    hasSettings: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onUpdateClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    onGitHubClick: (() -> Unit)? = null,
    hasUpdateAvailable: Boolean = false,
    hasGitHub: Boolean = false,
    gitHubUser: GitHubUser? = null,
    hasHelp: Boolean = false,
    helpIconRes: Int = R.drawable.rounded_help_24,
    helpContentDescription: Int = R.string.action_help_guide,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    subtitle: Any? = null, // Can be Int or String
    isBeta: Boolean = false,
    backIconRes: Int = R.drawable.rounded_arrow_back_24,
    isSmall: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onSignOutClick: (() -> Unit)? = null,
    hasHelpBadge: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val collapsedFraction = scrollBehavior?.state?.collapsedFraction ?: 0f
    collapsedFraction > 0.5f
    
    // Internal state for profile menu
    var showProfileMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val titleContent: @Composable () -> Unit = {
            val resolvedTitle = when (title) {
                is Int -> stringResource(id = title)
                is String -> title
                else -> ""
            }
            if (subtitle != null) {
                // Show title and subtitle
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            resolvedTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isBeta) {
                            androidx.compose.material3.Card(
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = stringResource(R.string.label_beta),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    val resolvedSubtitle = when (subtitle) {
                        is Int -> stringResource(id = subtitle)
                        is String -> subtitle
                        else -> ""
                    }
                    Text(
                        resolvedSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // Show only title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        resolvedTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isBeta) {
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = stringResource(R.string.label_beta),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
    }

    val navigationIconContent: @Composable () -> Unit = {
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
                        painter = painterResource(id = backIconRes),
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
    }

    val actionsContent: @Composable RowScope.() -> Unit = {
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
                    Box {
                        Icon(
                            painter = painterResource(id = helpIconRes),
                            contentDescription = stringResource(helpContentDescription),
                            modifier = Modifier.size(32.dp)
                        )
                        if (hasHelpBadge) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
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
                            contentDescription = stringResource(R.string.update_available_title),
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

            if (hasGitHub) {
                 if (hasUpdateAvailable) {
                     Spacer(modifier = Modifier.width(8.dp))
                 }
                 
                 val view = LocalView.current
                 
                 // Container for Icon and Menu
                 Box {
                     IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            if (gitHubUser != null) {
                                showProfileMenu = true
                            } else {
                                onGitHubClick?.invoke()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (gitHubUser != null) {
                            AsyncImage(
                                model = gitHubUser.avatarUrl,
                                contentDescription = stringResource(R.string.action_profile),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.brand_github),
                                contentDescription = stringResource(R.string.action_sign_in_github),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (gitHubUser != null) {
                        androidx.compose.material3.DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_sign_out)) },
                                onClick = {
                                    onSignOutClick?.invoke()
                                    showProfileMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_logout_24),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                 }
                
                if (hasSettings) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
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
                        contentDescription = stringResource(R.string.content_desc_settings),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
    }

    if (isSmall) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor
            ),
            modifier = Modifier.padding(horizontal = 8.dp),
            title = titleContent,
            navigationIcon = navigationIconContent,
            actions = actionsContent,
            scrollBehavior = scrollBehavior
        )
    } else {
        LargeFlexibleTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor
            ),
            modifier = Modifier.padding(horizontal = 8.dp),
            expandedHeight = if (subtitle != null) 200.dp else 160.dp,
            collapsedHeight = 64.dp,
            title = titleContent,
            navigationIcon = navigationIconContent,
            actions = actionsContent,
            scrollBehavior = scrollBehavior
        )
    }
}
