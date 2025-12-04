package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.sameerasw.essentials.R
import androidx.compose.ui.unit.dp

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
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),

        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (hasBack) {
                // Increase the hit target and icon size for the back button
                IconButton(onClick = { onBackClick?.invoke() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                        contentDescription = "Back",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        actions = {
            if (hasSearch) {
                // Increase hit target and icon size for the search button
                IconButton(onClick = { onSearchClick?.invoke() }, modifier = Modifier.size(64.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = "Search",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Add Settings button to the right of Search when requested
            if (hasSettings) {
                IconButton(onClick = { onSettingsClick?.invoke() }, modifier = Modifier.size(64.dp)) {
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
