/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Wallpaper
 * File: WallpaperScreen.kt
 * Description: UI component and settings composable for Wallpaper feature domain.
 */

package com.sameerasw.essentials.ui.composables.wallpaper

import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.navigation.ProvideBottomBar
import com.sameerasw.essentials.ui.navigation.BottomBarSpec
import com.sameerasw.essentials.ui.navigation.AppRoute
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.sameerasw.essentials.ui.activities.WallpaperStagingActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import com.sameerasw.essentials.services.handlers.WallpaperDockBridge
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Rect
import android.graphics.Rect as AndroidRect
import android.R as AndroidR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Slider
import com.sameerasw.essentials.ui.core.controls.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.SubcomposeAsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.domain.registry.PermissionRegistry
import com.sameerasw.essentials.ui.core.sheets.PermissionItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WallpaperScreen(
    route: AppRoute,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    initialTab: String = "daily",
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val wallpaperInfo by viewModel.dailyWallpaperInfo
    val isLoading by viewModel.isWallpaperLoading
    val isBlurEnabled by viewModel.isBlurEnabled
    val isAutoUpdateEnabled by viewModel.isDailyWallpaperAutoUpdateEnabled
    val dailyWallpaperAutoUpdateTime by viewModel.dailyWallpaperAutoUpdateTime
    val isDailyWallpaperShowLastTime by viewModel.isDailyWallpaperShowLastTime


    // Live Wallpaper Settings State
    var availableVideos by remember { mutableStateOf(repository.getLiveWallpaperAvailableVideos()) }
    var selectedVideo by remember { mutableStateOf(repository.getLiveWallpaperSelectedVideo()) }
    var playbackTrigger by remember { mutableStateOf(repository.getLiveWallpaperPlaybackTrigger()) }
    var progressiveBlurEnabled by remember { mutableStateOf(repository.isLiveWallpaperProgressiveBlurEnabled()) }
    var zoomEffectEnabled by remember { mutableStateOf(repository.isLiveWallpaperZoomEffectEnabled()) }
    var dockEnabled by remember { mutableStateOf(repository.isLiveWallpaperDockEnabled()) }
    var dockPadding by remember { mutableFloatStateOf(repository.getLiveWallpaperDockPadding()) }
    var dockOpacity by remember { mutableFloatStateOf(repository.getLiveWallpaperDockOpacity()) }
    val liveDockBounds by WallpaperDockBridge.bounds.collectAsState()
    var isAccessibilityGranted by remember { mutableStateOf(PermissionUtils.isAccessibilityServiceEnabled(context)) }
    var showDockPermissionSheet by remember { mutableStateOf(false) }
    var progressiveBlurHeight by remember { mutableFloatStateOf(repository.getLiveWallpaperProgressiveBlurHeight()) }
    var applyHome by remember { mutableStateOf(repository.getDailyWallpaperApplyHome()) }
    var applyLock by remember { mutableStateOf(repository.getDailyWallpaperApplyLock()) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var showSettingsCard by remember { mutableStateOf(false) }
    var isDailyLivePreviewExpanded by remember { mutableStateOf(false) }

    val pagerState =
        rememberPagerState(
            initialPage = if (initialTab == "live") 1 else 0,
            pageCount = { 2 },
        )

    val pickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    repository.addLiveWallpaperCustomVideo(it.toString())
                    availableVideos = repository.getLiveWallpaperAvailableVideos()
                    selectedVideo = it.toString()
                    repository.saveLiveWallpaperSelectedVideo(it.toString())
                }
            },
        )

    LaunchedEffect(availableVideos) {
        if (selectedVideo == SettingsRepository.LIVE_WALLPAPER_DEFAULT_VIDEO && availableVideos.isNotEmpty()) {
            val first =
                availableVideos.firstOrNull { it != SettingsRepository.LIVE_WALLPAPER_DEFAULT_VIDEO }
                    ?: availableVideos.firstOrNull()
            if (first != null) {
                selectedVideo = first
                repository.saveLiveWallpaperSelectedVideo(first)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCachedWallpaper()
        viewModel.fetchTodayWallpaper(context)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.reloadDailyWallpaperAutoUpdateState()
        isAccessibilityGranted = PermissionUtils.isAccessibilityServiceEnabled(context)
        if (isAccessibilityGranted && showDockPermissionSheet) {
            dockEnabled = true
            repository.setLiveWallpaperDockEnabled(true)
        }
        onPauseOrDispose { }
    }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBlurHeightPx = with(density) { (statusBarHeight * 1.15f).toPx() }

    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBlurHeightPx = with(density) { 150.dp.toPx() }

    val pullRefreshState = rememberPullToRefreshState()

    var lastSwipeHapticBucket by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPageOffsetFraction }
            .collect { offset ->
                val fraction = kotlin.math.abs(offset)
                val currentBucket = (fraction * 10).toInt()
                if (currentBucket != lastSwipeHapticBucket) {
                    if (fraction > 0f) {
                        HapticUtil.performSliderHaptic(view)
                    }
                    lastSwipeHapticBucket = currentBucket
                }
            }
    }

    LaunchedEffect(pagerState) {
        var isFirst = true
        snapshotFlow { pagerState.currentPage }
            .collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    HapticUtil.performHeavyHaptic(view)
                }
            }
    }

    var dailyWallpaperNextAutoUpdateTime by remember { mutableLongStateOf(0L) }
    var dailyWallpaperLastCheckedFormatted by remember { mutableStateOf("") }
    LaunchedEffect(dailyWallpaperAutoUpdateTime) {
        while (true) {
            if (!dailyWallpaperAutoUpdateTime.isNullOrEmpty()) {
                val prevTime =
                    runCatching { LocalDateTime.parse(dailyWallpaperAutoUpdateTime) }.getOrNull()
                if (prevTime != null) {
                    val passedMinutes = Duration.between(prevTime, LocalDateTime.now()).toMinutes()
                    val retryCount =
                        repository.getInt(SettingsRepository.KEY_DAILY_WALLPAPER_RETRY_COUNT, 0)
                    if (passedMinutes >= 0) {
                        if (retryCount in 1..2) {
                            val remainingMinutes = (30L - passedMinutes).coerceAtLeast(0L)
                            dailyWallpaperNextAutoUpdateTime = remainingMinutes / 60L
                        } else {
                            val cycleMinutes = passedMinutes % 1440L
                            val remainingMinutes =
                                if (cycleMinutes == 0L && passedMinutes > 0L) 0L else 1440L - cycleMinutes
                            dailyWallpaperNextAutoUpdateTime = remainingMinutes / 60L
                        }
                    } else {
                        dailyWallpaperNextAutoUpdateTime = 24L
                    }

                    dailyWallpaperLastCheckedFormatted =
                        runCatching {
                            prevTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                        }.getOrDefault("")
                }
            }
            kotlinx.coroutines.delay(60_000L)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled && !isDailyLivePreviewExpanded) 40f else 0f,
                    height = topBlurHeightPx,
                    direction = BlurDirection.TOP,
                    showGradientOverlay = !isDailyLivePreviewExpanded,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight + 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                    ).padding(6.dp)
                    .zIndex(3f),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = listOf(0, 1)
            tabs.forEachIndexed { index, page ->
                ToggleButton(
                    checked = pagerState.currentPage == page,
                    onCheckedChange = {
                        HapticUtil.performUIHaptic(view)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                    shapes =
                        when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        },
                    modifier = Modifier.size(width = 80.dp, height = 48.dp),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                id = if (page == 0) R.drawable.rounded_wallpaper_24 else R.drawable.rounded_slow_motion_video_24,
                            ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        val isMotionBlurEnabled by viewModel.isMotionBlurEnabled

        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .progressiveBlur(
                        blurRadius = if (isBlurEnabled && !isDailyLivePreviewExpanded) 40f else 0f,
                        height = bottomBlurHeightPx,
                        direction = BlurDirection.BOTTOM,
                        showGradientOverlay = !isDailyLivePreviewExpanded,
                    )
                    .scrollMotionBlur(pagerState, enabled = isMotionBlurEnabled),
        ) { page ->
            if (page == 0) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (wallpaperInfo != null) {
                        SubcomposeAsyncImage(
                            model = wallpaperInfo!!.urlMobile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LoadingIndicator()
                                }
                            },
                            error = {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_draw_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoading) {
                                LoadingIndicator()
                            }
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = {
                            HapticUtil.performUIHaptic(view)
                            viewModel.fetchTodayWallpaper(context)
                        },
                        state = pullRefreshState,
                        indicator = {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = statusBarHeight + 64.dp),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                val scale =
                                    if (isLoading) {
                                        1f
                                    } else {
                                        pullRefreshState.distanceFraction.coerceIn(
                                            0f,
                                            1f,
                                        )
                                    }
                                if (scale > 0f) {
                                    Card(
                                        shape = CircleShape,
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        modifier =
                                            Modifier
                                                .size(48.dp)
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                    alpha = scale
                                                },
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(36.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            } else {
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        contentPadding =
                            PaddingValues(
                                top = statusBarHeight + 96.dp,
                                bottom = bottomPadding + 180.dp,
                                start = 24.dp,
                                end = 24.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .scrollMotionBlur(gridState, enabled = isMotionBlurEnabled),
                    ) {
                        item {
                            DailyWallpaperLiveItem(
                                imageUrl = wallpaperInfo?.urlMobile,
                                isSelected = selectedVideo == SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE,
                                onClick = {
                                    HapticUtil.performCustomHaptic(view, 0.5f)
                                    if (selectedVideo == SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE) {
                                        isDailyLivePreviewExpanded = true
                                    }
                                    selectedVideo = SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE
                                    repository.saveLiveWallpaperSelectedVideo(SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE)
                                },
                            )
                        }

                        item {
                            AddVideoItem(onClick = {
                                HapticUtil.performCustomHaptic(view, 0.5f)
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly,
                                    ),
                                )
                            })
                        }

                        items(availableVideos) { video ->
                            ThumbnailItem(
                                videoName = video,
                                isSelected = video == selectedVideo,
                                onClick = {
                                    HapticUtil.performCustomHaptic(view, 0.5f)
                                    selectedVideo = video
                                    repository.saveLiveWallpaperSelectedVideo(video)
                                },
                                onRemove =
                                    if (context.resources.getIdentifier(
                                            video,
                                            "raw",
                                            context.packageName,
                                        ) == 0
                                    ) {
                                        {
                                            repository.removeLiveWallpaperCustomVideo(video)
                                            availableVideos =
                                                repository.getLiveWallpaperAvailableVideos()
                                            selectedVideo =
                                                repository.getLiveWallpaperSelectedVideo()
                                        }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible =
                            isDailyLivePreviewExpanded &&
                                selectedVideo == SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE &&
                                wallpaperInfo != null,
                        enter = fadeIn(animationSpec = tween(350)) + scaleIn(animationSpec = tween(350), initialScale = 0.92f),
                        exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300), targetScale = 0.92f),
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val previewBlurHeightPx = constraints.maxHeight * progressiveBlurHeight
                            val previewBlurRadiusPx = with(density) { 28.dp.toPx() }
                            val previewBlurRadius = if (progressiveBlurEnabled) previewBlurRadiusPx else 0f
                            SubcomposeAsyncImage(
                                model = wallpaperInfo?.urlMobile,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            HapticUtil.performUIHaptic(view)
                                            isDailyLivePreviewExpanded = false
                                        }.progressiveBlur(
                                            blurRadius = previewBlurRadius,
                                            height = previewBlurHeightPx,
                                            direction = BlurDirection.TOP,
                                            showGradientOverlay = false,
                                        ).progressiveBlur(
                                            blurRadius = previewBlurRadius,
                                            height = previewBlurHeightPx,
                                            direction = BlurDirection.BOTTOM,
                                            showGradientOverlay = false,
                                        ),
                                loading = {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        LoadingIndicator()
                                    }
                                },
                            )

                            if (dockEnabled && isAccessibilityGranted) {
                                PreviewDock(
                                    imageUrl = wallpaperInfo?.urlMobile,
                                    dockBounds = liveDockBounds,
                                    paddingDp = dockPadding,
                                    opacity = dockOpacity,
                                    topPx = with(density) { (statusBarHeight + 96.dp).toPx() },
                                    containerWidthPx = constraints.maxWidth,
                                    containerHeightPx = constraints.maxHeight,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDockPermissionSheet) {
            PermissionsBottomSheet(
                onDismissRequest = { showDockPermissionSheet = false },
                featureTitle = R.string.live_wallpaper_dock,
                permissions =
                    listOf(
                        PermissionItem(
                            iconRes = R.drawable.rounded_settings_accessibility_24,
                            title = R.string.perm_accessibility_title,
                            description = R.string.perm_accessibility_desc_common,
                            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
                            actionLabel = R.string.perm_action_enable,
                            action = { PermissionUtils.openAccessibilitySettings(context) },
                            isGranted = isAccessibilityGranted,
                        ),
                    ),
            )
        }

        BackHandler(enabled = isDailyLivePreviewExpanded) {
            isDailyLivePreviewExpanded = false
        }

        LaunchedEffect(pagerState.currentPage, selectedVideo) {
            if (pagerState.currentPage != 1 || selectedVideo != SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE) {
                isDailyLivePreviewExpanded = false
            }
        }

        AnimatedVisibility(
            visible = (pagerState.currentPage == 0 && wallpaperInfo != null) || pagerState.currentPage == 1,
            enter =
                fadeIn(animationSpec = tween(400)) +
                    slideInVertically(
                        animationSpec =
                            tween(
                                400,
                            ),
                    ) { it / 2 },
            exit =
                fadeOut(animationSpec = tween(400)) +
                    slideOutVertically(
                        animationSpec =
                            tween(
                                400,
                            ),
                    ) { it / 2 },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 96.dp + bottomPadding)
                    .zIndex(2f),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedVisibility(
                    visible = showSettingsCard,
                    enter =
                        expandVertically(animationSpec = tween(300)) +
                            fadeIn(
                                animationSpec =
                                    tween(
                                        300,
                                    ),
                            ),
                    exit =
                        shrinkVertically(animationSpec = tween(300)) +
                            fadeOut(
                                animationSpec =
                                    tween(
                                        300,
                                    ),
                            ),
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    ) {
                        if (pagerState.currentPage == 0) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        TranslatableText(
                                            stringResId = R.string.label_wallpaper_auto_update,
                                            style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (isAutoUpdateEnabled && !dailyWallpaperAutoUpdateTime.isNullOrEmpty()) {
                                            val timeText =
                                                if (isDailyWallpaperShowLastTime) {
                                                    stringResource(
                                                        R.string.label_wallpaper_last_checked,
                                                        dailyWallpaperLastCheckedFormatted,
                                                    )
                                                } else {
                                                    val showTime =
                                                        if (dailyWallpaperNextAutoUpdateTime > 0L) {
                                                            stringResource(
                                                                R.string.label_wallpaper_hours,
                                                                dailyWallpaperNextAutoUpdateTime.toInt(),
                                                            )
                                                        } else {
                                                            stringResource(R.string.label_wallpaper_a_few_minutes)
                                                        }
                                                    stringResource(
                                                        R.string.label_wallpaper_next_check,
                                                        showTime,
                                                    )
                                                }
                                            Text(
                                                text = timeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier =
                                                    Modifier.clickable(
                                                        enabled = true,
                                                        onClick = {
                                                            HapticUtil.performUIHaptic(view)
                                                            viewModel.setDailyWallpaperShowLastTime()
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isAutoUpdateEnabled,
                                        onCheckedChange = { checked ->
                                            HapticUtil.performUIHaptic(view)
                                            viewModel.setDailyWallpaperAutoUpdate(
                                                checked,
                                                context,
                                            )
                                        },
                                    )
                                }

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            ButtonGroupDefaults.ConnectedSpaceBetween,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ToggleButton(
                                        checked = applyHome,
                                        onCheckedChange = {
                                            if (!applyHome || applyLock) {
                                                HapticUtil.performUIHaptic(view)
                                                applyHome = !applyHome
                                                repository.setDailyWallpaperApplyHome(applyHome)
                                            }
                                        },
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        TranslatableText(R.string.wallpaper_home_screen)
                                    }
                                    ToggleButton(
                                        checked = applyLock,
                                        onCheckedChange = {
                                            if (!applyLock || applyHome) {
                                                HapticUtil.performUIHaptic(view)
                                                applyLock = !applyLock
                                                repository.setDailyWallpaperApplyLock(applyLock)
                                            }
                                        },
                                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        TranslatableText(R.string.wallpaper_lock_screen)
                                    }
                                }

                            }
                        } else {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (selectedVideo == SettingsRepository.LIVE_WALLPAPER_DAILY_IMAGE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        TranslatableText(
                                            stringResId = R.string.live_wallpaper_progressive_blur,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Switch(
                                            checked = progressiveBlurEnabled,
                                            onCheckedChange = { checked ->
                                                HapticUtil.performUIHaptic(view)
                                                progressiveBlurEnabled = checked
                                                repository.setLiveWallpaperProgressiveBlurEnabled(checked)
                                            },
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = progressiveBlurEnabled,
                                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TranslatableText(
                                                stringResId = R.string.live_wallpaper_blur_height,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Slider(
                                                value = progressiveBlurHeight,
                                                onValueChange = {
                                                    progressiveBlurHeight = it
                                                    HapticUtil.performSliderHaptic(view)
                                                },
                                                onValueChangeFinished = {
                                                    repository.setLiveWallpaperProgressiveBlurHeight(progressiveBlurHeight)
                                                },
                                                valueRange = 0.05f..0.4f,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        TranslatableText(
                                            stringResId = R.string.live_wallpaper_zoom_effect,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Switch(
                                            checked = zoomEffectEnabled,
                                            onCheckedChange = { checked ->
                                                HapticUtil.performUIHaptic(view)
                                                zoomEffectEnabled = checked
                                                repository.setLiveWallpaperZoomEffectEnabled(checked)
                                            },
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        TranslatableText(
                                            stringResId = R.string.live_wallpaper_dock,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Switch(
                                            checked = dockEnabled && isAccessibilityGranted,
                                            onCheckedChange = { checked ->
                                                HapticUtil.performUIHaptic(view)
                                                if (checked && !isAccessibilityGranted) {
                                                    showDockPermissionSheet = true
                                                } else {
                                                    dockEnabled = checked
                                                    repository.setLiveWallpaperDockEnabled(checked)
                                                }
                                            },
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = dockEnabled && isAccessibilityGranted,
                                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TranslatableText(
                                                stringResId = R.string.live_wallpaper_dock_padding,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Slider(
                                                value = dockPadding,
                                                onValueChange = {
                                                    dockPadding = it
                                                    HapticUtil.performSliderHaptic(view)
                                                },
                                                onValueChangeFinished = {
                                                    repository.setLiveWallpaperDockPadding(dockPadding)
                                                },
                                                valueRange = 0f..32f,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            TranslatableText(
                                                stringResId = R.string.live_wallpaper_dock_opacity,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Slider(
                                                value = dockOpacity,
                                                onValueChange = {
                                                    dockOpacity = it
                                                    HapticUtil.performSliderHaptic(view)
                                                },
                                                onValueChangeFinished = {
                                                    repository.setLiveWallpaperDockOpacity(dockOpacity)
                                                },
                                                valueRange = 0f..1f,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                } else {
                                    TranslatableText(
                                        stringResId = R.string.live_wallpaper_playback_trigger_title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )

                                    val options =
                                        listOf(
                                            SettingsRepository.LIVE_WALLPAPER_TRIGGER_UNLOCK to
                                                stringResource(
                                                    R.string.live_wallpaper_trigger_unlock,
                                                ),
                                            SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON to
                                                stringResource(
                                                    R.string.live_wallpaper_trigger_screen_on,
                                                ),
                                        )

                                    SegmentedPicker(
                                        items = options,
                                        selectedItem =
                                            options.find { it.first == playbackTrigger }
                                                ?: options.first(),
                                        onItemSelected = { option ->
                                            HapticUtil.performUIHaptic(view)
                                            playbackTrigger = option.first
                                            repository.saveLiveWallpaperPlaybackTrigger(option.first)
                                        },
                                        labelProvider = { it.second },
                                        containerColor = Color.Transparent,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                if (repository.getLiveWallpaperCustomVideos().isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            HapticUtil.performHeavyHaptic(view)
                                            repository.saveLiveWallpaperCustomVideos(emptyList())
                                            availableVideos =
                                                repository.getLiveWallpaperAvailableVideos()
                                            selectedVideo =
                                                repository.getLiveWallpaperSelectedVideo()
                                        },
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            ),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_delete_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TranslatableText(stringResId = R.string.label_clear_custom_videos)
                                    }
                                }
                            }
                        }
                    }
                }

                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = {
                                if (pagerState.currentPage == 0) {
                                    HapticUtil.performHeavyHaptic(view)
                                    wallpaperInfo?.urlMobile?.let { url ->
                                        viewModel.applyWallpaper(context, url) { success ->
                                            if (!success) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        R.string.label_wallpaper_apply_failed,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            }
                                        }
                                    }
                                } else {
                                    HapticUtil.performCustomHaptic(view, 0.8f)
                                    com.sameerasw.essentials.utils.LiveWallpaperUtil.openLiveWallpaperPicker(context)
                                }
                            },
                            modifier = Modifier.height(56.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TranslatableText(
                                stringResId = R.string.label_wallpaper_apply,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TrailingButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showSettingsCard = !showSettingsCard
                            },
                            modifier = Modifier.height(56.dp),
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        id = if (showSettingsCard) R.drawable.rounded_keyboard_arrow_down_24 else R.drawable.rounded_keyboard_arrow_up_24,
                                    ),
                                contentDescription = stringResource(R.string.label_options),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                )
            }
        }

        ProvideBottomBar(
            route = route,
            spec =
                BottomBarSpec(
                    title = stringResource(R.string.feat_daily_wallpaper_title),
                    onBack = onBack,
                    onHelpClick = { showHelpSheet = true },
                ),
        )

        if (showHelpSheet) {
            WallpaperHelpBottomSheet(
                selectedTab = pagerState.currentPage,
                onDismissRequest = { showHelpSheet = false },
                wallpaperInfo = wallpaperInfo,
                onViewImage = {
                    try {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(wallpaperInfo?.photoLink ?: ""),
                            )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onViewAuthor = {
                    try {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(wallpaperInfo?.authorLink ?: ""),
                            )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onViewCollection = {
                    try {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://unsplash.com/collections/LqO9knU9z2A/Likes"),
                            )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperHelpBottomSheet(
    selectedTab: Int,
    onDismissRequest: () -> Unit,
    wallpaperInfo: com.sameerasw.essentials.domain.model.WallpaperInfo?,
    onViewImage: () -> Unit,
    onViewAuthor: () -> Unit,
    onViewCollection: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedTab == 0) {
                TranslatableText(
                    stringResId = R.string.feat_daily_wallpaper_title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )

                TranslatableText(
                    stringResId = R.string.feat_daily_wallpaper_desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (wallpaperInfo != null) {
                    RoundedCardContainer {
                        Column(
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceBright,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    ).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TranslatableText(
                                stringResId = R.string.wallpaper_current_photo_credits,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = onViewImage,
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                ) {
                                    TranslatableText(stringResId = R.string.wallpaper_view_photo)
                                }

                                Button(
                                    onClick = onViewAuthor,
                                    modifier = Modifier.weight(1f),
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                ) {
                                    Text(
                                        text = wallpaperInfo.authorName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                RoundedCardContainer {
                    Column(
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceBright,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onViewCollection,
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        ) {
                            TranslatableText(stringResId = R.string.wallpaper_view_collection)
                        }

                        Button(
                            onClick = {
                                onDismissRequest()
                                val intent = Intent(context, WallpaperStagingActivity::class.java)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_wallpaper_24),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TranslatableText(stringResId = R.string.feat_wallpaper_staging_title)
                        }
                    }
                }
            } else {
                TranslatableText(
                    stringResId = R.string.feat_live_wallpaper_title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )

                TranslatableText(
                    stringResId = R.string.feat_live_wallpaper_desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RoundedCardContainer {
                    Column(
                        modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceBright,
                                    shape = MaterialTheme.shapes.extraSmall,
                                ).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TranslatableText(
                            stringResId = R.string.label_how_to_use,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "1. Choose one of our high quality default loops or add a custom video from your gallery.\n2. Adjust when the video should play (on Unlock or when Screen On) using the options arrow.\n3. Click Apply to set it via the system picker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ThumbnailItem(
    videoName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(videoName) {
        thumbnail =
            withContext(Dispatchers.IO) {
                val resId = context.resources.getIdentifier(videoName, "raw", context.packageName)
                if (resId != 0) {
                    MediaMetadataRetriever().use { retriever ->
                        try {
                            val uri = Uri.parse("android.resource://${context.packageName}/$resId")
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(0)
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    try {
                        val uri = Uri.parse(videoName)
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(context, uri)
                            retriever.getFrameAtTime(0)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onRemove?.invoke()
                        }
                        null
                    }
                }
            }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    if (onRemove != null) {
                        HapticUtil.performHeavyHaptic(view)
                        showMenu = true
                    }
                },
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            )

            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_circle_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                SegmentedDropdownMenuItem(
                    text = { TranslatableText(R.string.action_remove) },
                    onClick = {
                        showMenu = false
                        onRemove?.invoke()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_24),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun AddVideoItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier =
                Modifier
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyWallpaperLiveItem(
    imageUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier =
                Modifier
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_wallpaper_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { LoadingIndicator(modifier = Modifier.size(32.dp)) },
                )
            }

            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_circle_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_wallpaper_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .padding(4.dp)
                            .size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewDock(
    imageUrl: String?,
    dockBounds: AndroidRect?,
    paddingDp: Float,
    opacity: Float,
    topPx: Float,
    containerWidthPx: Int,
    containerHeightPx: Int,
) {
    val density = LocalDensity.current
    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    val paddingPx = with(density) { paddingDp.dp.toPx() }
    // real dock width and icon size when known, but placed near the top so the floating controls never cover it
    val iconsRect =
        dockBounds?.let {
            val top = topPx + paddingPx
            Rect(
                it.left - originInWindow.x,
                top,
                it.right - originInWindow.x,
                top + it.height(),
            )
        } ?: with(density) {
            val iconSize = 56.dp.toPx()
            val width = iconSize * 4 + 28.dp.toPx() * 3
            val top = topPx + paddingPx
            Rect(
                (containerWidthPx - width) / 2f,
                top,
                (containerWidthPx + width) / 2f,
                top + iconSize,
            )
        }
    val iconCount = (iconsRect.width / (iconsRect.height * 1.25f)).roundToInt().coerceIn(1, 7)
    val container =
        Rect(
            iconsRect.left - paddingPx,
            iconsRect.top - paddingPx,
            iconsRect.right + paddingPx,
            iconsRect.bottom + paddingPx,
        )
    val shape = RoundedCornerShape(with(density) { minOf(container.height / 2f, 32.dp.toPx()).toDp() })
    val isNight = isSystemInDarkTheme()
    val tintColor =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            colorResource(if (isNight) AndroidR.color.system_accent1_900 else AndroidR.color.system_accent1_50)
        } else if (isNight) {
            Color.Black
        } else {
            Color.White
        }
    val iconColors =
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.surfaceBright,
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { originInWindow = it.positionInWindow() },
    ) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(container.left.roundToInt(), container.top.roundToInt()) }
                    .size(with(density) { container.width.toDp() }, with(density) { container.height.toDp() })
                    .clip(shape),
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(Constraints.fixed(containerWidthPx, containerHeightPx))
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(-container.left.roundToInt(), -container.top.roundToInt())
                            }
                        }.blur(28.dp),
            )
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(tintColor.copy(alpha = opacity))
                        .border(1.dp, Color.White.copy(alpha = opacity * if (isNight) 0.25f else 0.7f), shape),
            )
            Row(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(with(density) { paddingPx.toDp() }),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(iconCount) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(with(density) { (iconsRect.height * 0.8f).toDp() })
                                .background(iconColors[index % iconColors.size], CircleShape),
                    )
                }
            }
        }
    }
}
