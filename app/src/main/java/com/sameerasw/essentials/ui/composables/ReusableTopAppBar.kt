package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReusableTopAppBar(
    title: String,
    hasBack: Boolean = false,
    hasSearch: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
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
                IconButton(onClick = { onBackClick?.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            if (hasSearch) {
                IconButton(onClick = { onSearchClick?.invoke() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = "Search"
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}
