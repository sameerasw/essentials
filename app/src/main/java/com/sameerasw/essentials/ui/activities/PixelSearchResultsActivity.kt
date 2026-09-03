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
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.domain.model.SearchableItem
import com.sameerasw.essentials.domain.registry.SearchRegistry
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PixelSearchResultsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            settingResults = emptyList()
            shortcutResults = emptyList()
            return
        }

        scope.launch(Dispatchers.IO) {
            // Apps
            if (isAppsEnabled) {
                val installed = AppUtil.getInstalledApps(context)
                val filteredApps = installed
                    .filter { it.appName.contains(trimmed, ignoreCase = true) }
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

            // Contacts
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

            // Settings
            if (isSettingsEnabled) {
                val results = SearchRegistry.search(context, trimmed, repository.isEnableUnsupportedFeatures())
                val mappedSettings = results.take(5).map {
                    PixelSearchResultItem.SettingItem(it)
                }
                withContext(Dispatchers.Main) {
                    settingResults = mappedSettings
                }
            }

            // Shortcuts
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight, bottom = navBarHeight),
        ) {
            // Search Input Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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

            // Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. APPS SECTION (Top Priority)
                if (isAppsEnabled && appResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_apps))
                    }
                    item {
                        RoundedCardContainer {
                            appResults.forEach { app ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingContent = {
                                        if (app.icon != null) {
                                            Image(
                                                bitmap = app.icon,
                                                contentDescription = app.appName,
                                                modifier = Modifier.size(36.dp),
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.rounded_apps_24),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            launchApp(context, app.packageName)
                                            onFinish()
                                        },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    ),
                                )
                            }
                        }
                    }
                }

                // 2. CONTACTS SECTION
                if (isContactsEnabled && contactResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_contacts))
                    }
                    item {
                        RoundedCardContainer {
                            contactResults.forEach { contact ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                        )
                                    },
                                    supportingContent = {
                                        contact.phoneNumber?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        contact.phoneNumber?.let { num ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                                    context.startActivity(smsIntent)
                                                    onFinish()
                                                }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.rounded_send_24),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            contact.phoneNumber?.let {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                                                context.startActivity(dialIntent)
                                                onFinish()
                                            }
                                        },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    ),
                                )
                            }
                        }
                    }
                }

                // 3. ESSENTIALS SETTINGS SECTION
                if (isSettingsEnabled && settingResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_settings))
                    }
                    item {
                        RoundedCardContainer {
                            settingResults.forEach { item ->
                                val setting = item.searchableItem
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = setting.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = setting.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingContent = {
                                        val iconRes = setting.icon ?: R.drawable.rounded_settings_24
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_chevron_right_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
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
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    ),
                                )
                            }
                        }
                    }
                }

                // 4. SHORTCUTS SECTION
                if (isShortcutsEnabled && shortcutResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_shortcuts))
                    }
                    item {
                        RoundedCardContainer {
                            shortcutResults.forEach { shortcut ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = shortcut.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = shortcut.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(shortcut.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.rounded_open_in_new_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            context.startActivity(shortcut.intent)
                                            onFinish()
                                        },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                                    ),
                                )
                            }
                        }
                    }
                }

                // 5. WEB SEARCH FALLBACK (Always available when query is typed)
                if (isWebEnabled && query.isNotBlank()) {
                    item {
                        SearchSectionHeader(stringResource(R.string.pixel_search_section_web))
                    }
                    item {
                        RoundedCardContainer {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = stringResource(R.string.pixel_search_web_search_action, query),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_web_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_open_in_browser_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        launchWebSearch(context, query)
                                        onFinish()
                                    },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ),
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
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

private fun loadShortcuts(context: Context, query: String): List<PixelSearchResultItem.ShortcutItem> {
    val items = mutableListOf<PixelSearchResultItem.ShortcutItem>()
    val q = query.lowercase()

    // Wallpaper Shortcut
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

    // App Freezing Shortcut
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
