package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.state.LocalMenuStateManager
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShortcutUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreezeGridUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onGetStartedClick: (() -> Unit)? = null,
    onAppLaunched: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val menuState = LocalMenuStateManager.current
    val scope = rememberCoroutineScope()
    val pickedApps by viewModel.freezePickedApps
    val isPickedAppsLoading by viewModel.isFreezePickedAppsLoading

    val frozenStates = remember { mutableStateMapOf<String, Boolean>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val filteredApps = remember(pickedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            pickedApps
        } else {
            pickedApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val bestMatch = remember(searchQuery, filteredApps) {
        if (searchQuery.isNotBlank() && filteredApps.isNotEmpty()) filteredApps.first() else null
    }

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
                    text = stringResource(R.string.msg_no_apps_frozen),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onGetStartedClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onGetStartedClick()
                        }
                    ) {
                        Text(stringResource(R.string.action_get_started))
                    }
                }
            }
        } else {
            val isShizukuAvailable by viewModel.isShizukuAvailable
            val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
            var isMenuExpanded by remember { mutableStateOf(false) }
            val scrollState = androidx.compose.foundation.rememberScrollState()

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { stream ->
                            viewModel.exportFreezeApps(stream)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let {
                    try {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            viewModel.importFreezeApps(context, stream)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
            ) {
                Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_search_24),
                            contentDescription = stringResource(R.string.label_search_content_description),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                HapticUtil.performVirtualKeyHaptic(view)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_close_24),
                                    contentDescription = stringResource(R.string.action_stop)
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(stringResource(R.string.search_frozen_apps_placeholder))
                    },
                    shape = MaterialTheme.shapes.extraExtraLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            bestMatch?.let { app ->
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.launchAndUnfreezeApp(context, app.packageName)
                                onAppLaunched?.invoke()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                RoundedCardContainer(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Freeze Button
                        Button(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.freezeAllAuto(context)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isShizukuAvailable && isShizukuPermissionGranted,
                            shape = ButtonDefaults.shape
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_freeze))
                        }

                        Spacer(Modifier.size(ButtonGroupDefaults.ConnectedSpaceBetween))

                        // Unfreeze Button
                        Button(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.unfreezeAllAuto(context)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isShizukuAvailable && isShizukuPermissionGranted,
                            shape = ButtonDefaults.shape
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.action_unfreeze))
                        }

                        // More Menu Button
                        IconButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isMenuExpanded = true
                            },
                            enabled = isShizukuAvailable && isShizukuPermissionGranted
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_more_vert_24),
                                contentDescription = stringResource(R.string.content_desc_more_options)
                            )

                            SegmentedDropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }
                            ) {
                                SegmentedDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_freeze_all)) },
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        viewModel.freezeAllManual(context)
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                SegmentedDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_unfreeze_all)) },
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        viewModel.unfreezeAllManual(context)
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                SegmentedDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_export_freeze)) },
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        exportLauncher.launch("freeze_apps_backup.json")
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_arrow_warm_up_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                SegmentedDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_import_freeze)) },
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        importLauncher.launch(arrayOf("application/json"))
                                        isMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_arrow_cool_down_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                onSettingsClick?.let { onSettings ->
                                    SegmentedDropdownMenuItem(
                                        text = { Text(stringResource(R.string.label_settings)) },
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            onSettings()
                                            isMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.rounded_settings_heart_24),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Grid Items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "¯\\_(ツ)_/¯",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = R.string.search_no_results, searchQuery),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        val chunkedApps = filteredApps.chunked(4)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            chunkedApps.forEach { rowApps ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowApps.forEach { app ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            AppGridItem(
                                                app = app,
                                                isFrozen = frozenStates[app.packageName] ?: false,
                                                isAutoFreezeEnabled = app.isEnabled,
                                                isHighlighted = (app == bestMatch && searchQuery.isNotEmpty()),
                                                menuState = menuState,
                                                onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    viewModel.launchAndUnfreezeApp(
                                                        context,
                                                        app.packageName
                                                    )
                                                    onAppLaunched?.invoke()
                                                },
                                                onToggleFreeze = {
                                                    scope.launch(Dispatchers.IO) {
                                                        val isCurrentlyFrozen = frozenStates[app.packageName] ?: false
                                                        if (isCurrentlyFrozen) {
                                                            FreezeManager.unfreezeApp(context, app.packageName)
                                                        } else {
                                                            FreezeManager.freezeApp(context, app.packageName)
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            frozenStates[app.packageName] = !isCurrentlyFrozen
                                                        }
                                                    }
                                                },
                                                onRemove = {
                                                    viewModel.updateFreezeAppEnabled(context, app.packageName, false)
                                                }
                                            )
                                        }
                                    }
                                    repeat(4 - rowApps.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
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
    isHighlighted: Boolean = false,
    menuState: com.sameerasw.essentials.ui.state.MenuStateManager,
    onClick: () -> Unit,
    onToggleFreeze: () -> Unit,
    onRemove: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val isBlurred = menuState.activeId != null && menuState.activeId != app.packageName
    val blurRadius by animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha"
    )

    DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = app.packageName
        } else {
            if (menuState.activeId == app.packageName) {
                menuState.activeId = null
            }
        }
        onDispose {
            if (menuState.activeId == app.packageName) {
                menuState.activeId = null
            }
        }
    }

    val grayscaleMatrix = remember { ColorMatrix().apply { setToSaturation(0.4f) } }
    
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColorAnimation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        border = if (isHighlighted) BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .blur(blurRadius)
            .combinedClickable(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
                onLongClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showMenu = true
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

            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                SegmentedDropdownMenuItem(
                    text = {
                        Text(if (isFrozen) stringResource(R.string.action_unfreeze) else stringResource(R.string.action_freeze))
                    },
                    onClick = {
                        showMenu = false
                        onToggleFreeze()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = if (isFrozen) R.drawable.rounded_mode_cool_off_24 else R.drawable.rounded_mode_cool_24),
                            contentDescription = null
                        )
                    }
                )

                SegmentedDropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.action_remove))
                    },
                    onClick = {
                        showMenu = false
                        onRemove()
                    },
                    enabled = !isFrozen,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_24),
                            contentDescription = null
                        )
                    }
                )

                SegmentedDropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.action_create_shortcut))
                    },
                    onClick = {
                        showMenu = false
                        ShortcutUtil.pinAppShortcut(context, app)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_home_health_24),
                            contentDescription = null
                        )
                    }
                )

                SegmentedDropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.action_app_info))
                    },
                    onClick = {
                        showMenu = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_info_24),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}
