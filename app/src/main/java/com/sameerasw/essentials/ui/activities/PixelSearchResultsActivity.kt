/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: PixelSearchResultsActivity.kt
 * Description: Activity displaying categorized search results from Pixel Launcher search bar.
 */

package com.sameerasw.essentials.ui.activities

import android.app.DownloadManager
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.WindowInsetsAnimationControlListener
import android.view.WindowInsetsAnimationController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.domain.registry.QSTileInfo
import com.sameerasw.essentials.domain.registry.QSTileRegistry
import com.sameerasw.essentials.domain.registry.SearchRegistry
import com.sameerasw.essentials.ui.activities.PixelSearchbarSettingsActivity
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import com.sameerasw.essentials.ui.activities.YourAndroidActivity
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.features.tiles.QSTilesSearchResultCard
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.FileSearchUtil
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShortcutUtil
import com.sameerasw.essentials.utils.WindowingUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PixelSearchResultsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dimAmount = if (isSystemDark) 0.2f else 0.08f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 60
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(dimAmount)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(if (isSystemDark) 0.35f else 0.15f)
        }

        val initialQuery = intent.getStringExtra(SearchManager.QUERY)
            ?: intent.getStringExtra("query")
            ?: ""

        setContent {
            val repository = remember { SettingsRepository(this) }
            val isPitchBlack by repository.isPitchBlackThemeEnabled.collectAsState(initial = false)

            EssentialsTheme(pitchBlackTheme = isPitchBlack) {
                PixelSearchResultsScreen(
                    initialQuery = initialQuery,
                    onFinish = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

private class Ref<T>(var value: T? = null)

enum class PixelSearchTab(val labelRes: Int, val iconRes: Int? = null) {
    ALL(R.string.pixel_search_tab_all),
    APPS(R.string.pixel_search_tab_apps, R.drawable.rounded_apps_24),
    MEDIA(R.string.pixel_search_tab_media, R.drawable.rounded_image_24),
    FILES(R.string.pixel_search_tab_files, R.drawable.rounded_description_24),
    CONTACTS(R.string.pixel_search_tab_contacts, R.drawable.rounded_person_24),
    SETTINGS(R.string.pixel_search_tab_settings, R.drawable.rounded_settings_24),
    WEB(R.string.pixel_search_tab_web, R.drawable.rounded_language_24),
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun PixelSearchResultsScreen(
    initialQuery: String,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    var selectedFileActionItem by remember { mutableStateOf<PixelSearchResultItem.FileItem?>(null) }
    var selectedAppActionItem by remember { mutableStateOf<PixelSearchResultItem.AppItem?>(null) }
    var selectedContactActionItem by remember { mutableStateOf<PixelSearchResultItem.ContactItem?>(null) }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(density) { 180.dp.toPx() }

    var insetsAnimController by remember { mutableStateOf<WindowInsetsAnimationController?>(null) }

    val overscrollNestedScrollConnection = remember {
        object : NestedScrollConnection {

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (insetsAnimController == null) {
                            val rootInsets = view.rootWindowInsets
                            if (rootInsets != null &&
                                rootInsets.isVisible(android.view.WindowInsets.Type.ime())
                            ) {
                                view.windowInsetsController?.controlWindowInsetsAnimation(
                                    android.view.WindowInsets.Type.ime(),
                                    -1L, null, null,
                                    object : WindowInsetsAnimationControlListener {
                                        override fun onReady(controller: WindowInsetsAnimationController, types: Int) {
                                            insetsAnimController = controller
                                        }
                                        override fun onFinished(controller: WindowInsetsAnimationController) {
                                            insetsAnimController = null
                                        }
                                        override fun onCancelled(controller: WindowInsetsAnimationController?) {
                                            insetsAnimController = null
                                        }
                                    }
                                )
                            }
                        }
                        val ctrl = insetsAnimController
                        if (ctrl != null) {
                            val shown = ctrl.shownStateInsets.bottom.toFloat()
                            if (shown > 0f) {
                                val target = (shown - dragOffsetY).coerceIn(0f, shown)
                                ctrl.setInsetsAndAlpha(
                                    android.graphics.Insets.of(0, 0, 0, target.toInt()),
                                    1f,
                                    target / shown,
                                )
                            }
                        }
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                if (available.y < 0f && dragOffsetY > 0f) {
                    val snap = minOf(-available.y, dragOffsetY)
                    dragOffsetY = (dragOffsetY - snap).coerceAtLeast(0f)
                    return Offset(0f, -snap)
                }

                if (available.y < 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (insetsAnimController == null) {
                        val rootInsets = view.rootWindowInsets
                        if (rootInsets != null &&
                            rootInsets.isVisible(android.view.WindowInsets.Type.ime())
                        ) {
                            view.windowInsetsController?.controlWindowInsetsAnimation(
                                android.view.WindowInsets.Type.ime(),
                                -1L, null, null,
                                object : WindowInsetsAnimationControlListener {
                                    override fun onReady(controller: WindowInsetsAnimationController, types: Int) {
                                        insetsAnimController = controller
                                    }
                                    override fun onFinished(controller: WindowInsetsAnimationController) {
                                        insetsAnimController = null
                                    }
                                    override fun onCancelled(controller: WindowInsetsAnimationController?) {
                                        insetsAnimController = null
                                    }
                                }
                            )
                        }
                    }
                    val ctrl = insetsAnimController
                    if (ctrl != null) {
                        val shown = ctrl.shownStateInsets.bottom.toFloat()
                        if (shown > 0f) {
                            val current = ctrl.currentInsets.bottom.toFloat()
                            val target = (current + available.y).coerceIn(0f, shown)
                            ctrl.setInsetsAndAlpha(
                                android.graphics.Insets.of(0, 0, 0, target.toInt()),
                                1f,
                                target / shown,
                            )
                        }
                    }
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragOffsetY > dismissThresholdPx) {
                    onFinish()
                    return Velocity.Zero
                } else if (dragOffsetY > 0f) {
                    scope.launch {
                        animate(
                            initialValue = dragOffsetY,
                            targetValue = 0f,
                            animationSpec = spring(),
                        ) { v, _ -> dragOffsetY = v }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val ctrl = insetsAnimController
                    if (ctrl != null) {
                        insetsAnimController = null
                        val current = ctrl.currentInsets.bottom.toFloat()
                        val shown = ctrl.shownStateInsets.bottom.toFloat()
                        val shouldShow = !(shown > 0f && (available.y < -500f || current < shown * 0.5f))
                        val target = if (shouldShow) shown else 0f
                        animate(
                            initialValue = current,
                            targetValue = target,
                            initialVelocity = (-available.y).coerceIn(-shown * 15f, shown * 15f),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ) { value, _ ->
                            if (shown > 0f) {
                                ctrl.setInsetsAndAlpha(
                                    android.graphics.Insets.of(0, 0, 0, value.toInt()),
                                    1f,
                                    value / shown,
                                )
                            }
                        }
                        ctrl.finish(shouldShow)
                    }
                }

                return Velocity.Zero
            }
        }
    }

    val listState = rememberLazyListState()
    var userHasScrolled by remember { mutableStateOf(false) }

    if (listState.isScrollInProgress) {
        DisposableEffect(Unit) {
            userHasScrolled = true
            onDispose { }
        }
    }

    val viewModel: MainViewModel = viewModel()
    var appResults by remember { mutableStateOf<List<PixelSearchResultItem.AppItem>>(emptyList()) }
    var contactResults by remember { mutableStateOf<List<PixelSearchResultItem.ContactItem>>(emptyList()) }
    var systemSettingResults by remember { mutableStateOf<List<PixelSearchResultItem.SystemSettingItem>>(emptyList()) }
    var settingResults by remember { mutableStateOf<List<PixelSearchResultItem.SettingItem>>(emptyList()) }
    var matchingQsTiles by remember { mutableStateOf<List<QSTileInfo>>(emptyList()) }
    var shortcutResults by remember { mutableStateOf<List<PixelSearchResultItem.ShortcutItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var allInstalledApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    val searchJobRef = remember { Ref<Job>() }
    var mathResult by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(PixelSearchTab.ALL) }
    var mediaResults by remember { mutableStateOf<List<PixelSearchResultItem.FileItem>>(emptyList()) }
    var fileResults by remember { mutableStateOf<List<PixelSearchResultItem.FileItem>>(emptyList()) }

    val isAppsEnabled = remember { repository.isPixelSearchResultAppsEnabled() }
    val isContactsEnabled = remember { repository.isPixelSearchResultContactsEnabled() }
    val isSettingsEnabled = remember { repository.isPixelSearchResultSettingsEnabled() }
    val isShortcutsEnabled = remember { repository.isPixelSearchResultShortcutsEnabled() }
    val isFilesEnabled = remember { repository.isPixelSearchResultFilesEnabled() }
    val isWebEnabled = remember { repository.isPixelSearchResultWebEnabled() }
    val isBubblesWebEnabled = remember { repository.isPixelSearchBubblesWebEnabled() }
    val searchEngine = remember { repository.getPixelSearchEngine() }

    fun performSearch(q: String, currentTab: PixelSearchTab = selectedTab) {
        searchJobRef.value?.cancel()
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            mathResult = null
            appResults = emptyList()
            contactResults = emptyList()
            systemSettingResults = emptyList()
            settingResults = emptyList()
            matchingQsTiles = emptyList()
            shortcutResults = emptyList()
            if (isFilesEnabled && (currentTab == PixelSearchTab.MEDIA || currentTab == PixelSearchTab.FILES)) {
                isSearching = true
                searchJobRef.value = scope.launch(Dispatchers.IO) {
                    val fileSearchResults = FileSearchUtil.searchFiles(context, "", limit = 24)
                    withContext(Dispatchers.Main) {
                        mediaResults = fileSearchResults.mediaItems
                        fileResults = fileSearchResults.documentItems
                        isSearching = false
                    }
                }
            } else {
                mediaResults = emptyList()
                fileResults = emptyList()
                isSearching = false
            }
            return
        }

        mathResult = MathEvaluator.evaluate(trimmed)
        isSearching = true
        searchJobRef.value = scope.launch(Dispatchers.IO) {
            if (isFilesEnabled) {
                val fileSearchResults = FileSearchUtil.searchFiles(context, trimmed, limit = 24)
                withContext(Dispatchers.Main) {
                    mediaResults = fileSearchResults.mediaItems
                    fileResults = fileSearchResults.documentItems
                }
            }
            if (isAppsEnabled) {
                val installed = allInstalledApps.ifEmpty { AppUtil.getInstalledApps(context, includeSelf = true) }
                val installedPkgs = installed.map { it.packageName }.toSet()
                val freezeSelections = repository.loadFreezeSelectedApps()
                val missingFrozenPkgs = freezeSelections
                    .map { it.packageName }
                    .filter { !installedPkgs.contains(it) }

                val additionalFrozenApps = if (missingFrozenPkgs.isNotEmpty()) {
                    AppUtil.getAppsByPackageNames(context, missingFrozenPkgs)
                } else {
                    emptyList()
                }

                val allApps = installed + additionalFrozenApps
                val filteredApps = allApps
                    .filter { app ->
                        app.appName.contains(trimmed, ignoreCase = true)
                    }
                    .map {
                        val isFrozen = FreezeManager.isAppFrozen(context, it.packageName)
                        val icon = if (isFrozen) {
                            applyGrayscaleFilter(it.icon)
                        } else {
                            it.icon
                        }
                        PixelSearchResultItem.AppItem(
                            appName = it.appName,
                            packageName = it.packageName,
                            icon = icon,
                            isSystemApp = it.isSystemApp,
                            isFrozen = isFrozen,
                        )
                    }
                    .sortedWith(compareBy({ !it.isFrozen }, { it.appName.lowercase() }))
                    .take(6)
                withContext(Dispatchers.Main) {
                    appResults = filteredApps
                }
            }

            if (isContactsEnabled && ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CONTACTS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val contacts = loadContacts(context, trimmed)
                withContext(Dispatchers.Main) {
                    contactResults = contacts
                }
            }

            if (isSettingsEnabled) {
                val systemSettings = loadSystemSettings(trimmed)
                val results = SearchRegistry.search(context, trimmed, repository.isEnableUnsupportedFeatures())
                val qsTiles = QSTileRegistry.searchTiles(
                    context = context,
                    query = trimmed,
                    includeUnsupported = repository.isEnableUnsupportedFeatures(),
                    isUseUsageStats = repository.getBoolean(SettingsRepository.KEY_USE_USAGE_ACCESS),
                )
                val mappedSettings = results
                    .filter { it.featureKey != "Quick settings tiles" }
                    .take(6)
                    .map {
                        PixelSearchResultItem.SettingItem(it)
                    }
                withContext(Dispatchers.Main) {
                    systemSettingResults = systemSettings
                    settingResults = mappedSettings
                    matchingQsTiles = qsTiles
                }
            }

            if (isShortcutsEnabled) {
                val shortcuts = loadShortcuts(context, trimmed)
                withContext(Dispatchers.Main) {
                    shortcutResults = shortcuts
                }
            }

            withContext(Dispatchers.Main) {
                isSearching = false
            }
        }
    }

    LaunchedEffect(query) {
        userHasScrolled = false
        performSearch(query)
    }

    LaunchedEffect(selectedTab) {
        if (query.isBlank() && (selectedTab == PixelSearchTab.MEDIA || selectedTab == PixelSearchTab.FILES)) {
            performSearch(query, selectedTab)
        }
    }

    LaunchedEffect(Unit) {
        val installed = withContext(Dispatchers.IO) {
            AppUtil.getInstalledApps(context, includeSelf = true)
        }
        allInstalledApps = installed
        if (query.isNotBlank()) {
            performSearch(query)
        }
    }

    LaunchedEffect(selectedTab, mathResult, appResults, mediaResults, fileResults, contactResults, systemSettingResults, settingResults, shortcutResults) {
        if (!userHasScrolled && (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0)) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarHeightPx = with(LocalDensity.current) { statusBarHeight.toPx() }
    val bottomBlurHeightPx = with(LocalDensity.current) { 140.dp.toPx() }

    // Identify which section is topmost
    val isAllTab = selectedTab == PixelSearchTab.ALL
    val hasCalculation = isAllTab && mathResult != null
    val hasApps = !hasCalculation && (isAllTab || selectedTab == PixelSearchTab.APPS) && isAppsEnabled && appResults.isNotEmpty()
    val hasMedia = !hasCalculation && !hasApps && (isAllTab || selectedTab == PixelSearchTab.MEDIA) && isFilesEnabled && mediaResults.isNotEmpty()
    val hasFiles = !hasCalculation && !hasApps && !hasMedia && (isAllTab || selectedTab == PixelSearchTab.FILES) && isFilesEnabled && fileResults.isNotEmpty()
    val hasContacts = !hasCalculation && !hasApps && !hasMedia && !hasFiles && (isAllTab || selectedTab == PixelSearchTab.CONTACTS) && isContactsEnabled && contactResults.isNotEmpty()
    val hasSystemSettings = !hasCalculation && !hasApps && !hasMedia && !hasFiles && !hasContacts && (isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isSettingsEnabled && systemSettingResults.isNotEmpty()
    val hasEssentials = !hasCalculation && !hasApps && !hasMedia && !hasFiles && !hasContacts && !hasSystemSettings && (isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isSettingsEnabled && (matchingQsTiles.isNotEmpty() || settingResults.isNotEmpty())
    val hasShortcuts = !hasCalculation && !hasApps && !hasMedia && !hasFiles && !hasContacts && !hasSystemSettings && !hasEssentials && (isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isShortcutsEnabled && shortcutResults.isNotEmpty()

    val highlightColor = MaterialTheme.colorScheme.secondaryContainer
    val normalCardColor = MaterialTheme.colorScheme.surfaceBright

    fun launchTopmostOrWeb() {
        when {
            hasCalculation -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Calculation Result", mathResult)
                clipboard.setPrimaryClip(clip)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
                onFinish()
            }
            hasApps -> {
                launchApp(context, appResults.first().packageName)
                onFinish()
            }
            hasMedia -> {
                launchFile(context, mediaResults.first())
                onFinish()
            }
            hasFiles -> {
                launchFile(context, fileResults.first())
                onFinish()
            }
            hasContacts -> {
                contactResults.first().phoneNumber?.let {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                    context.startActivity(dialIntent)
                    onFinish()
                } ?: run {
                    if (query.isNotBlank()) launchWebSearch(context, query, isBubblesWebEnabled, searchEngine)
                    onFinish()
                }
            }
            hasSystemSettings -> {
                context.startActivity(systemSettingResults.first().intent)
                onFinish()
            }
            hasEssentials -> {
                val setting = settingResults.firstOrNull()?.searchableItem
                if (setting != null) {
                    val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                        putExtra("feature", setting.featureKey)
                        setting.targetSettingHighlightKey?.let {
                            putExtra("highlight_setting", it)
                        }
                    }
                    context.startActivity(intent)
                }
                onFinish()
            }
            hasShortcuts -> {
                context.startActivity(shortcutResults.first().intent)
                onFinish()
            }
            else -> {
                if (query.isNotBlank()) {
                    launchWebSearch(context, query, isBubblesWebEnabled, searchEngine)
                }
                onFinish()
            }
        }
    }

    val quickLaunchApps = remember(allInstalledApps) {
        allInstalledApps
            .filter { !it.isSystemApp }
            .sortedByDescending { it.lastUpdated }
            .take(5)
            .ifEmpty { allInstalledApps.take(5) }
    }

    val scrimColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .progressiveBlur(
                blurRadius = 40f,
                height = statusBarHeightPx * 1.2f,
                direction = BlurDirection.TOP,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, dragOffsetY.roundToInt()) },
        ) {
            // Pinned top category filter tabs
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(15f)
                    .padding(top = statusBarHeight),
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(PixelSearchTab.values()) { tab ->
                        val isSelected = selectedTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                selectedTab = tab
                                if (query.isBlank() && (tab == PixelSearchTab.MEDIA || tab == PixelSearchTab.FILES)) {
                                    performSearch(query, tab)
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            leadingIcon = tab.iconRes?.let { iconRes ->
                                {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            border = null,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .progressiveBlur(
                        blurRadius = 40f,
                        height = bottomBlurHeightPx,
                        direction = BlurDirection.BOTTOM,
                    ),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(overscrollNestedScrollConnection)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        top = statusBarHeight + 58.dp,
                        bottom = 120.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                // QUICK LAUNCH (Empty state)
                if ((isAllTab || selectedTab == PixelSearchTab.APPS) && query.isBlank() && quickLaunchApps.isNotEmpty()) {
                    item(key = "quick_launch_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_quick_launch),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "quick_launch_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            quickLaunchApps.forEach { app ->
                                FeatureCard(
                                    title = app.appName,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        launchApp(context, app.packageName)
                                        onFinish()
                                    },
                                    onLongClick = {
                                        selectedAppActionItem = PixelSearchResultItem.AppItem(
                                            appName = app.appName,
                                            packageName = app.packageName,
                                            icon = app.icon,
                                            isSystemApp = app.isSystemApp,
                                        )
                                    },
                                    containerColor = normalCardColor,
                                    showToggle = false,
                                    hasMoreSettings = false,
                                    iconPainter = BitmapPainter(app.icon),
                                    iconSize = 36.dp,
                                    hasIconBackground = false,
                                )
                            }
                        }
                    }
                }

                // CALCULATION RESULT
                if (isAllTab && mathResult != null) {
                    item(key = "calculation_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_calculation),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "calculation_card") {
                        val resultText = mathResult ?: ""
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            FeatureCard(
                                title = "= $resultText",
                                description = query.trim(),
                                isEnabled = true,
                                onToggle = {},
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Calculation Result", resultText)
                                    clipboard.setPrimaryClip(clip)
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                    }
                                    onFinish()
                                },
                                containerColor = highlightColor,
                                iconRes = R.drawable.rounded_calculate_24,
                                hasIconBackground = true,
                                iconTint = MaterialTheme.colorScheme.primary,
                                showToggle = false,
                                hasMoreSettings = false,
                                customTrailingContent = {
                                    IconButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Calculation Result", resultText)
                                            clipboard.setPrimaryClip(clip)
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_content_copy_24),
                                            contentDescription = stringResource(R.string.pixel_search_copied_to_clipboard),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                // APPS
                if ((isAllTab || selectedTab == PixelSearchTab.APPS) && isAppsEnabled && appResults.isNotEmpty()) {
                    item(key = "apps_header") {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_apps), Modifier.animateItem())
                    }
                    item(key = "apps_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            appResults.forEachIndexed { index, app ->
                                val isTopmost = index == 0 && hasApps
                                FeatureCard(
                                    title = app.appName,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        if (app.isFrozen) {
                                            viewModel.launchAndUnfreezeApp(context, app.packageName)
                                        } else {
                                            launchApp(context, app.packageName)
                                        }
                                        onFinish()
                                    },
                                    onLongClick = {
                                        selectedAppActionItem = app
                                    },
                                    containerColor = if (isTopmost) highlightColor else normalCardColor,
                                    showToggle = false,
                                    hasMoreSettings = app.isFrozen,
                                    description = if (app.isFrozen) stringResource(R.string.action_unfreeze) else null,
                                    customTrailingContent = if (app.isFrozen) {
                                        {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_mode_cool_24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    } else null,
                                    additionalMenuItems = if (app.isFrozen) {
                                        { onDismiss ->
                                            SegmentedDropdownMenuItem(
                                                text = { Text(stringResource(R.string.action_unfreeze)) },
                                                onClick = {
                                                    onDismiss()
                                                    scope.launch(Dispatchers.IO) {
                                                        FreezeManager.unfreezeApp(context, app.packageName)
                                                        viewModel.refreshFreezePickedApps(context, silent = true)
                                                        performSearch(query)
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_mode_cool_off_24),
                                                        contentDescription = null,
                                                    )
                                                },
                                            )
                                            SegmentedDropdownMenuItem(
                                                text = { Text(stringResource(R.string.action_app_info)) },
                                                onClick = {
                                                    onDismiss()
                                                    val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.fromParts("package", app.packageName, null)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(infoIntent)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_info_24),
                                                        contentDescription = null,
                                                    )
                                                },
                                            )
                                        }
                                    } else null,
                                    iconPainter = if (app.icon != null) {
                                        BitmapPainter(app.icon)
                                    } else null,
                                    iconRes = if (app.icon == null) R.drawable.rounded_apps_24 else null,
                                    iconSize = 36.dp,
                                    hasIconBackground = app.icon == null,
                                )
                            }
                        }
                    }
                }

                // PHOTOS & VIDEOS (MEDIA)
                if ((isAllTab || selectedTab == PixelSearchTab.MEDIA) && isFilesEnabled && mediaResults.isNotEmpty()) {
                    item(key = "media_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_media),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "media_cards") {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(mediaResults, key = { it.id }) { media ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .combinedClickable(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                launchFile(context, media)
                                                onFinish()
                                            },
                                            onLongClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                selectedFileActionItem = media
                                            },
                                        ),
                                ) {
                                    AsyncImage(
                                        model = media.uri,
                                        contentDescription = media.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (media.isGif) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp),
                                        ) {
                                            Text(
                                                text = "GIF",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    } else if (media.isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.rounded_play_arrow_24),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // FILES & DOCUMENTS
                if ((isAllTab || selectedTab == PixelSearchTab.FILES) && isFilesEnabled && fileResults.isNotEmpty()) {
                    item(key = "files_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_files),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "files_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            fileResults.forEachIndexed { index, file ->
                                val isTopmost = index == 0 && hasFiles
                                FeatureCard(
                                    title = file.displayName,
                                    description = FileSearchUtil.formatFileSize(file.sizeBytes),
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        launchFile(context, file)
                                        onFinish()
                                    },
                                    onLongClick = {
                                        selectedFileActionItem = file
                                    },
                                    containerColor = if (isTopmost) highlightColor else normalCardColor,
                                    iconRes = file.iconRes,
                                    hasIconBackground = true,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    showToggle = false,
                                    hasMoreSettings = false,
                                )
                            }
                        }
                    }
                }

                // CONTACTS
                if ((isAllTab || selectedTab == PixelSearchTab.CONTACTS) && isContactsEnabled && contactResults.isNotEmpty()) {
                    item(key = "contacts_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_contacts),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "contacts_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            contactResults.forEachIndexed { index, contact ->
                                val isTopmost = index == 0 && hasContacts
                                ListItem(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        contact.phoneNumber?.let {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                            context.startActivity(dialIntent)
                                            onFinish()
                                        }
                                    },
                                    onLongClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        selectedContactActionItem = contact
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingContent = {
                                        if (!contact.photoUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = contact.photoUri,
                                                contentDescription = contact.name,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(ColorUtil.getPastelColorFor(contact.name)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = contact.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = ColorUtil.getVibrantColorFor(contact.name),
                                                )
                                            }
                                        }
                                    },
                                    supportingContent = {
                                        contact.phoneNumber?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        contact.phoneNumber?.let { num ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                                    context.startActivity(callIntent)
                                                    onFinish()
                                                }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_call_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                                    context.startActivity(smsIntent)
                                                    onFinish()
                                                }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_chat_bubble_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isTopmost) highlightColor else normalCardColor,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                ) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                // SETTINGS (System Settings)
                if ((isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isSettingsEnabled && systemSettingResults.isNotEmpty()) {
                    item(key = "system_settings_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_system_settings),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "system_settings_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            systemSettingResults.forEachIndexed { index, setting ->
                                val isTopmost = index == 0 && hasSystemSettings
                                FeatureCard(
                                    title = setting.title,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        context.startActivity(setting.intent)
                                        onFinish()
                                    },
                                    containerColor = if (isTopmost) highlightColor else normalCardColor,
                                    iconRes = setting.iconRes,
                                    showToggle = false,
                                    hasMoreSettings = false,
                                    description = setting.subtitle,
                                )
                            }
                        }
                    }
                }

                // ESSENTIALS
                if ((isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isSettingsEnabled && (matchingQsTiles.isNotEmpty() || settingResults.isNotEmpty())) {
                    item(key = "essentials_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_essentials),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (settingResults.isNotEmpty()) {
                        item(key = "essentials_cards") {
                            RoundedCardContainer(modifier = Modifier.animateItem()) {
                                settingResults.forEachIndexed { index, item ->
                                    val isTopmost = index == 0 && hasEssentials
                                    val setting = item.searchableItem
                                    FeatureCard(
                                        title = setting.title,
                                        isEnabled = true,
                                        onToggle = {},
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            val feature = FeatureRegistry.ALL_FEATURES.find { it.id == setting.featureKey }
                                            val targetFeatureKey =
                                                if (feature != null && !feature.hasMoreSettings && feature.parentFeatureId != null) {
                                                    feature.parentFeatureId
                                                } else {
                                                    setting.featureKey
                                                }
                                            val highlightKey =
                                                if (feature != null && !feature.hasMoreSettings && feature.parentFeatureId != null) {
                                                    feature.id
                                                } else {
                                                    setting.targetSettingHighlightKey
                                                }

                                            val intent =
                                                if (targetFeatureKey == "Pixel Searchbar") {
                                                    Intent(context, PixelSearchbarSettingsActivity::class.java)
                                                } else if (targetFeatureKey == "LiveWallpaper" || targetFeatureKey == "Daily Wallpaper") {
                                                    Intent(context, WallpaperActivity::class.java).apply {
                                                        putExtra(
                                                            "tab",
                                                            if (targetFeatureKey == "LiveWallpaper") "live" else "daily",
                                                        )
                                                    }
                                                } else if (targetFeatureKey == "App updates") {
                                                    Intent(context, YourAndroidActivity::class.java)
                                                } else {
                                                    Intent(context, FeatureSettingsActivity::class.java).apply {
                                                        putExtra("feature", targetFeatureKey)
                                                        highlightKey?.let {
                                                            putExtra("highlight_setting", it)
                                                        }
                                                    }
                                                }
                                            context.startActivity(intent)
                                            onFinish()
                                        },
                                        containerColor = if (isTopmost) highlightColor else normalCardColor,
                                        iconRes = setting.icon ?: R.drawable.rounded_settings_24,
                                        showToggle = false,
                                        hasMoreSettings = true,
                                        description = setting.description,
                                        isBeta = setting.isBeta,
                                        descriptionOverride =
                                            if (setting.parentFeature != null) {
                                                "${setting.parentFeature} > ${setting.description}"
                                            } else {
                                                setting.description
                                            },
                                    )
                                }
                            }
                        }
                    }
                    if (matchingQsTiles.isNotEmpty()) {
                        item(key = "essentials_qs_tiles") {
                            QSTilesSearchResultCard(
                                tiles = matchingQsTiles,
                                viewModel = viewModel,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                // SHORTCUTS
                if ((isAllTab || selectedTab == PixelSearchTab.SETTINGS) && isShortcutsEnabled && shortcutResults.isNotEmpty()) {
                    item(key = "shortcuts_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_shortcuts),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "shortcuts_cards") {
                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            shortcutResults.forEachIndexed { index, shortcut ->
                                val isTopmost = index == 0 && hasShortcuts
                                FeatureCard(
                                    title = shortcut.label,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        context.startActivity(shortcut.intent)
                                        onFinish()
                                    },
                                    containerColor = if (isTopmost) highlightColor else normalCardColor,
                                    iconRes = shortcut.iconRes,
                                    showToggle = false,
                                    hasMoreSettings = false,
                                    description = shortcut.subtitle,
                                )
                            }
                        }
                    }
                }

                // WEB SEARCH
                if ((isAllTab || selectedTab == PixelSearchTab.WEB) && isWebEnabled && query.isNotBlank()) {
                    val isTopmost = !hasCalculation && !hasApps && !hasMedia && !hasFiles && !hasContacts && !hasSystemSettings && !hasEssentials && !hasShortcuts
                    item(key = "web_header") {
                        SearchSectionHeader(
                            stringResource(R.string.pixel_search_section_web),
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "web_cards") {
                        val isUrl = remember(query) { isLikelyUrl(query.trim()) }
                        val actionTitle = if (isUrl) {
                            stringResource(R.string.pixel_search_open_url_action, query)
                        } else {
                            stringResource(R.string.pixel_search_web_search_action, query)
                        }
                        val webIcon = if (isUrl) R.drawable.rounded_link_24 else R.drawable.rounded_language_24

                        RoundedCardContainer(modifier = Modifier.animateItem()) {
                            FeatureCard(
                                title = actionTitle,
                                isEnabled = true,
                                onToggle = {},
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    launchWebSearch(context, query, isBubblesWebEnabled, searchEngine)
                                    onFinish()
                                },
                                containerColor = if (isTopmost) highlightColor else normalCardColor,
                                iconRes = webIcon,
                                hasIconBackground = false,
                                iconTint = MaterialTheme.colorScheme.primary,
                                showToggle = false,
                                hasMoreSettings = false,
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

            Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(20f)
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = navBarHeight.coerceAtLeast(16.dp)),
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.pixel_search_input_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        when {
                            isSearching -> LoadingIndicator()
                            query.isNotEmpty() -> IconButton(onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                query = ""
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_close_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            keyboardController?.hide()
                            launchTopmostOrWeb()
                        }
                    ),
                )
            }
        }
    }

    }

    val activeFile = selectedFileActionItem
    if (activeFile != null) {
        EssentialsBottomSheet(
            onDismissRequest = { selectedFileActionItem = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (activeFile.isImage || activeFile.isVideo || activeFile.isGif) {
                        AsyncImage(
                            model = activeFile.uri,
                            contentDescription = activeFile.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = ColorUtil.getPastelColorFor(activeFile.displayName),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(activeFile.iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeFile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val details = buildString {
                            append(FileSearchUtil.formatFileSize(activeFile.sizeBytes))
                            activeFile.path?.let { p ->
                                append(" • ")
                                append(p)
                            }
                        }
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_open),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedFileActionItem
                            selectedFileActionItem = null
                            if (target != null) {
                                launchFile(context, target)
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_open_in_new_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_open_with),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedFileActionItem
                            selectedFileActionItem = null
                            if (target != null) {
                                openWithFile(context, target)
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_apps_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_show_in_folder),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedFileActionItem
                            selectedFileActionItem = null
                            if (target != null) {
                                openFileFolder(context, target)
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_folder_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_share),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedFileActionItem
                            selectedFileActionItem = null
                            if (target != null) {
                                shareFile(context, target)
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_share_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_copy_path),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedFileActionItem
                            selectedFileActionItem = null
                            if (target != null) {
                                copyFilePath(context, target)
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_content_copy_24,
                        hasIconBackground = true,
                    )
                }
            }
        }
    }

    val activeApp = selectedAppActionItem
    if (activeApp != null) {
        EssentialsBottomSheet(
            onDismissRequest = { selectedAppActionItem = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (activeApp.icon != null) {
                        Image(
                            painter = BitmapPainter(activeApp.icon),
                            contentDescription = activeApp.appName,
                            modifier = Modifier.size(52.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = ColorUtil.getPastelColorFor(activeApp.appName),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_apps_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeApp.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = activeApp.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_open),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedAppActionItem
                            selectedAppActionItem = null
                            if (target != null) {
                                if (target.isFrozen) {
                                    viewModel.launchAndUnfreezeApp(context, target.packageName)
                                } else {
                                    launchApp(context, target.packageName)
                                }
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_open_in_new_24,
                        hasIconBackground = true,
                    )
                    if (activeApp.isFrozen) {
                        FeatureCard(
                            title = stringResource(R.string.action_unfreeze),
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val target = selectedAppActionItem
                                selectedAppActionItem = null
                                if (target != null) {
                                    scope.launch(Dispatchers.IO) {
                                        FreezeManager.unfreezeApp(context, target.packageName)
                                        viewModel.refreshFreezePickedApps(context, silent = true)
                                        performSearch(query)
                                    }
                                }
                            },
                            showToggle = false,
                            hasMoreSettings = false,
                            iconRes = R.drawable.rounded_mode_cool_off_24,
                            hasIconBackground = true,
                        )
                    }
                    FeatureCard(
                        title = stringResource(R.string.action_app_info),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedAppActionItem
                            selectedAppActionItem = null
                            if (target != null) {
                                val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", target.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(infoIntent)
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_info_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_play_store),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedAppActionItem
                            selectedAppActionItem = null
                            if (target != null) {
                                openAppInPlayStore(context, target.packageName)
                                onFinish()
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_storefront_24,
                        hasIconBackground = true,
                    )
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_share_app),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedAppActionItem
                            selectedAppActionItem = null
                            if (target != null) {
                                shareApp(context, target.appName, target.packageName)
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_share_24,
                        hasIconBackground = true,
                    )
                }
            }
        }
    }

    val activeContact = selectedContactActionItem
    if (activeContact != null) {
        EssentialsBottomSheet(
            onDismissRequest = { selectedContactActionItem = null },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (!activeContact.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = activeContact.photoUri,
                            contentDescription = activeContact.name,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ColorUtil.getPastelColorFor(activeContact.name)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = activeContact.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = ColorUtil.getVibrantColorFor(activeContact.name),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeContact.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        activeContact.phoneNumber?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    if (activeContact.phoneNumber != null) {
                        FeatureCard(
                            title = stringResource(R.string.pixel_search_action_call),
                            description = activeContact.phoneNumber,
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val target = selectedContactActionItem
                                selectedContactActionItem = null
                                if (target?.phoneNumber != null) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${target.phoneNumber}"))
                                    context.startActivity(dialIntent)
                                    onFinish()
                                }
                            },
                            showToggle = false,
                            hasMoreSettings = false,
                            iconRes = R.drawable.rounded_call_24,
                            hasIconBackground = true,
                        )
                        FeatureCard(
                            title = stringResource(R.string.pixel_search_action_sms),
                            description = activeContact.phoneNumber,
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val target = selectedContactActionItem
                                selectedContactActionItem = null
                                if (target?.phoneNumber != null) {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${target.phoneNumber}"))
                                    context.startActivity(smsIntent)
                                    onFinish()
                                }
                            },
                            showToggle = false,
                            hasMoreSettings = false,
                            iconRes = R.drawable.rounded_chat_bubble_24,
                            hasIconBackground = true,
                        )
                        FeatureCard(
                            title = stringResource(R.string.pixel_search_action_copy_phone),
                            isEnabled = true,
                            onToggle = {},
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val target = selectedContactActionItem
                                selectedContactActionItem = null
                                if (target?.phoneNumber != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Phone Number", target.phoneNumber)
                                    clipboard.setPrimaryClip(clip)
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            showToggle = false,
                            hasMoreSettings = false,
                            iconRes = R.drawable.rounded_content_copy_24,
                            hasIconBackground = true,
                        )
                    }
                    FeatureCard(
                        title = stringResource(R.string.pixel_search_action_view_contact),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            val target = selectedContactActionItem
                            selectedContactActionItem = null
                            if (target != null) {
                                try {
                                    val contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, target.id)
                                    val intent = Intent(Intent.ACTION_VIEW, contactUri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    onFinish()
                                } catch (_: Exception) {
                                }
                            }
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_person_24,
                        hasIconBackground = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

private fun applyGrayscaleFilter(bitmap: ImageBitmap): ImageBitmap {
    val src = bitmap.asAndroidBitmap()
    val output = android.graphics.Bitmap.createBitmap(src.width, src.height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val paint = android.graphics.Paint()
    val matrix = android.graphics.ColorMatrix().apply {
        setSaturation(0.2f)
    }
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    paint.alpha = (0.65f * 255).toInt()
    canvas.drawBitmap(src, 0f, 0f, paint)
    return output.asImageBitmap()
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    } catch (_: Exception) {
    }
}

private fun isLikelyUrl(query: String): Boolean {
    val lower = query.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) return true
    if (query.contains(" ") || !query.contains(".")) return false
    val commonTlds = listOf(".com", ".org", ".net", ".io", ".dev", ".app", ".co", ".ai", ".me", ".info", ".edu", ".gov", ".lk", ".uk", ".ca", ".de", ".jp", ".fr", ".au", ".in")
    return commonTlds.any { lower.contains(it) } || android.util.Patterns.WEB_URL.matcher(query).matches()
}

private fun buildSearchOrUrl(query: String, searchEngine: String): Uri {
    val trimmed = query.trim()
    if (isLikelyUrl(trimmed)) {
        val target = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return Uri.parse(target)
    }

    val encoded = Uri.encode(trimmed)
    val url = when (searchEngine.lowercase()) {
        "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
        "brave search", "brave" -> "https://search.brave.com/search?q=$encoded"
        "startpage" -> "https://www.startpage.com/sp/search?query=$encoded"
        "kagi" -> "https://kagi.com/search?q=$encoded"
        "ecosia" -> "https://www.ecosia.org/search?q=$encoded"
        "bing" -> "https://www.bing.com/search?q=$encoded"
        else -> "https://www.google.com/search?q=$encoded"
    }
    return Uri.parse(url)
}

private fun launchWebSearch(
    context: Context,
    query: String,
    useBubbles: Boolean = false,
    searchEngine: String = "Google",
) {
    try {
        val targetUri = buildSearchOrUrl(query, searchEngine)
        if (useBubbles) {
            val launched = WindowingUtils.launchOverlayWindow(context, targetUri)
            if (launched) return
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }
}

private fun loadContacts(context: Context, query: String): List<PixelSearchResultItem.ContactItem> {
    val results = mutableListOf<PixelSearchResultItem.ContactItem>()
    try {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 5",
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = if (idIdx >= 0) cursor.getString(idIdx) else ""
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                val number = if (numIdx >= 0) cursor.getString(numIdx) else null
                val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                if (name.isNotBlank()) {
                    results.add(
                        PixelSearchResultItem.ContactItem(
                            id = id,
                            name = name,
                            phoneNumber = number,
                            photoUri = photo,
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
    }
    return results
}

private fun loadSystemSettings(query: String): List<PixelSearchResultItem.SystemSettingItem> {
    val q = query.lowercase()
    val all = listOf(
        PixelSearchResultItem.SystemSettingItem(
            title = "Wi-Fi",
            subtitle = "Network & internet",
            iconRes = R.drawable.rounded_android_wifi_3_bar_24,
            intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Bluetooth",
            subtitle = "Connected devices",
            iconRes = R.drawable.rounded_bluetooth_24,
            intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Display",
            subtitle = "Brightness, theme, screen timeout",
            iconRes = R.drawable.rounded_mobile_text_2_24,
            intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Sound & vibration",
            subtitle = "Volume, haptics, Do Not Disturb",
            iconRes = R.drawable.rounded_volume_up_24,
            intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Battery",
            subtitle = "Battery usage and saver",
            iconRes = R.drawable.rounded_battery_charging_60_24,
            intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Apps",
            subtitle = "Installed apps and permissions",
            iconRes = R.drawable.rounded_apps_24,
            intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Notifications",
            subtitle = "Notification history and alerts",
            iconRes = R.drawable.rounded_notifications_unread_24,
            intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Storage",
            subtitle = "Internal storage, cleanup",
            iconRes = R.drawable.rounded_save_24,
            intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
        PixelSearchResultItem.SystemSettingItem(
            title = "Security & privacy",
            subtitle = "Screen lock, app permissions",
            iconRes = R.drawable.rounded_security_24,
            intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        ),
    )

    return all.filter {
        it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
    }.take(4)
}

private fun launchFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            Toast.makeText(context, item.displayName, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openWithFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.pixel_search_action_open_with)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        launchFile(context, item)
    }
}

private fun shareFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.pixel_search_action_share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, item.displayName, Toast.LENGTH_SHORT).show()
    }
}

private fun openFileFolder(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val parentPath = item.path?.let { java.io.File(it).parent }
        if (parentPath != null) {
            val relativePath = parentPath.removePrefix("/storage/emulated/0/").removePrefix("/")
            val folderUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A" + Uri.encode(relativePath))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {
    }

    try {
        item.path?.let { p ->
            val parentFile = java.io.File(p).parentFile
            if (parentFile != null && parentFile.exists()) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(parentFile), "resource/folder")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }
    } catch (_: Exception) {
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = Uri.parse("content://com.android.externalstorage.documents/root/primary")
            setDataAndType(uri, "vnd.android.document/root")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        copyFilePath(context, item)
    }
}

private fun copyFilePath(context: Context, item: PixelSearchResultItem.FileItem) {
    val path = item.path ?: item.displayName
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("File Path", path)
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
}

private fun openAppInPlayStore(context: Context, packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun shareApp(context: Context, appName: String, packageName: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.pixel_search_action_share_app)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}

private fun loadShortcuts(context: Context, query: String): List<PixelSearchResultItem.ShortcutItem> {
    val items = mutableListOf<PixelSearchResultItem.ShortcutItem>()
    val q = query.lowercase()

    if ("wallpaper".contains(q) || "daily".contains(q) || "background".contains(q)) {
        items.add(
            PixelSearchResultItem.ShortcutItem(
                id = "shortcut_wallpaper",
                label = context.getString(R.string.feat_daily_wallpaper_title),
                subtitle = "Essentials wallpaper changer",
                iconRes = R.drawable.rounded_wallpaper_24,
                intent = Intent(context, WallpaperActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        )
    }

    if ("freeze".contains(q) || "app".contains(q) || "hibernate".contains(q)) {
        items.add(
            PixelSearchResultItem.ShortcutItem(
                id = "shortcut_freeze",
                label = "App Freezing",
                subtitle = "Freeze unused apps",
                iconRes = R.drawable.rounded_mode_cool_24,
                intent = Intent(context, AppFreezingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        )
    }

    return items
}

internal object MathEvaluator {
    private val PERCENT_OF_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*%\s*of\s*(\d+(?:\.\d+)?)$""", RegexOption.IGNORE_CASE)

    fun evaluate(expression: String): String? {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return null

        // Exclude inputs that are phone numbers or dates
        if (trimmed.startsWith('+') && !trimmed.drop(1).any { it in "+-*x×/÷^%" }) return null
        if (trimmed.matches(Regex("""^\d{2,4}-\d{1,4}-\d{1,4}$"""))) return null

        // Handle "X% of Y"
        val percentMatch = PERCENT_OF_REGEX.matchEntire(trimmed)
        if (percentMatch != null) {
            val (pStr, baseStr) = percentMatch.destructured
            val p = pStr.toDoubleOrNull() ?: return null
            val base = baseStr.toDoubleOrNull() ?: return null
            return formatResult((p / 100.0) * base)
        }

        // Must contain at least one math operator
        val hasOperator = trimmed.any { it in "+-*x×/÷^%" }
        if (!hasOperator) return null

        // Must contain at least one digit
        if (!trimmed.any { it.isDigit() }) return null

        val normalized = trimmed
            .replace('×', '*')
            .replace('x', '*')
            .replace('÷', '/')

        if (!normalized.all { it.isDigit() || it in ".+-*/%^() \t" }) return null

        return try {
            val result = Parser(normalized).parse()
            if (result.isNaN() || result.isInfinite()) null else formatResult(result)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatResult(value: Double): String {
        return if (value == kotlin.math.floor(value) && !value.isInfinite() && kotlin.math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
            val df = java.text.DecimalFormat("0.######", symbols)
            df.format(value)
        }
    }

    private class Parser(private val input: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ' || ch == '\t') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' ' || ch == '\t') nextChar()
            if (pos < input.length) throw IllegalArgumentException("Unexpected char: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+') -> x += parseTerm()
                    eat('-') -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*') -> x *= parseFactor()
                    eat('/') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%') -> {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Modulo by zero")
                        x %= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            while (ch == ' ' || ch == '\t') nextChar()
            when {
                eat('+') -> return parseFactor()
                eat('-') -> return -parseFactor()
            }

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                val numStr = input.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
            } else {
                throw IllegalArgumentException("Unexpected token: $ch")
            }

            if (eat('^')) x = Math.pow(x, parseFactor())

            return x
        }
    }
}

