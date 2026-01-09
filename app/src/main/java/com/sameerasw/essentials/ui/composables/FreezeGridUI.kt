package com.sameerasw.essentials.ui.composables

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon

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
            pickedApps.forEach { app ->
                frozenStates[app.packageName] = FreezeManager.isAppFrozen(context, app.packageName)
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = contentPadding.calculateBottomPadding() + 88.dp,
                        top = 0.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp, end = 16.dp)
        ) {
            ExpandableFreezeFab(
                onUnfreezeAll = { viewModel.unfreezeAllApps(context) },
                onFreezeAll = { viewModel.freezeAllApps(context) },
                onFreezeAutomatic = { viewModel.freezeAutomaticApps(context) }
            )
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
                    bitmap = app.icon.toBitmap().asImageBitmap(),
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
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isFrozen) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableFreezeFab(
    onUnfreezeAll: () -> Unit,
    onFreezeAll: () -> Unit,
    onFreezeAutomatic: () -> Unit
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    FloatingActionButtonMenu(
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier
                    .semantics {
                        stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                        contentDescription = "Toggle menu"
                    },
                checked = fabMenuExpanded,
                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
            ) {
                // Animate the icon based on the state
                val progress by animateFloatAsState(
                    targetValue = if (fabMenuExpanded) 1f else 0f,
                    label = "fab_icon_animation"
                )
                
                Icon(
                    painter = painterResource(
                        id = if (fabMenuExpanded) R.drawable.rounded_close_24 else R.drawable.rounded_mode_cool_24
                    ),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ progress }),
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onFreezeAll()
            },
            icon = { Icon(painterResource(id = R.drawable.rounded_mode_cool_24), contentDescription = null) },
            text = { Text(text = "Freeze All") },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onUnfreezeAll()
            },
            icon = { Icon(painterResource(id = R.drawable.rounded_mode_cool_off_24), contentDescription = null) },
            text = { Text(text = "Unfreeze All") },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                fabMenuExpanded = false
                onFreezeAutomatic()
            },
            icon = { Icon(painterResource(id = R.drawable.rounded_nest_farsight_cool_24), contentDescription = null) },
            text = { Text(text = "Freeze Automatic") },
        )
    }
}
