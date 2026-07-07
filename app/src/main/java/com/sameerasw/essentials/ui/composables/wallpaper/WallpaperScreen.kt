package com.sameerasw.essentials.ui.composables.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import coil.compose.SubcomposeAsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.LiveWallpaperService
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.utils.HapticUtil
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
    onBack: () -> Unit,
    viewModel: MainViewModel,
    initialTab: String = "daily"
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
    var applyHome by remember { mutableStateOf(repository.getDailyWallpaperApplyHome()) }
    var applyLock by remember { mutableStateOf(repository.getDailyWallpaperApplyLock()) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var showSettingsCard by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = if (initialTab == "live") 1 else 0,
        pageCount = { 2 }
    )

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                repository.addLiveWallpaperCustomVideo(it.toString())
                availableVideos = repository.getLiveWallpaperAvailableVideos()
                selectedVideo = it.toString()
                repository.saveLiveWallpaperSelectedVideo(it.toString())
            }
        }
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
        viewModel.fetchTodayWallpaper(context)
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
    LaunchedEffect(dailyWallpaperAutoUpdateTime) {
        if (!dailyWallpaperAutoUpdateTime.isNullOrEmpty()){
            val prevTime = LocalDateTime.parse(dailyWallpaperAutoUpdateTime)
            val passedTime = Duration.between(
                prevTime,
                LocalDateTime.now()
            ).toHours()
            val hoursLeft = (12L - passedTime).coerceAtLeast(0)
            dailyWallpaperNextAutoUpdateTime = hoursLeft
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .progressiveBlur(
                blurRadius = if (isBlurEnabled) 40f else 0f,
                height = topBlurHeightPx,
                direction = BlurDirection.TOP
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarHeight + 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape
                )
                .padding(6.dp)
                .zIndex(3f),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically
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
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    modifier = Modifier.size(width = 80.dp, height = 48.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (page == 0) R.drawable.rounded_wallpaper_24 else R.drawable.rounded_slow_motion_video_24
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled) 40f else 0f,
                    height = bottomBlurHeightPx,
                    direction = BlurDirection.BOTTOM
                )
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator()
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_draw_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = statusBarHeight + 64.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val scale =
                                    if (isLoading) 1f else pullRefreshState.distanceFraction.coerceIn(
                                        0f,
                                        1f
                                    )
                                if (scale > 0f) {
                                    Card(
                                        shape = CircleShape,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                alpha = scale
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            top = statusBarHeight + 96.dp,
                            bottom = bottomPadding + 180.dp,
                            start = 24.dp,
                            end = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            AddVideoItem(onClick = {
                                HapticUtil.performCustomHaptic(view, 0.5f)
                                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
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
                                onRemove = if (context.resources.getIdentifier(
                                        video,
                                        "raw",
                                        context.packageName
                                    ) == 0
                                ) {
                                    {
                                        repository.removeLiveWallpaperCustomVideo(video)
                                        availableVideos =
                                            repository.getLiveWallpaperAvailableVideos()
                                        selectedVideo = repository.getLiveWallpaperSelectedVideo()
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = (pagerState.currentPage == 0 && wallpaperInfo != null) || pagerState.currentPage == 1,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 2 },
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                animationSpec = tween(
                    400
                )
            ) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp + bottomPadding)
                .zIndex(2f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = showSettingsCard,
                    enter = expandVertically(animationSpec = tween(300)) + fadeIn(
                        animationSpec = tween(
                            300
                        )
                    ),
                    exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(
                        animationSpec = tween(
                            300
                        )
                    )
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        if (pagerState.currentPage == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.label_wallpaper_auto_update),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isAutoUpdateEnabled && !dailyWallpaperAutoUpdateTime.isNullOrEmpty()){
                                                val timeText = if(isDailyWallpaperShowLastTime) {
                                                    val showTime = LocalDateTime.parse(dailyWallpaperAutoUpdateTime)
                                                        .format(DateTimeFormatter.ofPattern("hh:mm a"))
                                                    "Last checked at $showTime"
                                                } else{
                                                    val showTime = if(dailyWallpaperNextAutoUpdateTime > 0L) "$dailyWallpaperNextAutoUpdateTime hours" else "a few minutes"
                                                    "Next check in $showTime"
                                                }
                                                Text(
                                                    text = timeText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.clickable(enabled = true, onClick = {
                                                        HapticUtil.performUIHaptic(view)
                                                        viewModel.setDailyWallpaperShowLastTime()
                                                    })
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isAutoUpdateEnabled,
                                            onCheckedChange = { checked ->
                                                HapticUtil.performUIHaptic(view)
                                                viewModel.setDailyWallpaperAutoUpdate(checked, context)
                                            }
                                        )
                                    }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    verticalAlignment = Alignment.CenterVertically
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
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Home Screen")
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
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Lock Screen")
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.live_wallpaper_playback_trigger_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val options = listOf(
                                    SettingsRepository.LIVE_WALLPAPER_TRIGGER_UNLOCK to stringResource(
                                        R.string.live_wallpaper_trigger_unlock
                                    ),
                                    SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON to stringResource(
                                        R.string.live_wallpaper_trigger_screen_on
                                    )
                                )

                                SegmentedPicker(
                                    items = options,
                                    selectedItem = options.find { it.first == playbackTrigger }
                                        ?: options.first(),
                                    onItemSelected = { option ->
                                        HapticUtil.performUIHaptic(view)
                                        playbackTrigger = option.first
                                        repository.saveLiveWallpaperPlaybackTrigger(option.first)
                                    },
                                    labelProvider = { it.second },
                                    containerColor = Color.Transparent,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_delete_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = stringResource(R.string.label_clear_custom_videos))
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
                                                Toast.makeText(
                                                    context,
                                                    R.string.label_wallpaper_apply_failed,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                } else {
                                    HapticUtil.performCustomHaptic(view, 0.8f)
                                    val intent =
                                        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                            putExtra(
                                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                                ComponentName(
                                                    context,
                                                    LiveWallpaperService::class.java
                                                )
                                            )
                                        }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.label_wallpaper_apply),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TrailingButton(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showSettingsCard = !showSettingsCard
                            },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (showSettingsCard) R.drawable.rounded_keyboard_arrow_down_24 else R.drawable.rounded_keyboard_arrow_up_24),
                                contentDescription = "Options",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        }

        EssentialsFloatingToolbar(
            title = stringResource(R.string.feat_daily_wallpaper_title),
            onBackClick = {
                HapticUtil.performUIHaptic(view)
                onBack()
            },
            onHelpClick = {
                HapticUtil.performUIHaptic(view)
                showHelpSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        )

        if (showHelpSheet) {
            WallpaperHelpBottomSheet(
                selectedTab = pagerState.currentPage,
                onDismissRequest = { showHelpSheet = false },
                wallpaperInfo = wallpaperInfo,
                onViewImage = {
                    try {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(wallpaperInfo?.photoLink ?: ""))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onViewAuthor = {
                    try {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(wallpaperInfo?.authorLink ?: ""))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onViewCollection = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://unsplash.com/collections/LqO9knU9z2A/Likes")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
    onViewCollection: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                Text(
                    text = stringResource(R.string.feat_daily_wallpaper_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = stringResource(R.string.feat_daily_wallpaper_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (wallpaperInfo != null) {
                    RoundedCardContainer {
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceBright,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Current Photo Credits",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onViewImage,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(text = "View Photo")
                                }

                                Button(
                                    onClick = onViewAuthor,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = wallpaperInfo.authorName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onViewCollection,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(text = "View Sameera's Collection")
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.feat_live_wallpaper_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = stringResource(R.string.feat_live_wallpaper_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceBright,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "How to use",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "1. Choose one of our high quality default loops or add a custom video from your gallery.\n2. Adjust when the video should play (on Unlock or when Screen On) using the options arrow.\n3. Click Apply to set it via the system picker.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onRemove: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(videoName) {
        thumbnail = withContext(Dispatchers.IO) {
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
        modifier = Modifier.combinedClickable(
            onClick = { onClick() },
            onLongClick = {
                if (onRemove != null) {
                    HapticUtil.performHeavyHaptic(view)
                    showMenu = true
                }
            }
        )
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            thumbnail?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_circle_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_remove)) },
                    onClick = {
                        showMenu = false
                        onRemove?.invoke()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_24),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AddVideoItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_add_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
