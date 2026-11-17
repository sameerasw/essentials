/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Wallpaper Staging
 * File: WallpaperStagingScreen.kt
 * Description: UI Screen for previewing, skipping, and staging tomorrow's mobile wallpaper.
 */

package com.sameerasw.essentials.ui.features.wallpaper

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.sameerasw.essentials.data.repository.WallpaperStagingRepository
import com.sameerasw.essentials.domain.model.WallpaperInfo
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WallpaperStagingScreen(
    onBack: () -> Unit,
    settingsRepository: SettingsRepository,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val stagingRepository = remember { WallpaperStagingRepository(settingsRepository) }

    val isOwner = remember { settingsRepository.getGitHubUser()?.login?.equals(OWNER_LOGIN, ignoreCase = true) == true }
    var accessKey by remember { mutableStateOf(settingsRepository.getUnsplashAccessKey() ?: "") }
    var showApiKeyDialog by remember { mutableStateOf(isOwner && accessKey.isBlank()) }
    var tempApiKey by remember { mutableStateOf(accessKey) }

    var isLoading by remember { mutableStateOf(true) }
    var isCommitting by remember { mutableStateOf(false) }
    var candidatePhotos by remember { mutableStateOf<List<WallpaperInfo>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var stagedInfo by remember { mutableStateOf<WallpaperInfo?>(null) }
    var historyIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val currentPhoto: WallpaperInfo? =
        candidatePhotos.getOrNull(currentIndex) ?: stagedInfo

    val isCurrentStaged =
        currentPhoto != null && stagedInfo != null && currentPhoto.id == stagedInfo?.id

    val isBlurEnabled =
        remember {
            settingsRepository.getBoolean(SettingsRepository.KEY_USE_BLUR, true)
        }

    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBlurHeightPx = with(density) { (statusBarHeight * 1.2f).toPx() }
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBlurHeightPx = with(density) { 150.dp.toPx() }

    var shuffledPages by remember { mutableStateOf<List<Int>>(emptyList()) }
    var nextPageIndex by remember { mutableIntStateOf(0) }
    var isFetchingMore by remember { mutableStateOf(false) }
    var pendingAdvance by remember { mutableStateOf(false) }
    var randomAvailable by remember { mutableStateOf(true) }
    var recentlySeen by remember { mutableStateOf(settingsRepository.getWallpaperStagingSeen().toSet()) }

    suspend fun gather(target: Int, exclude: Set<String>): List<WallpaperInfo> {
        val found = LinkedHashMap<String, WallpaperInfo>()
        fun accept(photos: List<WallpaperInfo>) =
            photos.forEach { if (it.id !in historyIds && it.id !in exclude && it.id !in recentlySeen) found.putIfAbsent(it.id, it) }

        var attempts = 0
        while (randomAvailable && found.size < target && attempts < RANDOM_ATTEMPTS) {
            attempts++
            val batch = stagingRepository.fetchRandomCollectionPhotos(accessKey)
            if (batch.isEmpty()) randomAvailable = false else accept(batch)
        }
        if (found.size < target && shuffledPages.isEmpty()) {
            val totalPages = stagingRepository.fetchCollectionTotalPages(accessKey)
            shuffledPages = (1..(if (totalPages > 0) totalPages else FALLBACK_PAGES)).shuffled()
            nextPageIndex = 0
        }
        while (found.size < target && nextPageIndex < shuffledPages.size) {
            val page = shuffledPages[nextPageIndex]
            nextPageIndex++
            accept(stagingRepository.fetchCollectionPhotos(accessKey, page))
        }
        return found.values.shuffled()
    }

    fun markSeen(photo: WallpaperInfo?) {
        val id = photo?.id ?: return
        if (id in recentlySeen) return
        recentlySeen = recentlySeen + id
        settingsRepository.markWallpaperStagingSeen(id)
    }

    fun fetchNextBatch() {
        if (accessKey.isBlank() || isFetchingMore) return

        isFetchingMore = true
        coroutineScope.launch {
            var fresh = gather(10, candidatePhotos.map { it.id }.toSet())
            if (fresh.isEmpty() && recentlySeen.isNotEmpty()) {
                settingsRepository.clearWallpaperStagingSeen()
                recentlySeen = emptySet()
                randomAvailable = true
                shuffledPages = emptyList()
                fresh = gather(10, candidatePhotos.map { it.id }.toSet())
            }
            if (fresh.isNotEmpty()) candidatePhotos = candidatePhotos + fresh
            if (pendingAdvance) {
                pendingAdvance = false
                currentIndex = if (currentIndex + 1 < candidatePhotos.size) currentIndex + 1 else 0
            }
            isFetchingMore = false
        }
    }

    fun showNext() {
        if (candidatePhotos.isEmpty()) return
        markSeen(candidatePhotos.getOrNull(currentIndex))
        if (currentIndex + 1 < candidatePhotos.size) {
            currentIndex++
            if (candidatePhotos.size - currentIndex <= 5) fetchNextBatch()
        } else {
            pendingAdvance = true
            fetchNextBatch()
        }
    }

    fun loadData() {
        if (!isOwner) {
            isLoading = true
            coroutineScope.launch {
                stagedInfo = stagingRepository.fetchStagedWallpaper()
                candidatePhotos = emptyList()
                isLoading = false
            }
            return
        }
        if (accessKey.isBlank()) {
            isLoading = false
            return
        }
        isLoading = true
        coroutineScope.launch {
            stagedInfo = stagingRepository.fetchStagedWallpaper()
            historyIds = stagingRepository.fetchMobileHistory()
            randomAvailable = true
            shuffledPages = emptyList()
            nextPageIndex = 0
            pendingAdvance = false

            var initial = gather(15, emptySet())
            if (initial.isEmpty() && recentlySeen.isNotEmpty()) {
                settingsRepository.clearWallpaperStagingSeen()
                recentlySeen = emptySet()
                randomAvailable = true
                shuffledPages = emptyList()
                initial = gather(15, emptySet())
            }

            val randomized = initial.toMutableList()
            // If there's already a staged wallpaper, ensure it is the initial photo shown
            stagedInfo?.let { staged ->
                randomized.removeAll { it.id == staged.id }
                randomized.add(0, staged)
            }

            candidatePhotos = randomized
            currentIndex = 0
            isLoading = false
        }
    }

    LaunchedEffect(accessKey) {
        if (!isOwner || accessKey.isNotBlank()) {
            loadData()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .progressiveBlur(
                    blurRadius = if (isBlurEnabled) 40f else 0f,
                    height = topBlurHeightPx,
                    direction = BlurDirection.TOP,
                ),
    ) {
        // Main Image Preview
        if (isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LoadingIndicator(modifier = Modifier.size(44.dp))
                }
            }
        } else if (currentPhoto != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubcomposeAsyncImage(
                    model = currentPhoto.urlMobile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .progressiveBlur(
                                blurRadius = if (isBlurEnabled) 40f else 0f,
                                height = bottomBlurHeightPx,
                                direction = BlurDirection.BOTTOM,
                            ),
                    loading = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator(modifier = Modifier.size(36.dp))
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
                                painter = painterResource(id = R.drawable.rounded_wallpaper_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                )

                // Top Credits & Status Chip
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusBarHeight + 16.dp, start = 20.dp, end = 20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                                shape = CircleShape,
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .zIndex(3f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isCurrentStaged) {
                        Box(
                            modifier =
                                Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            TranslatableText(
                                stringResId = R.string.wallpaper_staging_staged_badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    TranslatableText(
                        stringResId = R.string.wallpaper_photo_by,
                        formatArgs = listOf(currentPhoto.authorName),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    val url = currentPhoto.photoLink.ifBlank { currentPhoto.authorLink }
                                    if (url.isNotBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                    )
                }
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_wallpaper_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                    TranslatableText(
                        stringResId = if (isOwner) R.string.wallpaper_staging_error else R.string.wallpaper_staging_not_set,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isOwner) {
                        Button(
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showApiKeyDialog = true
                            },
                        ) {
                            TranslatableText(R.string.wallpaper_staging_set_api_key)
                        }
                    }
                }
            }
        }

        
        AnimatedVisibility(
            visible = isOwner && currentPhoto != null && !isLoading,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 96.dp + bottomPadding)
                    .zIndex(2f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                
                if (stagedInfo != null) {
                    IconButton(
                        onClick = {
                            HapticUtil.performHeavyHaptic(view)
                            isCommitting = true
                            coroutineScope.launch {
                                val success = stagingRepository.stageTomorrowWallpaper(null)
                                isCommitting = false
                                if (success) {
                                    stagedInfo = null
                                    Toast.makeText(
                                        context,
                                        R.string.wallpaper_staging_cleared_success,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_24),
                            contentDescription = stringResource(R.string.wallpaper_staging_clear),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                
                Button(
                    onClick = {
                        if (currentPhoto == null || isCommitting || isCurrentStaged) return@Button
                        HapticUtil.performHeavyHaptic(view)
                        isCommitting = true
                        coroutineScope.launch {
                            val success = stagingRepository.stageTomorrowWallpaper(currentPhoto)
                            isCommitting = false
                            if (success) {
                                stagedInfo = currentPhoto
                                Toast.makeText(
                                    context,
                                    R.string.wallpaper_staging_staged_success,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.wallpaper_commit_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                    enabled = !isCommitting && !isCurrentStaged,
                    modifier =
                        Modifier
                            .weight(1.2f)
                            .height(52.dp),
                    shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                    colors =
                        if (isCurrentStaged) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        },
                ) {
                    if (isCommitting) {
                        LoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            painter =
                                painterResource(
                                    id = if (isCurrentStaged) R.drawable.rounded_check_24 else R.drawable.rounded_bookmark_24,
                                ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text =
                                if (isCurrentStaged) {
                                    stringResource(R.string.wallpaper_staging_picked)
                                } else {
                                    stringResource(R.string.wallpaper_staging_stage)
                                },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

                
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        showNext()
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                    shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                ) {
                    TranslatableText(
                        stringResId = R.string.wallpaper_staging_next,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_forward_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        
        EssentialsFloatingToolbar(
            title = stringResource(R.string.feat_wallpaper_staging_title),
            onBackClick = {
                HapticUtil.performUIHaptic(view)
                onBack()
            },
            fabAction = if (isOwner) {
                {
                    HapticUtil.performUIHaptic(view)
                    tempApiKey = accessKey
                    showApiKeyDialog = true
                }
            } else {
                null
            },
            fabIconRes = R.drawable.rounded_vpn_key_24.takeIf { isOwner },
            fabContentDescription = stringResource(R.string.wallpaper_staging_set_api_key),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f),
        )

        // API Key Modal Dialog
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = { TranslatableText(R.string.wallpaper_staging_set_api_key) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TranslatableText(
                            stringResId = R.string.wallpaper_staging_api_key_desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = tempApiKey,
                            onValueChange = { tempApiKey = it },
                            placeholder = { TranslatableText(R.string.wallpaper_staging_api_key_hint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            accessKey = tempApiKey.trim()
                            settingsRepository.setUnsplashAccessKey(accessKey)
                            showApiKeyDialog = false
                            loadData()
                        },
                    ) {
                        TranslatableText(R.string.action_save)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        TranslatableText(R.string.action_cancel)
                    }
                },
            )
        }
    }
}

private const val OWNER_LOGIN = "sameerasw"
private const val RANDOM_ATTEMPTS = 3
private const val FALLBACK_PAGES = 10
