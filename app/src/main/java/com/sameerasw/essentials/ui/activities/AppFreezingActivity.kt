package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.utils.ShortcutUtil
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class AppFreezingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        
        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val context = LocalContext.current
                val view = LocalView.current
                val pickedApps by viewModel.freezePickedApps
                val isPickedAppsLoading by viewModel.isFreezePickedAppsLoading
                val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled
    
                val gridState = rememberLazyGridState()
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                val frozenStates = remember { mutableStateMapOf<String, Boolean>() }
                val lifecycleOwner = LocalLifecycleOwner.current
                
                // Refresh frozen states when activity gains focus
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

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = getString(R.string.freeze_activity_title),
                            subtitle = getString(R.string.freeze_activity_subtitle),
                            hasBack = false,
                            scrollBehavior = scrollBehavior,
                            actions = {
                                IconButton(onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                                        putExtra("feature", "Freeze")
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_settings_24),
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        ExpandableFreezeFab(
                            onUnfreezeAll = { viewModel.unfreezeAllApps(context) },
                            onFreezeAll = { viewModel.freezeAllApps(context) },
                            onFreezeAutomatic = { viewModel.freezeAutomaticApps(context) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
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
                                    .padding(24.dp)
                            ) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 88.dp),
                                    state = gridState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 88.dp),
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
                                                // Finish after launch
                                                finish()
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
                Icon(
                    painter = painterResource(
                        id = if (checkedProgress > 0.5f) R.drawable.rounded_close_24 else R.drawable.rounded_mode_cool_24
                    ),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
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
