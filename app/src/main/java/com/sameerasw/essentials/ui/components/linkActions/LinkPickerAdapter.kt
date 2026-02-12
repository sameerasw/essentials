package com.sameerasw.essentials.ui.components.linkActions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

private const val TAG = "LinkPickerScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerScreen(uri: Uri, onFinish: () -> Unit, modifier: Modifier = Modifier, demo: Boolean = false) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Mutable state for the current URI
    var currentUri by remember { mutableStateOf(uri) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf(currentUri.toString()) }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    // App lists
    var baseOpenWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var baseShareWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    Log.d(TAG, "LinkPickerScreen called with demo = $demo")
    Log.d(TAG, "LinkPickerScreen created with URI: $currentUri")

    // Query apps whenever currentUri changes
    LaunchedEffect(currentUri) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            val open = queryOpenWithApps(context, currentUri)
            val share = queryShareWithApps(context, currentUri)
            withContext(Dispatchers.Main) {
                baseOpenWithApps = open
                baseShareWithApps = share
                isLoadingApps = false
            }
        }
    }

    // Pinned packages state
    val pinnedPackages = remember { mutableStateOf(getPinnedPackages(context)) }

    // Sorted and filtered apps
    val openWithApps = remember(baseOpenWithApps, pinnedPackages.value, searchQuery) {
        baseOpenWithApps
            .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareBy { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) })
    }

    val shareWithApps = remember(baseShareWithApps, pinnedPackages.value, searchQuery) {
        baseShareWithApps
            .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareBy { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) })
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                            Icon(
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
            // Link display and Edit action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link display container
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Link",
                                    currentUri.toString()
                                )
                            )
                            Toast
                                .makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT)
                                .show()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_link_24),
                            contentDescription = "Link Icon",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = if (demo) {
                                "Long press an app to pin/ unpin"
                            } else {
                                currentUri.toString()
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Edit Button Container
                Card(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable {
                            editingText = currentUri.toString()
                            showEditSheet = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_edit_24),
                            contentDescription = "Edit Link",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = { 
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = "Search"
                    ) 
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (isLoadingApps) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                // HorizontalPager fills remaining space
                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            OpenWithContent(openWithApps, currentUri, onFinish, Modifier, togglePin, pinnedPackages.value, demo)
                        }
                        1 -> {
                            ShareWithContent(shareWithApps, currentUri, onFinish, Modifier, togglePin, pinnedPackages.value, demo)
                        }
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(300) // Wait for sheet animation
            focusRequester.requestFocus()
        }

        ModalBottomSheet(
            onDismissRequest = { },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Link",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilledIconButton(
                        onClick = {
                            var text = editingText.trim()
                            
                            if (text.contains(" ")) {
                                Toast.makeText(context, "Invalid Link: Contains spaces", Toast.LENGTH_SHORT).show()
                                return@FilledIconButton
                            }

                            if (text.isNotEmpty()) {
                                // Add https if no scheme is present
                                if (!text.contains("://")) {
                                    text = "https://$text"
                                }
                                
                                try {
                                    val newUri = Uri.parse(text)
                                    // Validate scheme
                                    if (newUri.scheme.isNullOrBlank()) {
                                        Toast.makeText(context, "Invalid Link: Missing scheme", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentUri = newUri
                                        showEditSheet = false
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Invalid URI", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_save_24),
                            contentDescription = "Save changes",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("URL") },
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

private fun queryOpenWithApps(context: Context, uri: Uri): List<ResolvedAppInfo> {
    if (uri.scheme.isNullOrBlank()) return emptyList()
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
        } catch (_: Exception) {
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

        // Map to ResolvedAppInfo and sort
        val collator = Collator.getInstance(Locale.getDefault())
        val resolvedList = filtered.map { 
            ResolvedAppInfo(it, it.loadLabel(pm).toString()) 
        }.sortedWith { o1, o2 ->
            collator.compare(
                o1.label.lowercase(Locale.getDefault()),
                o2.label.lowercase(Locale.getDefault())
            )
        }

        Log.d(TAG, "Final open with apps: ${resolvedList.size}")
        resolvedList
    } catch (e: Exception) {
        Log.e(TAG, "Error querying open with apps", e)
        emptyList()
    }
}

private fun queryShareWithApps(context: Context, uri: Uri): List<ResolvedAppInfo> {
    if (uri.scheme.isNullOrBlank()) return emptyList()
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
        } catch (_: Exception) {
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

        // Map to ResolvedAppInfo and sort
        val collator = Collator.getInstance(Locale.getDefault())
        val resolvedList = filtered.map { 
            ResolvedAppInfo(it, it.loadLabel(pm).toString()) 
        }.sortedWith { o1, o2 ->
            collator.compare(
                o1.label.lowercase(Locale.getDefault()),
                o2.label.lowercase(Locale.getDefault())
            )
        }

        Log.d(TAG, "Final share with apps: ${resolvedList.size}")
        resolvedList
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
    prefs.edit { putStringSet("pinned_packages", packages) }
}
