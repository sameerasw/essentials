/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Widget Settings
 * File: WidgetPickerSheet.kt
 * Description: Picks a widget for a stack from every installed app, grouped by app.
 */

package com.sameerasw.essentials.ui.features.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.widgets.StackWidgetProvider
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private class WidgetApp(
    val label: String,
    val icon: ImageBitmap?,
    val widgets: List<AppWidgetProviderInfo>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val apps by produceState<List<WidgetApp>?>(null) {
        value = withContext(Dispatchers.IO) { loadWidgetApps(context) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.widget_picker_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.rounded_search_24), contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            val loaded = apps
            if (loaded == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val search = query.trim()
            val shown =
                if (search.isEmpty()) {
                    loaded
                } else {
                    loaded.mapNotNull { app ->
                        if (app.label.contains(search, ignoreCase = true)) return@mapNotNull app
                        val matching =
                            app.widgets.filter { it.loadLabel(context.packageManager).contains(search, ignoreCase = true) }
                        if (matching.isEmpty()) null else WidgetApp(app.label, app.icon, matching)
                    }
                }

            if (shown.isEmpty()) {
                Text(
                    text = stringResource(R.string.widget_picker_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
                )
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(shown, key = { it.widgets.first().provider.packageName }) { app ->
                    val key = app.widgets.first().provider.packageName
                    WidgetAppGroup(
                        app = app,
                        // Searching opens every matching app, as the launcher's picker does.
                        isExpanded = search.isNotEmpty() || key in expanded,
                        onToggle = { expanded = if (key in expanded) expanded - key else expanded + key },
                        onPick = onPick,
                        isFirst = app === shown.first(),
                        isLast = app === shown.last(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetAppGroup(
    app: WidgetApp,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onPick: (AppWidgetProviderInfo) -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val shape =
        RoundedCornerShape(
            topStart = if (isFirst) 24.dp else 4.dp,
            topEnd = if (isFirst) 24.dp else 4.dp,
            bottomStart = if (isLast) 24.dp else 4.dp,
            bottomEnd = if (isLast) 24.dp else 4.dp,
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceBright),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticUtil.performUIHaptic(view)
                        onToggle()
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (app.icon != null) {
                Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            } else {
                Icon(painterResource(R.drawable.rounded_widgets_24), contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = widgetCount(context, app.widgets.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = null,
                modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                app.widgets.forEach { info ->
                    WidgetOption(info = info, onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onPick(info)
                    })
                }
            }
        }
    }
}

@Composable
private fun WidgetOption(
    info: AppWidgetProviderInfo,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val label = remember(info) { info.loadLabel(context.packageManager) }
    val description =
        remember(info) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.loadDescription(context)?.toString() else null
        }
    val (cellsWide, cellsHigh) = remember(info) { cellSize(context, info) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetPreview(info)
        }
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(R.string.widget_picker_size, cellsWide, cellsHigh),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WidgetPreview(info: AppWidgetProviderInfo) {
    val context = LocalContext.current
    // A preview image is preferred: some apps only fill their preview layout once the widget is live,
    // so it draws blank here. Otherwise draw the layout at the widget's own size and scale it down
    // to fit, as the launcher does, rather than squeezing it into the preview box.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.previewLayout != 0 && info.previewImage == 0) {
        val (widthDp, heightDp) = remember(info) { previewSizeDp(context, info) }
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val scale = minOf(maxWidth.value / widthDp, maxHeight.value / heightDp, 1f)
            AndroidView(
                modifier =
                    Modifier
                        .requiredSize(widthDp.dp, heightDp.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                factory = { viewContext ->
                    FrameLayout(viewContext).apply {
                        try {
                            addView(
                                RemoteViews(info.provider.packageName, info.previewLayout).apply(viewContext, this),
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        } catch (e: Exception) {
                            Log.w("WidgetPicker", "Couldn't draw the preview of ${info.provider}", e)
                        }
                    }
                },
            )
        }
        return
    }

    val image =
        remember(info) {
            try {
                info.loadPreviewImage(context, 0)?.let { drawable ->
                    val width = drawable.intrinsicWidth.coerceAtLeast(1)
                    val height = drawable.intrinsicHeight.coerceAtLeast(1)
                    val scale = (600f / maxOf(width, height)).coerceAtMost(1f)
                    drawable.toBitmap((width * scale).roundToInt(), (height * scale).roundToInt()).asImageBitmap()
                }
            } catch (_: Exception) {
                null
            } ?: info.loadIcon(context, context.resources.displayMetrics.densityDpi)?.toBitmap(128, 128)?.asImageBitmap()
        }
    if (image != null) {
        Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Fit)
    }
}

private fun loadWidgetApps(context: Context): List<WidgetApp> {
    val pm = context.packageManager
    val ownStack = ComponentName(context, StackWidgetProvider::class.java)
    return AppWidgetManager
        .getInstance(context)
        .installedProviders
        .filter {
            it.provider != ownStack &&
                it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0
        }.groupBy { it.provider.packageName }
        .map { (packageName, widgets) ->
            val appInfo =
                try {
                    pm.getApplicationInfo(packageName, 0)
                } catch (_: Exception) {
                    null
                }
            WidgetApp(
                label = appInfo?.loadLabel(pm)?.toString() ?: packageName,
                icon = appInfo?.loadIcon(pm)?.toBitmap(128, 128)?.asImageBitmap(),
                widgets = widgets.sortedBy { it.loadLabel(pm).lowercase() },
            )
        }.sortedBy { it.label.lowercase() }
}

/** Roughly the size the widget would have on the homescreen, for drawing its preview. */
private fun previewSizeDp(
    context: Context,
    info: AppWidgetProviderInfo,
): Pair<Float, Float> {
    val density = context.resources.displayMetrics.density
    val (cellsWide, cellsHigh) = cellSize(context, info)
    val width = maxOf(info.minWidth / density, cellsWide * CELL_WIDTH_DP)
    val height = maxOf(info.minHeight / density, cellsHigh * CELL_HEIGHT_DP)
    return width to height
}

// About the size of one cell on a phone's 4-column homescreen.
private const val CELL_WIDTH_DP = 90f
private const val CELL_HEIGHT_DP = 100f

/** The widget's size in homescreen cells, from its own target size when it gives one. */
private fun cellSize(
    context: Context,
    info: AppWidgetProviderInfo,
): Pair<Int, Int> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
        return info.targetCellWidth to info.targetCellHeight
    }
    // Android's documented sizing: a widget n cells across needs about 70n - 30 dp.
    val density = context.resources.displayMetrics.density
    fun cells(px: Int) = (((px / density) + 30) / 70).roundToInt().coerceAtLeast(1)
    return cells(info.minWidth) to cells(info.minHeight)
}

private fun widgetCount(
    context: Context,
    count: Int,
): String =
    if (count == 1) {
        context.getString(R.string.widget_stack_count_one)
    } else {
        context.getString(R.string.widget_stack_count_many, count)
    }
