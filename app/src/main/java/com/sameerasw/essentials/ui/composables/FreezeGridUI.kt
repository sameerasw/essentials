package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShortcutUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreezeGridUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val view = LocalView.current
    val pickedApps by viewModel.freezePickedApps
    val isPickedAppsLoading by viewModel.isFreezePickedAppsLoading

    val gridState = rememberLazyGridState()
    val frozenStates = remember { mutableStateMapOf<String, Boolean>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh frozen states when active
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.check(context)
                viewModel.refreshFreezePickedApps(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pickedApps) {
        withContext(Dispatchers.IO) {
            val states = pickedApps.associate { app ->
                app.packageName to FreezeManager.isAppFrozen(context, app.packageName)
            }
            // Batch update on Main thread
            withContext(Dispatchers.Main) {
                frozenStates.putAll(states)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isPickedAppsLoading && pickedApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (pickedApps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No apps selected to freeze.\nGo to settings to pick apps.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            RoundedCardContainer(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 150.dp,
                        top = 0.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(pickedApps, key = { it.packageName }) { app ->
                        AppGridItem(
                            app = app,
                            isFrozen = frozenStates[app.packageName] ?: false,
                            isAutoFreezeEnabled = app.isEnabled,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.launchAndUnfreezeApp(
                                    context,
                                    app.packageName
                                )
                                // We don't finish() here since this is a tab
                            },
                            onLongClick = {
                                ShortcutUtil.pinAppShortcut(context, app)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: NotificationApp,
    isFrozen: Boolean,
    isAutoFreezeEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val view = LocalView.current
    val grayscaleMatrix = remember { ColorMatrix().apply { setToSaturation(0.4f) } }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
                onLongClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onLongClick()
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
            ) {
                // App Icon
                Image(
                    bitmap = app.icon,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isFrozen) ColorFilter.colorMatrix(grayscaleMatrix) else null,
                    alpha = if (isFrozen) 0.6f else 1f
                )

                // Status Badges (Top Right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    horizontalArrangement = Arrangement.spacedBy((-4).dp)
                ) {
                    // Auto-freeze Exclusion Badge (Lock)
                    if (!isAutoFreezeEnabled) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_lock_clock_24),
                                contentDescription = "Auto-freeze excluded",
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isFrozen) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
