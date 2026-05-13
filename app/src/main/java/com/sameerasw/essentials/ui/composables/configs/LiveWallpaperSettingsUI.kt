package com.sameerasw.essentials.ui.composables.configs

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.LiveWallpaperService
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWallpaperSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SettingsRepository(context) }
    var availableVideos by remember { mutableStateOf(repository.getLiveWallpaperAvailableVideos()) }
    var selectedVideo by remember { mutableStateOf(repository.getLiveWallpaperSelectedVideo()) }
    var playbackTrigger by remember { mutableStateOf(repository.getLiveWallpaperPlaybackTrigger()) }

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {
                HapticUtil.performCustomHaptic(view, 0.8f)
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(context, LiveWallpaperService::class.java)
                    )
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_open_in_new_24),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.btn_apply),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(
            text = stringResource(R.string.live_wallpaper_playback_trigger_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            val options = listOf(
                SettingsRepository.LIVE_WALLPAPER_TRIGGER_UNLOCK to stringResource(R.string.live_wallpaper_trigger_unlock),
                SettingsRepository.LIVE_WALLPAPER_TRIGGER_SCREEN_ON to stringResource(R.string.live_wallpaper_trigger_screen_on)
            )

            SegmentedPicker(
                items = options,
                selectedItem = options.find { it.first == playbackTrigger } ?: options.first(),
                onItemSelected = { option ->
                    playbackTrigger = option.first
                    repository.saveLiveWallpaperPlaybackTrigger(option.first)
                },
                labelProvider = { it.second },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = stringResource(R.string.live_wallpaper_video_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(max = 800.dp)
                .fillMaxWidth()
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
                    onRemove = if (video.startsWith("content://")) {
                        {
                            repository.removeLiveWallpaperCustomVideo(video)
                            availableVideos = repository.getLiveWallpaperAvailableVideos()
                            selectedVideo = repository.getLiveWallpaperSelectedVideo()
                        }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
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
            } ?: Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black))

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
