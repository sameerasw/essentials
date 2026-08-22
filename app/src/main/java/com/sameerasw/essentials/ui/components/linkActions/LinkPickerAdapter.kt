/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: LinkPickerAdapter.kt
 * Description: UI layout element for LinkPickerAdapter.kt.
 */

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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.Collator
import java.util.Locale

private const val TAG = "LinkPickerScreen"

private val TRACKING_PARAMS =
    setOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "fbclid",
        "gclid",
        "igsh",
        "si",
        "ref",
        "ref_src",
        "source",
        "feature",
        "tracking_id",
    )

private fun hasTrackingParameters(uri: Uri): Boolean {
    return try {
        uri.queryParameterNames.any { it.lowercase(Locale.getDefault()) in TRACKING_PARAMS }
    } catch (_: Exception) {
        false
    }
}

private fun cleanTrackingParams(uri: Uri): Uri {
    return try {
        val queryParamNames = uri.queryParameterNames
        if (queryParamNames.none { it.lowercase(Locale.getDefault()) in TRACKING_PARAMS }) return uri

        val builder = uri.buildUpon().clearQuery()
        for (key in queryParamNames) {
            if (key.lowercase(Locale.getDefault()) !in TRACKING_PARAMS) {
                val values = uri.getQueryParameters(key)
                for (v in values) {
                    builder.appendQueryParameter(key, v)
                }
            }
        }
        builder.build()
    } catch (_: Exception) {
        uri
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerScreen(
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    demo: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    // Mutable state for the current URI
    var currentUri by remember { mutableStateOf(uri) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editingText by remember { mutableStateOf(currentUri.toString()) }

    // Search & tab state
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Preview image state
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // App lists
    var baseOpenWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var baseShareWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    Log.d(TAG, "LinkPickerScreen called with demo = $demo, URI = $currentUri")

    LaunchedEffect(currentUri) {
        isLoadingApps = true
        previewImageUrl = null

        withContext(Dispatchers.IO) {
            // Load apps immediately so UI is ready without waiting for web scraping
            val openDeferred = async { queryOpenWithApps(context, currentUri) }
            val shareDeferred = async { queryShareWithApps(context, currentUri) }

            val open = openDeferred.await()
            val share = shareDeferred.await()

            withContext(Dispatchers.Main) {
                baseOpenWithApps = open
                baseShareWithApps = share
                isLoadingApps = false
            }

            // Fetch preview image asynchronously and smoothly update when ready
            val preview = fetchPreviewImageUrl(currentUri)
            withContext(Dispatchers.Main) {
                previewImageUrl = preview
            }
        }
    }

    // Pinned packages state
    val pinnedPackages = remember { mutableStateOf(getPinnedPackages(context)) }

    // Sorted and filtered apps
    val openWithApps =
        remember(baseOpenWithApps, pinnedPackages.value, searchQuery) {
            baseOpenWithApps
                .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) })
        }

    val shareWithApps =
        remember(baseShareWithApps, pinnedPackages.value, searchQuery) {
            baseShareWithApps
                .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) })
        }
    val isToolsSupported = remember { com.sameerasw.essentials.utils.WindowingUtils.isFloatingModeSupported(context) }
    val tabItems = remember(isToolsSupported) {
        if (isToolsSupported) listOf(0, 1, 2) else listOf(0, 1)
    }

    // Toggle pin
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

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val imeBottom = WindowInsets.ime.asPaddingValues(density).calculateBottomPadding()

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0.dp) {
            sheetState.expand()
        }
    }

    var lastSheetHapticBucket by remember { mutableIntStateOf(0) }
    LaunchedEffect(sheetState) {
        snapshotFlow {
            try {
                sheetState.requireOffset()
            } catch (_: Exception) {
                null
            }
        }.collect { offset ->
            if (offset != null) {
                val bucket = (offset / with(density) { 32.dp.toPx() }).toInt()
                if (bucket != lastSheetHapticBucket) {
                    HapticUtil.performSliderHaptic(view)
                    lastSheetHapticBucket = bucket
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val sheetOffset =
            try {
                sheetState.requireOffset()
            } catch (_: Exception) {
                null
            }

        val topAreaHeight =
            if (sheetOffset != null) {
                with(density) { sheetOffset.coerceAtLeast(0f).toDp() } + statusBarTop + 28.dp
            } else {
                screenHeightDp * 0.45f + statusBarTop + 28.dp
            }

        val isDarkTheme = isSystemInDarkTheme()

        val animatedAlpha by animateFloatAsState(
            targetValue = if (previewImageUrl != null) 1f else 0f,
            animationSpec = tween(durationMillis = 800),
            label = "PreviewAlpha",
        )

        // Background layer: Preview image with progressive blur that fades in seamlessly without jumping scrim
        if (previewImageUrl != null || animatedAlpha > 0f) {
            val topBlurHeightPx = with(density) { (statusBarTop * 1.5f + 48.dp).toPx() }
            val bottomBlurHeightPx = with(density) { 120.dp.toPx() }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(topAreaHeight)
                        .align(Alignment.TopCenter)
                        .alpha(animatedAlpha)
                        .progressiveBlur(
                            blurRadius = 40f,
                            height = topBlurHeightPx,
                            direction = BlurDirection.TOP,
                            showGradientOverlay = true,
                        ).progressiveBlur(
                            blurRadius = 40f,
                            height = bottomBlurHeightPx,
                            direction = BlurDirection.BOTTOM,
                            showGradientOverlay = true,
                        ),
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(previewImageUrl)
                            .crossfade(true)
                            .build(),
                    contentDescription = "Link Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val dynamicScrimColor =
            if (isDarkTheme) {
                BottomSheetDefaults.ScrimColor
            } else {
                MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
            }

        EssentialsBottomSheet(
            onDismissRequest = onFinish,
            sheetState = sheetState,
            scrimColor = dynamicScrimColor,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            // Tab Selector (Open With / Share With)
            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
            ) {
                SegmentedPicker(
                    items = tabItems,
                    selectedItem = selectedTab,
                    onItemSelected = {
                        selectedTab = it
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    labelProvider = {
                        when (it) {
                            0 -> context.getString(R.string.label_open_with)
                            1 -> context.getString(R.string.label_share_with)
                            else -> context.getString(R.string.label_tools)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Link Preview Card
            val domain = currentUri.host ?: currentUri.scheme ?: "Link"
            val hasTrackingParams = hasTrackingParameters(currentUri)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_link_24),
                                contentDescription = "Link Icon",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = currentUri.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Action buttons (Copy, Clean, Edit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Copy Link Button
                        FilledTonalButton(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Link",
                                        currentUri.toString(),
                                    ),
                                )
                                Toast
                                    .makeText(
                                        context,
                                        "Link copied to clipboard",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_content_copy_24),
                                contentDescription = "Copy",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Copy", maxLines = 1)
                        }

                        // Clean Tracker Button (if tracking params exist)
                        if (hasTrackingParams) {
                            FilledTonalButton(
                                onClick = {
                                    val cleaned = cleanTrackingParams(currentUri)
                                    currentUri = cleaned
                                    editingText = cleaned.toString()
                                    Toast
                                        .makeText(
                                            context,
                                            "Tracking parameters removed",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_auto_awesome_24),
                                    contentDescription = "Clean",
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text("Clean", maxLines = 1)
                            }
                        }

                        // Edit Button
                        FilledTonalButton(
                            onClick = {
                                editingText = currentUri.toString()
                                showEditSheet = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_edit_24),
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Edit", maxLines = 1)
                        }
                    }
                }
            }

            // Search Bar (Only shown for Open and Share tabs)
            if (selectedTab != 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_search_24),
                            contentDescription = "Search",
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            // Apps / Actions List
            if (isLoadingApps && selectedTab != 2) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        OpenWithContent(
                            resolveInfos = openWithApps,
                            uri = currentUri,
                            onFinish = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                            togglePin = togglePin,
                            pinnedPackages = pinnedPackages.value,
                            demo = demo,
                        )
                    }
                    1 -> {
                        ShareWithContent(
                            resolveInfos = shareWithApps,
                            uri = currentUri,
                            onFinish = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                            togglePin = togglePin,
                            pinnedPackages = pinnedPackages.value,
                            demo = demo,
                        )
                    }
                    else -> {
                        if (isToolsSupported) {
                            FloatingWithContent(
                                uri = currentUri,
                                onSelectTab = { selectedTab = it },
                                onFinish = onFinish,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OpenWithContent(
                                resolveInfos = openWithApps,
                                uri = currentUri,
                                onFinish = onFinish,
                                modifier = Modifier.fillMaxWidth(),
                                togglePin = togglePin,
                                pinnedPackages = pinnedPackages.value,
                                demo = demo,
                            )
                        }
                    }
                }
            }
        }
    }
}

    if (showEditSheet) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            delay(300)
            focusRequester.requestFocus()
        }

        @Suppress("DEPRECATION")
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = editSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Edit Link",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    FilledIconButton(
                        onClick = {
                            var text = editingText.trim()

                            if (text.contains(" ")) {
                                Toast
                                    .makeText(
                                        context,
                                        "Invalid Link: Contains spaces",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                return@FilledIconButton
                            }

                            if (text.isNotEmpty()) {
                                if (!text.contains("://")) {
                                    text = "https://$text"
                                }

                                try {
                                    val newUri = Uri.parse(text)
                                    if (newUri.scheme.isNullOrBlank()) {
                                        Toast
                                            .makeText(
                                                context,
                                                "Invalid Link: Missing scheme",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    } else {
                                        currentUri = newUri
                                        showEditSheet = false
                                    }
                                } catch (_: Exception) {
                                    Toast
                                        .makeText(context, "Invalid URI", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_save_24),
                            contentDescription = "Save changes",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    label = { Text("URL") },
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

private fun queryOpenWithApps(
    context: Context,
    uri: Uri,
): List<ResolvedAppInfo> {
    if (uri.scheme.isNullOrBlank()) return emptyList()
    return try {
        val pm = context.packageManager
        val ourPackageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW, uri)

        Log.d(TAG, "Querying OPEN_WITH for: $uri")
        Log.d(TAG, "Our package: $ourPackageName")

        // Try different flags combinations
        val resolves =
            try {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS,
                )
            } catch (_: Exception) {
                Log.d(TAG, "MATCH_ALL | MATCH_DISABLED_UNTIL_USED_COMPONENTS failed, trying MATCH_ALL")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }

        Log.d(TAG, "Total apps before filtering: ${resolves.size}")

        val filtered =
            resolves
                .filter {
                    val shouldInclude = it.activityInfo.packageName != ourPackageName
                    if (!shouldInclude) {
                        Log.d(TAG, "Filtering out our own app: ${it.activityInfo.packageName}")
                    }
                    shouldInclude
                }.distinctBy { it.activityInfo.packageName }

        Log.d(TAG, "Apps after filtering: ${filtered.size}")

        // Map to ResolvedAppInfo and sort
        val collator = Collator.getInstance(Locale.getDefault())
        val resolvedList =
            filtered
                .map {
                    ResolvedAppInfo(it, it.loadLabel(pm).toString())
                }.sortedWith { o1, o2 ->
                    collator.compare(
                        o1.label.lowercase(Locale.getDefault()),
                        o2.label.lowercase(Locale.getDefault()),
                    )
                }

        Log.d(TAG, "Final open with apps: ${resolvedList.size}")
        resolvedList
    } catch (e: Exception) {
        Log.e(TAG, "Error querying open with apps", e)
        emptyList()
    }
}

private fun queryShareWithApps(
    context: Context,
    uri: Uri,
): List<ResolvedAppInfo> {
    if (uri.scheme.isNullOrBlank()) return emptyList()
    return try {
        val pm = context.packageManager
        val ourPackageName = context.packageName

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, uri.toString())
            }

        Log.d(TAG, "Querying SHARE_WITH for: $uri")

        val resolves =
            try {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_ALL or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS,
                )
            } catch (_: Exception) {
                Log.d(TAG, "MATCH_ALL | MATCH_DISABLED_UNTIL_USED_COMPONENTS failed, trying MATCH_ALL")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }

        Log.d(TAG, "Total share apps before filtering: ${resolves.size}")

        val filtered =
            resolves
                .filter {
                    val shouldInclude = it.activityInfo.packageName != ourPackageName
                    if (!shouldInclude) {
                        Log.d(
                            TAG,
                            "Filtering out our own app from share: ${it.activityInfo.packageName}",
                        )
                    }
                    shouldInclude
                }.distinctBy { it.activityInfo.packageName }

        Log.d(TAG, "Share apps after filtering: ${filtered.size}")

        // Map to ResolvedAppInfo and sort
        val collator = Collator.getInstance(Locale.getDefault())
        val resolvedList =
            filtered
                .map {
                    ResolvedAppInfo(it, it.loadLabel(pm).toString())
                }.sortedWith { o1, o2 ->
                    collator.compare(
                        o1.label.lowercase(Locale.getDefault()),
                        o2.label.lowercase(Locale.getDefault()),
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

private fun setPinnedPackages(
    context: Context,
    packages: Set<String>,
) {
    val prefs: SharedPreferences = context.getSharedPreferences("link_prefs", Context.MODE_PRIVATE)
    prefs.edit { putStringSet("pinned_packages", packages) }
}

private fun fetchPreviewImageUrl(uri: Uri): String? {
    val urlString = uri.toString()
    val lower = urlString.lowercase(Locale.getDefault())

    // 1. Direct image links
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
        lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") ||
        lower.endsWith(".bmp") || lower.endsWith(".ico")
    ) {
        return urlString
    }

    if (uri.scheme != "http" && uri.scheme != "https") return null

    val host = uri.host?.lowercase(Locale.getDefault()) ?: ""

    // 2. Fast service-specific thumbnail extraction (YouTube, etc.)
    if (host.contains("youtube.com") || host.contains("youtu.be")) {
        val videoId =
            if (host.contains("youtu.be")) {
                uri.lastPathSegment
            } else {
                uri.getQueryParameter("v") ?: if (uri.pathSegments.contains("shorts") || uri.pathSegments.contains("live") || uri.pathSegments.contains("embed")) {
                    uri.lastPathSegment
                } else null
            }
        if (!videoId.isNullOrBlank()) {
            return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }

    return try {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8")
        }

        val contentType = connection.contentType ?: ""
        if (contentType.startsWith("image/")) {
            return urlString
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val sb = StringBuilder()
        var line: String?
        var lineCount = 0
        while (reader.readLine().also { line = it } != null && lineCount < 300) {
            sb.append(line).append("\n")
            lineCount++
            if (line?.contains("</head>", ignoreCase = true) == true) break
        }
        reader.close()
        val html = sb.toString()

        // 3. OpenGraph / Twitter Card / Schema image
        val ogRegex = Regex("""<meta[^>]+(?:property|name|itemprop)=["'](?:og:image|og:image:secure_url|twitter:image|twitter:image:src|image)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val ogMatch = ogRegex.find(html)?.groupValues?.get(1)
        if (!ogMatch.isNullOrBlank()) {
            return resolveRelativeUrl(urlString, ogMatch)
        }

        val ogRegexReversed = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name|itemprop)=["'](?:og:image|og:image:secure_url|twitter:image|twitter:image:src|image)["']""", RegexOption.IGNORE_CASE)
        val ogMatchReversed = ogRegexReversed.find(html)?.groupValues?.get(1)
        if (!ogMatchReversed.isNullOrBlank()) {
            return resolveRelativeUrl(urlString, ogMatchReversed)
        }

        // 4. Apple Touch Icon / Large icon from HTML
        val touchIconRegex = Regex("""<link[^>]+rel=["'](?:apple-touch-icon|apple-touch-icon-precomposed|icon|shortcut icon)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val touchIconMatch = touchIconRegex.find(html)?.groupValues?.get(1)
        if (!touchIconMatch.isNullOrBlank()) {
            return resolveRelativeUrl(urlString, touchIconMatch)
        }

        val touchIconReversed = Regex("""<link[^>]+href=["']([^"']+)["'][^>]+rel=["'](?:apple-touch-icon|apple-touch-icon-precomposed|icon|shortcut icon)["']""", RegexOption.IGNORE_CASE)
        val touchIconReversedMatch = touchIconReversed.find(html)?.groupValues?.get(1)
        if (!touchIconReversedMatch.isNullOrBlank()) {
            return resolveRelativeUrl(urlString, touchIconReversedMatch)
        }

        // 5. Fallback for any website: High-resolution domain icon via Google favicon service (128px)
        if (host.isNotBlank()) {
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } else {
            null
        }
    } catch (_: Exception) {
        // Fallback on network/parse failure to domain icon
        if (host.isNotBlank()) {
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } else {
            null
        }
    }
}

private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
    return try {
        URL(URL(baseUrl), relativeUrl).toString()
    } catch (_: Exception) {
        relativeUrl
    }
}
