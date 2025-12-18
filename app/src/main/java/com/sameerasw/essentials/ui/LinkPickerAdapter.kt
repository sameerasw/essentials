package com.sameerasw.essentials.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

private const val TAG = "LinkPickerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerScreen(uri: Uri, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Log.d(TAG, "LinkPickerScreen created with URI: $uri")

    // Query apps once when composable is created
    val baseOpenWithApps = remember {
        queryOpenWithApps(context, uri).also {
            Log.d(TAG, "Open with apps: ${it.size}")
            it.forEach { app ->
                Log.d(TAG, "  - ${app.activityInfo.loadLabel(context.packageManager)} (${app.activityInfo.packageName})")
            }
        }
    }

    val baseShareWithApps = remember {
        queryShareWithApps(context, uri).also {
            Log.d(TAG, "Share with apps: ${it.size}")
            it.forEach { app ->
                Log.d(TAG, "  - ${app.activityInfo.loadLabel(context.packageManager)} (${app.activityInfo.packageName})")
            }
        }
    }

    // Pinned packages state
    val pinnedPackages = remember { mutableStateOf(getPinnedPackages(context)) }

    // Sorted apps: pinned first, then unpinned, both alphabetical
    val openWithApps = remember(baseOpenWithApps, pinnedPackages.value) {
        baseOpenWithApps.sortedWith(compareBy<ResolveInfo> { !pinnedPackages.value.contains(it.activityInfo.packageName) }.thenBy { it.loadLabel(context.packageManager).toString().lowercase() })
    }

    val shareWithApps = remember(baseShareWithApps, pinnedPackages.value) {
        baseShareWithApps.sortedWith(compareBy<ResolveInfo> { !pinnedPackages.value.contains(it.activityInfo.packageName) }.thenBy { it.loadLabel(context.packageManager).toString().lowercase() })
    }

    // toggle pin
    val togglePin: (String) -> Unit = { packageName ->
        val current = pinnedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setPinnedPackages(context, current)
        pinnedPackages.value = current
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Pager state for swiping between tabs
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            ReusableTopAppBar(
                title = "Link Actions",
                hasBack = true,
                hasSearch = false,
                hasSettings = false,
                onBackClick = onFinish,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val items = listOf("Open With", "Share With")
            val selectedIcons = listOf(R.drawable.rounded_open_in_browser_24, R.drawable.rounded_share_24)
            val unselectedIcons = listOf(R.drawable.rounded_open_in_browser_24, R.drawable.rounded_share_24)

            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            val selected = pagerState.currentPage == index
                            val iconOffset by animateDpAsState(
                                targetValue = if (selected) 0.dp else 2.dp,
                                label = "NavIconOffset"
                            )
                            androidx.compose.material3.Icon(
                                painter = painterResource(if (selected) selectedIcons[index] else unselectedIcons[index]),
                                contentDescription = item,
                                modifier = Modifier.offset(y = iconOffset)
                            )
                        },
                        label = {
                            val selected = pagerState.currentPage == index
                            val alpha by animateFloatAsState(
                                targetValue = if (selected) 1f else 0f,
                                label = "NavLabelAlpha"
                            )
                            Text(item, modifier = Modifier.alpha(alpha))
                        },
                        alwaysShowLabel = true,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Track page changes for haptic feedback on swipe
        LaunchedEffect(pagerState.currentPage) {
            snapshotFlow { pagerState.currentPage }.collect { _ ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Link display container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Link", uri.toString()))
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_link_24),
                        contentDescription = "Link Icon",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = uri.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // HorizontalPager fills remaining space
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = pagerState,
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) { page ->
                when (page) {
                    0 -> {
                        OpenWithContent(openWithApps, uri, onFinish, Modifier, togglePin, pinnedPackages.value)
                    }
                    1 -> {
                        ShareWithContent(shareWithApps, uri, onFinish, Modifier, togglePin, pinnedPackages.value)
                    }
                }
            }
        }
    }
}

private fun queryOpenWithApps(context: android.content.Context, uri: Uri): List<ResolveInfo> {
    return try {
        val pm = context.packageManager
        val ourPackageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, uri)

        Log.d(TAG, "Querying OPEN_WITH for: $uri")
        Log.d(TAG, "Our package: $ourPackageName")

        // Try different flags combinations
        val resolves = try {
            pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            )
        } catch (e: Exception) {
            Log.d(TAG, "MATCH_ALL | MATCH_DISABLED_UNTIL_USED_COMPONENTS failed, trying MATCH_ALL")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        Log.d(TAG, "Total apps before filtering: ${resolves.size}")

        val filtered = resolves
            .filter {
                val shouldInclude = it.activityInfo.packageName != ourPackageName
                if (!shouldInclude) {
                    Log.d(TAG, "Filtering out our own app: ${it.activityInfo.packageName}")
                }
                shouldInclude
            }
            .distinctBy { it.activityInfo.packageName }

        Log.d(TAG, "Apps after filtering: ${filtered.size}")

        // Sort by label
        val collator = Collator.getInstance(Locale.getDefault())
        val sorted = filtered.sortedWith { o1, o2 ->
            collator.compare(
                o1.activityInfo.loadLabel(pm).toString().lowercase(Locale.getDefault()),
                o2.activityInfo.loadLabel(pm).toString().lowercase(Locale.getDefault())
            )
        }

        Log.d(TAG, "Final open with apps: ${sorted.size}")
        sorted
    } catch (e: Exception) {
        Log.e(TAG, "Error querying open with apps", e)
        emptyList()
    }
}

private fun queryShareWithApps(context: android.content.Context, uri: Uri): List<ResolveInfo> {
    return try {
        val pm = context.packageManager
        val ourPackageName = context.packageName

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri.toString())
        }

        Log.d(TAG, "Querying SHARE_WITH for: $uri")

        val resolves = try {
            pm.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
            )
        } catch (e: Exception) {
            Log.d(TAG, "MATCH_ALL | MATCH_DISABLED_UNTIL_USED_COMPONENTS failed, trying MATCH_ALL")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        Log.d(TAG, "Total share apps before filtering: ${resolves.size}")

        val filtered = resolves
            .filter {
                val shouldInclude = it.activityInfo.packageName != ourPackageName
                if (!shouldInclude) {
                    Log.d(TAG, "Filtering out our own app from share: ${it.activityInfo.packageName}")
                }
                shouldInclude
            }
            .distinctBy { it.activityInfo.packageName }

        Log.d(TAG, "Share apps after filtering: ${filtered.size}")

        // Sort by label
        val collator = Collator.getInstance(Locale.getDefault())
        val sorted = filtered.sortedWith { o1, o2 ->
            collator.compare(
                o1.activityInfo.loadLabel(pm).toString().lowercase(Locale.getDefault()),
                o2.activityInfo.loadLabel(pm).toString().lowercase(Locale.getDefault())
            )
        }

        Log.d(TAG, "Final share with apps: ${sorted.size}")
        sorted
    } catch (e: Exception) {
        Log.e(TAG, "Error querying share with apps", e)
        emptyList()
    }
}

private fun getPinnedPackages(context: Context): Set<String> {
    val prefs: SharedPreferences = context.getSharedPreferences("link_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("pinned_packages", emptySet()) ?: emptySet()
}

private fun setPinnedPackages(context: Context, packages: Set<String>) {
    val prefs: SharedPreferences = context.getSharedPreferences("link_prefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("pinned_packages", packages).apply()
}
