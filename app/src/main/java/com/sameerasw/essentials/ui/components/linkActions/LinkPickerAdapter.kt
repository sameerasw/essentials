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
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.WindowingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.Collator
import java.util.Locale

private data class LinkActionItem(
    val titleRes: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
)

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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun LinkPickerScreen(
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    demo: Boolean = false,
    initialTab: Int = 0,
    initialOpenShorten: Boolean = false,
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
    var selectedTab by remember { mutableIntStateOf(if (initialOpenShorten) 2 else initialTab) }
    var autoOpenShortenInTools by remember { mutableStateOf(initialOpenShorten) }

    // Preview data state
    var linkPreviewData by remember { mutableStateOf<LinkPreviewData?>(null) }
    var isLoadingPreview by remember { mutableStateOf(true) }

    // App lists
    var baseOpenWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var baseShareWithApps by remember { mutableStateOf<List<ResolvedAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    Log.d(TAG, "LinkPickerScreen called with demo = $demo, URI = $currentUri")

    LaunchedEffect(currentUri) {
        isLoadingApps = true
        isLoadingPreview = true
        linkPreviewData = null

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

            // Fetch preview data asynchronously and smoothly update when ready
            val preview = fetchLinkPreviewData(currentUri)
            withContext(Dispatchers.Main) {
                linkPreviewData = preview
                isLoadingPreview = false
            }
        }
    }

    // Pinned packages state
    val pinnedPackages = remember { mutableStateOf(getPinnedPackages(context)) }
    var isGridView by remember { mutableStateOf(getShareViewModeGrid(context)) }

    // Sorted and filtered apps
    val openWithApps =
        remember(baseOpenWithApps, pinnedPackages.value, searchQuery, currentUri) {
            baseOpenWithApps
                .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
                .sortedWith(
                    compareBy(
                        { !isAppRecommendedForUri(currentUri, it.resolveInfo.activityInfo.packageName, it.label) },
                        { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) },
                    )
                )
        }

    val shareWithApps =
        remember(baseShareWithApps, pinnedPackages.value, searchQuery) {
            baseShareWithApps
                .filter { searchQuery.isEmpty() || it.label.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy { !pinnedPackages.value.contains(it.resolveInfo.activityInfo.packageName) })
        }
    val tabItems = remember { listOf(0, 1) }

    val isFloatingSupported = remember { WindowingUtils.isFloatingModeSupported(context) }
    var showQrSheet by remember { mutableStateOf(false) }
    var showShortenSheet by remember { mutableStateOf(initialOpenShorten) }

    val settingsRepository = remember { SettingsRepository(context) }
    var isMacConnected by remember {
        mutableStateOf(
            settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED, false) &&
                settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED, false)
        )
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED ||
                key == SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED
            ) {
                isMacConnected =
                    settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED, false) &&
                        settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED, false)
            }
        }
        settingsRepository.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            settingsRepository.unregisterOnSharedPreferenceChangeListener(listener)
        }
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
        var isPreviewImageLoaded by remember { mutableStateOf(false) }

        val animatedAlpha by animateFloatAsState(
            targetValue = if (isPreviewImageLoaded) 1f else 0f,
            animationSpec = tween(durationMillis = 600),
            label = "PreviewAlpha",
        )

        val textExpansionAlpha =
            if (sheetOffset != null) {
                val offsetDp = with(density) { sheetOffset.toDp() }
                ((offsetDp.value - 120f) / 80f).coerceIn(0f, 1f)
            } else {
                1f
            }

        val defaultScrim =
            if (isDarkTheme) {
                BottomSheetDefaults.ScrimColor
            } else {
                MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
            }

        val dynamicScrimColor =
            if (isPreviewImageLoaded) {
                defaultScrim.copy(alpha = defaultScrim.alpha * (1f - animatedAlpha))
            } else {
                defaultScrim
            }

        val pagerScope = rememberCoroutineScope()
        val pagerState =
            rememberPagerState(
                initialPage = initialTab.coerceIn(0, tabItems.lastIndex),
                pageCount = { tabItems.size },
            )

        LaunchedEffect(pagerState.currentPage) {
            if (selectedTab != pagerState.currentPage) {
                selectedTab = pagerState.currentPage
            }
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
                        .clip(RoundedCornerShape(24.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                                Crossfade(
                                    targetState = if (isLoadingPreview) "loading" else (linkPreviewData?.faviconUrl ?: "icon"),
                                    label = "FaviconCrossfade",
                                ) { state ->
                                    if (state == "loading") {
                                        LoadingIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    } else if (state != "icon" && !linkPreviewData?.faviconUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model =
                                                ImageRequest.Builder(context)
                                                    .data(linkPreviewData?.faviconUrl)
                                                    .crossfade(true)
                                                    .build(),
                                            contentDescription = "Website Icon",
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_link_24),
                                            contentDescription = "Link Icon",
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
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

                        val actionItems = remember(isFloatingSupported, isMacConnected) {
                            listOfNotNull(
                                if (isMacConnected) {
                                    LinkActionItem(
                                        titleRes = R.string.action_send_to_mac,
                                        iconRes = R.drawable.rounded_devices_24,
                                        onClick = {
                                            val shareIntent =
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, currentUri.toString())
                                                    setPackage("com.sameerasw.airsync")
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                            try {
                                                context.startActivity(shareIntent)
                                                onFinish()
                                            } catch (_: Exception) {
                                                try {
                                                    val broadcastIntent =
                                                        Intent("com.sameerasw.airsync.action.SEND_LINK").apply {
                                                            putExtra("url", currentUri.toString())
                                                            setPackage("com.sameerasw.airsync")
                                                        }
                                                    context.sendBroadcast(broadcastIntent)
                                                    onFinish()
                                                } catch (e: Exception) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.download_airsync),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                }
                                            }
                                        },
                                    )
                                } else null,
                                LinkActionItem(
                                    titleRes = R.string.shorten_copy_link,
                                    iconRes = R.drawable.rounded_content_copy_24,
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
                                                context.getString(R.string.action_copy_clipboard),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                ),
                                LinkActionItem(
                                    titleRes = R.string.action_edit,
                                    iconRes = R.drawable.rounded_edit_24,
                                    onClick = {
                                        editingText = currentUri.toString()
                                        showEditSheet = true
                                    },
                                ),
                                if (isFloatingSupported) {
                                    LinkActionItem(
                                        titleRes = R.string.action_preview_web,
                                        iconRes = R.drawable.rounded_open_in_browser_24,
                                        onClick = {
                                            val bubblesEnabled = WindowingUtils.areNotificationBubblesEnabled(context)
                                            val canWriteSecure = PermissionUtils.canWriteSecureSettings(context)
                                            if (bubblesEnabled || canWriteSecure) {
                                                WindowingUtils.launchOverlayWindow(context, currentUri, isPrivate = true)
                                                onFinish()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.preview_web_desc_disabled),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        },
                                    )
                                } else null,
                                LinkActionItem(
                                    titleRes = R.string.qr_code_title,
                                    iconRes = R.drawable.rounded_qr_code_24,
                                    onClick = {
                                        showQrSheet = true
                                    },
                                ),
                                LinkActionItem(
                                    titleRes = R.string.shorten_root_button,
                                    iconRes = R.drawable.rounded_smart_button_24,
                                    onClick = {
                                        showShortenSheet = true
                                    },
                                ),
                            )
                        }

                        val actionCarouselState = rememberCarouselState { actionItems.size }
                        HorizontalMultiBrowseCarousel(
                            state = actionCarouselState,
                            preferredItemWidth = 110.dp,
                            itemSpacing = 4.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                        ) { index ->
                            val action = actionItems[index]
                            Surface(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    action.onClick()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceBright,
                                modifier = Modifier.fillMaxSize().maskClip(RoundedCornerShape(16.dp)),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = action.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(id = action.titleRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoadingApps) {
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
                    if (openWithApps.isNotEmpty()) {
                        val openWithCarouselState = rememberCarouselState { openWithApps.size }
                        HorizontalMultiBrowseCarousel(
                            state = openWithCarouselState,
                            preferredItemWidth = 68.dp,
                            itemSpacing = 4.dp,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(68.dp),
                        ) { index ->
                            val appInfo = openWithApps[index]
                            val packageName = appInfo.resolveInfo.activityInfo.packageName
                            val isPinned = pinnedPackages.value.contains(packageName)
                            val isRecommended = remember(currentUri, packageName, appInfo.label) {
                                isAppRecommendedForUri(currentUri, packageName, appInfo.label)
                            }
                            var icon by remember(appInfo.resolveInfo) { mutableStateOf<Drawable?>(null) }
                            LaunchedEffect(appInfo.resolveInfo) {
                                withContext(Dispatchers.IO) {
                                    icon = appInfo.resolveInfo.loadIcon(context.packageManager)
                                }
                            }

                            val itemBgColor =
                                if (isRecommended) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else if (isPinned) {
                                    if (isDarkTheme) Color.Black else Color.White
                                } else {
                                    MaterialTheme.colorScheme.surfaceBright
                                }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = itemBgColor,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .maskClip(RoundedCornerShape(20.dp))
                                        .combinedClickable(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                val intent = Intent(Intent.ACTION_VIEW, currentUri)
                                                intent.setClassName(
                                                    appInfo.resolveInfo.activityInfo.packageName,
                                                    appInfo.resolveInfo.activityInfo.name,
                                                )
                                                context.startActivity(intent)
                                                onFinish()
                                            },
                                            onLongClick = {
                                                togglePin(packageName)
                                            },
                                        ),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (icon != null) {
                                        AsyncImage(
                                            model = icon,
                                            contentDescription = appInfo.label,
                                            modifier = Modifier.size(42.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp, bottom = 0.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.label_share_with),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ToggleButton(
                                checked = !isGridView,
                                onCheckedChange = {
                                    HapticUtil.performUIHaptic(view)
                                    isGridView = false
                                    setShareViewModeGrid(context, false)
                                },
                                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_view_headline_24),
                                    contentDescription = stringResource(R.string.label_view_list),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            ToggleButton(
                                checked = isGridView,
                                onCheckedChange = {
                                    HapticUtil.performUIHaptic(view)
                                    isGridView = true
                                    setShareViewModeGrid(context, true)
                                },
                                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_grid_view_24),
                                    contentDescription = stringResource(R.string.label_view_grid),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    ShareWithContent(
                        resolveInfos = shareWithApps,
                        uri = currentUri,
                        onFinish = onFinish,
                        modifier = Modifier.fillMaxWidth(),
                        togglePin = togglePin,
                        pinnedPackages = pinnedPackages.value,
                        isGridView = isGridView,
                        demo = demo,
                    )
                }
            }
        }

        if (linkPreviewData?.imageUrl != null || animatedAlpha > 0f) {
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
                            showGradientOverlay = false,
                        ).progressiveBlur(
                            blurRadius = 40f,
                            height = bottomBlurHeightPx,
                            direction = BlurDirection.BOTTOM,
                            showGradientOverlay = false,
                        ),
            ) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(linkPreviewData?.imageUrl)
                            .crossfade(true)
                            .build(),
                    contentDescription = "Link Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { isPreviewImageLoaded = true },
                    onError = { isPreviewImageLoaded = false },
                )
            }

            if (!linkPreviewData?.title.isNullOrBlank() || !linkPreviewData?.description.isNullOrBlank()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(topAreaHeight)
                            .align(Alignment.TopCenter)
                            .alpha(animatedAlpha * textExpansionAlpha)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (!linkPreviewData?.title.isNullOrBlank()) {
                                Text(
                                    text = linkPreviewData?.title.orEmpty(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (!linkPreviewData?.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = linkPreviewData?.description.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShortenSheet) {
        ShortenUrlSheet(
            uri = currentUri,
            onDismiss = { showShortenSheet = false },
        )
    }

    if (showQrSheet) {
        QrCodeSheet(
            contentUri = currentUri.toString(),
            onDismiss = { showQrSheet = false },
        )
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
                }.distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }

        Log.d(TAG, "Share apps after filtering: ${filtered.size}")

        // Map to ResolvedAppInfo and sort by package/app label first, then by specific action label
        val collator = Collator.getInstance(Locale.getDefault())
        val resolvedList =
            filtered
                .map {
                    ResolvedAppInfo(it, it.loadLabel(pm).toString())
                }.sortedWith { o1, o2 ->
                    val pkg1 = o1.resolveInfo.activityInfo.packageName
                    val pkg2 = o2.resolveInfo.activityInfo.packageName
                    if (pkg1 == pkg2) {
                        collator.compare(
                            o1.label.lowercase(Locale.getDefault()),
                            o2.label.lowercase(Locale.getDefault()),
                        )
                    } else {
                        val appLabel1 = try {
                            pm.getApplicationLabel(o1.resolveInfo.activityInfo.applicationInfo).toString()
                        } catch (_: Exception) {
                            pkg1
                        }
                        val appLabel2 = try {
                            pm.getApplicationLabel(o2.resolveInfo.activityInfo.applicationInfo).toString()
                        } catch (_: Exception) {
                            pkg2
                        }
                        val appComp = collator.compare(
                            appLabel1.lowercase(Locale.getDefault()),
                            appLabel2.lowercase(Locale.getDefault()),
                        )
                        if (appComp != 0) appComp else collator.compare(pkg1, pkg2)
                    }
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

private fun getShareViewModeGrid(context: Context): Boolean {
    val prefs: SharedPreferences = context.getSharedPreferences("link_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("share_view_grid", false)
}

private fun setShareViewModeGrid(
    context: Context,
    isGrid: Boolean,
) {
    val prefs: SharedPreferences = context.getSharedPreferences("link_prefs", Context.MODE_PRIVATE)
    prefs.edit { putBoolean("share_view_grid", isGrid) }
}

private data class LinkPreviewData(
    val imageUrl: String? = null,
    val faviconUrl: String? = null,
    val title: String? = null,
    val description: String? = null,
)

private val faviconCache = androidx.collection.LruCache<String, String>(100)

private fun fetchLinkPreviewData(uri: Uri): LinkPreviewData {
    val urlString = uri.toString()
    val lower = urlString.lowercase(Locale.getDefault())

    // 1. Direct image links
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
        lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".svg") ||
        lower.endsWith(".bmp") || lower.endsWith(".ico")
    ) {
        return LinkPreviewData(imageUrl = urlString)
    }

    if (uri.scheme != "http" && uri.scheme != "https") return LinkPreviewData()

    val host = uri.host?.lowercase(Locale.getDefault()) ?: ""
    val cachedFavicon = if (host.isNotBlank()) faviconCache[host] else null

    // 2. Fast service-specific thumbnail extraction (YouTube, etc.)
    var fastImage: String? = null
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
            fastImage = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
    }

    return try {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }

        val contentType = connection.contentType ?: ""
        if (contentType.startsWith("image/")) {
            return LinkPreviewData(imageUrl = urlString)
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val sb = StringBuilder()
        var line: String?
        var lineCount = 0
        while (reader.readLine().also { line = it } != null && lineCount < 800) {
            sb.append(line).append("\n")
            lineCount++
        }
        reader.close()
        val html = sb.toString()

        var imageUrl: String? = fastImage
        if (imageUrl == null) {
            val ogRegex = Regex("""<meta[^>]+(?:property|name|itemprop)=["'](?:og:image|og:image:secure_url|twitter:image|twitter:image:src|image)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val ogMatch = ogRegex.find(html)?.groupValues?.get(1)
            if (!ogMatch.isNullOrBlank()) {
                imageUrl = resolveRelativeUrl(urlString, cleanHtmlEntity(ogMatch))
            }
        }

        if (imageUrl == null) {
            val ogRegexReversed = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name|itemprop)=["'](?:og:image|og:image:secure_url|twitter:image|twitter:image:src|image)["']""", RegexOption.IGNORE_CASE)
            val ogMatchReversed = ogRegexReversed.find(html)?.groupValues?.get(1)
            if (!ogMatchReversed.isNullOrBlank()) {
                imageUrl = resolveRelativeUrl(urlString, cleanHtmlEntity(ogMatchReversed))
            }
        }

        if (imageUrl == null) {
            val jsonLdImgRegex = Regex(""""image"\s*:\s*(?:\[\s*)?"([^"]+)"""", RegexOption.IGNORE_CASE)
            val jsonLdMatch = jsonLdImgRegex.find(html)?.groupValues?.get(1)
            if (!jsonLdMatch.isNullOrBlank() && jsonLdMatch.startsWith("http")) {
                imageUrl = jsonLdMatch.replace("\\/", "/")
            }
        }

        // Favicon extraction
        var extractedFavicon: String? = cachedFavicon
        if (extractedFavicon == null) {
            val touchIconRegex = Regex("""<link[^>]+rel=["'](?:apple-touch-icon|apple-touch-icon-precomposed|icon|shortcut icon)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val touchIconMatch = touchIconRegex.find(html)?.groupValues?.get(1)
            if (!touchIconMatch.isNullOrBlank()) {
                extractedFavicon = resolveRelativeUrl(urlString, cleanHtmlEntity(touchIconMatch))
            }
        }

        if (extractedFavicon == null) {
            val touchIconReversed = Regex("""<link[^>]+href=["']([^"']+)["'][^>]+rel=["'](?:apple-touch-icon|apple-touch-icon-precomposed|icon|shortcut icon)["']""", RegexOption.IGNORE_CASE)
            val touchIconReversedMatch = touchIconReversed.find(html)?.groupValues?.get(1)
            if (!touchIconReversedMatch.isNullOrBlank()) {
                extractedFavicon = resolveRelativeUrl(urlString, cleanHtmlEntity(touchIconReversedMatch))
            }
        }

        if (extractedFavicon == null && host.isNotBlank()) {
            extractedFavicon = "https://www.google.com/s2/favicons?domain=$host&sz=128"
        }

        if (extractedFavicon != null && host.isNotBlank()) {
            faviconCache.put(host, extractedFavicon)
        }

        if (imageUrl == null) {
            imageUrl = extractedFavicon
        }

        var title: String? = null
        val ogTitleRegex = Regex("""<meta[^>]+(?:property|name)=["'](?:og:title|twitter:title)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val ogTitleMatch = ogTitleRegex.find(html)?.groupValues?.get(1)
        if (!ogTitleMatch.isNullOrBlank()) {
            title = cleanHtmlEntity(ogTitleMatch)
        } else {
            val htmlTitleRegex = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            val htmlTitleMatch = htmlTitleRegex.find(html)?.groupValues?.get(1)
            if (!htmlTitleMatch.isNullOrBlank()) {
                title = cleanHtmlEntity(htmlTitleMatch.trim())
            }
        }

        var description: String? = null
        val ogDescRegex = Regex("""<meta[^>]+(?:property|name)=["'](?:og:description|twitter:description|description)["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val ogDescMatch = ogDescRegex.find(html)?.groupValues?.get(1)
        if (!ogDescMatch.isNullOrBlank()) {
            description = cleanHtmlEntity(ogDescMatch.trim())
        }

        LinkPreviewData(
            imageUrl = imageUrl,
            faviconUrl = extractedFavicon,
            title = title,
            description = description,
        )
    } catch (_: Exception) {
        val fallbackFavicon = cachedFavicon ?: if (host.isNotBlank()) "https://www.google.com/s2/favicons?domain=$host&sz=128" else null
        LinkPreviewData(
            imageUrl = fallbackFavicon,
            faviconUrl = fallbackFavicon,
        )
    }
}

private fun isAppRecommendedForUri(uri: Uri, packageName: String, appLabel: String): Boolean {
    val host = uri.host?.lowercase(Locale.getDefault()) ?: return false
    val pkg = packageName.lowercase(Locale.getDefault())
    val label = appLabel.lowercase(Locale.getDefault())

    // Known domain mappings
    val domainKeywords = when {
        host.contains("instagram.com") || host.contains("instagr.am") -> listOf("instagram")
        host.contains("reddit.com") || host.contains("redd.it") -> listOf("reddit")
        host.contains("facebook.com") || host.contains("fb.com") || host.contains("fb.watch") -> listOf("facebook", "katana", "orca")
        host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> listOf("twitter")
        host.contains("youtube.com") || host.contains("youtu.be") -> listOf("youtube", "vanced", "revanced", "newpipe")
        host.contains("tiktok.com") -> listOf("tiktok", "musically")
        host.contains("spotify.com") -> listOf("spotify")
        host.contains("github.com") -> listOf("github", "git")
        host.contains("telegram.org") || host.contains("t.me") -> listOf("telegram", "nekogram")
        host.contains("discord.com") || host.contains("discord.gg") -> listOf("discord", "aliucord")
        host.contains("threads.net") -> listOf("threads", "barcelona")
        host.contains("pinterest.com") || host.contains("pin.it") -> listOf("pinterest")
        host.contains("linkedin.com") || host.contains("lnkd.in") -> listOf("linkedin")
        host.contains("twitch.tv") -> listOf("twitch")
        host.contains("medium.com") -> listOf("medium")
        host.contains("netflix.com") -> listOf("netflix")
        host.contains("amazon.") -> listOf("amazon")
        else -> {
            // General domain extractor (e.g., "sub.example.co.uk" -> "example")
            val parts = host.split('.').filter { it != "www" && it != "m" && it != "mobile" && it != "app" && it.length > 2 }
            parts
        }
    }

    return domainKeywords.any { keyword ->
        keyword.length >= 3 && (pkg.contains(keyword) || label.contains(keyword))
    }
}

private fun cleanHtmlEntity(text: String): String {
    return try {
        val unescaped = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        unescaped
            .replace("&#x2F;", "/")
            .replace("\\/", "/")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    } catch (_: Exception) {
        text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#x2F;", "/")
            .replace("\\/", "/")
            .trim()
    }
}

private fun resolveRelativeUrl(baseUrl: String, relativeUrl: String): String {
    return try {
        URL(URL(baseUrl), relativeUrl).toString()
    } catch (_: Exception) {
        relativeUrl
    }
}
