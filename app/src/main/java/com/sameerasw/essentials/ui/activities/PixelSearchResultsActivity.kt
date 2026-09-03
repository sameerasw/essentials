/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: PixelSearchResultsActivity.kt
 * Description: Activity displaying categorized search results from Pixel Launcher search bar.
 */

package com.sameerasw.essentials.ui.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.domain.registry.SearchRegistry
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    var query by remember { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }

    var appResults by remember { mutableStateOf<List<PixelSearchResultItem.AppItem>>(emptyList()) }
    var contactResults by remember { mutableStateOf<List<PixelSearchResultItem.ContactItem>>(emptyList()) }
    var systemSettingResults by remember { mutableStateOf<List<PixelSearchResultItem.SystemSettingItem>>(emptyList()) }
    var settingResults by remember { mutableStateOf<List<PixelSearchResultItem.SettingItem>>(emptyList()) }
    var shortcutResults by remember { mutableStateOf<List<PixelSearchResultItem.ShortcutItem>>(emptyList()) }

    val isAppsEnabled = remember { repository.isPixelSearchResultAppsEnabled() }
    val isContactsEnabled = remember { repository.isPixelSearchResultContactsEnabled() }
    val isSettingsEnabled = remember { repository.isPixelSearchResultSettingsEnabled() }
    val isShortcutsEnabled = remember { repository.isPixelSearchResultShortcutsEnabled() }
    val isWebEnabled = remember { repository.isPixelSearchResultWebEnabled() }

    fun performSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            appResults = emptyList()
            contactResults = emptyList()
            systemSettingResults = emptyList()
            settingResults = emptyList()
            shortcutResults = emptyList()
            return
        }

        scope.launch(Dispatchers.IO) {
            if (isAppsEnabled) {
                val installed = AppUtil.getInstalledApps(context)
                val filteredApps = installed
                    .filter { app ->
                        app.appName.contains(trimmed, ignoreCase = true) &&
                            context.packageManager.getLaunchIntentForPackage(app.packageName) != null
                    }
                    .take(6)
                    .map {
                        PixelSearchResultItem.AppItem(
                            appName = it.appName,
                            packageName = it.packageName,
                            icon = it.icon,
                            isSystemApp = it.isSystemApp,
                        )
                    }
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
                val mappedSettings = results.take(6).map {
                    PixelSearchResultItem.SettingItem(it)
                }
                withContext(Dispatchers.Main) {
                    systemSettingResults = systemSettings
                    settingResults = mappedSettings
                }
            }

            if (isShortcutsEnabled) {
                val shortcuts = loadShortcuts(context, trimmed)
                withContext(Dispatchers.Main) {
                    shortcutResults = shortcuts
                }
            }
        }
    }

    LaunchedEffect(query) {
        performSearch(query)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarHeightPx = with(LocalDensity.current) { statusBarHeight.toPx() }
    val bottomBlurHeightPx = with(LocalDensity.current) { 140.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .progressiveBlur(
                blurRadius = 40f,
                height = statusBarHeightPx * 1.2f,
                direction = BlurDirection.TOP,
            ),
    ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = statusBarHeight + 16.dp,
                    bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // APPS
                if (isAppsEnabled && appResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_apps))
                    }
                    item {
                        RoundedCardContainer {
                            appResults.forEach { app ->
                                FeatureCard(
                                    title = app.appName,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        launchApp(context, app.packageName)
                                        onFinish()
                                    },
                                    showToggle = false,
                                    hasMoreSettings = false,
                                    customTrailingContent = null,
                                    iconPainter = if (app.icon != null) {
                                        androidx.compose.ui.graphics.painter.BitmapPainter(app.icon)
                                    } else null,
                                    iconRes = if (app.icon == null) R.drawable.rounded_apps_24 else null,
                                )
                            }
                        }
                    }
                }

                // CONTACTS
                if (isContactsEnabled && contactResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_contacts))
                    }
                    item {
                        RoundedCardContainer {
                            contactResults.forEach { contact ->
                                ListItem(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        contact.phoneNumber?.let {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                            context.startActivity(dialIntent)
                                            onFinish()
                                        }
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
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
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
                if (isSettingsEnabled && systemSettingResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_system_settings))
                    }
                    item {
                        RoundedCardContainer {
                            systemSettingResults.forEach { setting ->
                                FeatureCard(
                                    title = setting.title,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        context.startActivity(setting.intent)
                                        onFinish()
                                    },
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
                if (isSettingsEnabled && settingResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_essentials))
                    }
                    item {
                        RoundedCardContainer {
                            settingResults.forEach { item ->
                                val setting = item.searchableItem
                                FeatureCard(
                                    title = setting.title,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                                            putExtra("feature", setting.featureKey)
                                            setting.targetSettingHighlightKey?.let {
                                                putExtra("highlight_setting", it)
                                            }
                                        }
                                        context.startActivity(intent)
                                        onFinish()
                                    },
                                    iconRes = setting.icon ?: R.drawable.rounded_settings_24,
                                    showToggle = false,
                                    hasMoreSettings = true,
                                    description = setting.description,
                                    isBeta = setting.isBeta,
                                )
                            }
                        }
                    }
                }

                // SHORTCUTS
                if (isShortcutsEnabled && shortcutResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_shortcuts))
                    }
                    item {
                        RoundedCardContainer {
                            shortcutResults.forEach { shortcut ->
                                FeatureCard(
                                    title = shortcut.label,
                                    isEnabled = true,
                                    onToggle = {},
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        context.startActivity(shortcut.intent)
                                        onFinish()
                                    },
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
                if (isWebEnabled && query.isNotBlank()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_web))
                    }
                    item {
                        RoundedCardContainer {
                            FeatureCard(
                                title = stringResource(R.string.pixel_search_web_search_action, query),
                                isEnabled = true,
                                onToggle = {},
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    launchWebSearch(context, query)
                                    onFinish()
                                },
                                iconRes = R.drawable.rounded_web_24,
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
                    leadingIcon = {
                        IconButton(onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onFinish()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_arrow_back_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
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
                            if (query.isNotBlank()) {
                                launchWebSearch(context, query)
                                onFinish()
                            }
                        }
                    ),
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
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

private fun launchWebSearch(context: Context, query: String) {
    try {
        val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (_: Exception) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
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
