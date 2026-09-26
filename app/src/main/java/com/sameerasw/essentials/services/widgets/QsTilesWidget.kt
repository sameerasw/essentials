/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: QsTilesWidget.kt
 * Description: Background service component for QsTilesWidget.kt.
 */

package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.tiles.DataSimController
import com.sameerasw.essentials.services.tiles.DataSimTileService
import com.sameerasw.essentials.services.tiles.QsTileRegistry
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QsTilesWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    @RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        if (
            DataSimTileService::class.java.name in SettingsRepository(context).getPinnedQsTiles() &&
            PermissionUtils.hasReadPhoneStatePermission(context)
        ) {
            withContext(Dispatchers.IO) {
                try {
                    DataSimController.read(context)
                } catch (e: Exception) {
                    Log.w("QsTilesWidget", "Cannot refresh data SIM state", e)
                }
            }
        }
        provideContent {
            GlanceTheme {
                val prefs =
                    androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
                val KEY_UPDATE =
                    androidx.datastore.preferences.core
                        .longPreferencesKey("qs_widget_last_update")

                @Suppress("UNUSED_VARIABLE")
                val lastUpdate = prefs[KEY_UPDATE] ?: 0L

                val repository = SettingsRepository(context)
                val pinnedClassNames = repository.getPinnedQsTiles()

                val pinnedTiles =
                    pinnedClassNames.mapNotNull { QsTileRegistry.getTileByClassName(it) }
                val width = LocalSize.current.width
                val height = LocalSize.current.height

                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxSize(),
                ) {
                    if (pinnedTiles.isEmpty()) {
                        Column(
                            modifier =
                                GlanceModifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .cornerRadius(16.dp)
                                    .background(GlanceTheme.colors.primary)
                                    .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = context.getString(R.string.qs_tiles_widget_empty_state),
                                style =
                                    TextStyle(
                                        color = GlanceTheme.colors.onPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = FontFamily("google-sans-flex"),
                                        textAlign = TextAlign.Center,
                                    ),
                                modifier = GlanceModifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        val columnsCount = if (width >= 300.dp) 2 else 1
                        val targetRowHeight = 65.dp
                        val rowsCount = (height / targetRowHeight).toInt().coerceAtLeast(1)
                        val pageSize = columnsCount * rowsCount
                        val pages = pinnedTiles.chunked(pageSize)

                        val halfSpacing = 4.dp
                        val cellHeight = targetRowHeight

                        val scrollMode =
                            if (Build.VERSION.SDK_INT >= 37) {
                                VerticalScrollMode.SnapScrollMatchHeight(height)
                            } else {
                                VerticalScrollMode.Normal
                            }

                        LazyColumn(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalScrollMode = scrollMode,
                        ) {
                            items(pages) { pageTiles ->
                                Column(
                                    modifier = GlanceModifier.fillMaxSize().padding(halfSpacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    val rows = pageTiles.chunked(columnsCount)
                                    for (rowIndex in 0 until rowsCount) {
                                        val rowTiles = rows.getOrNull(rowIndex)
                                        if (rowTiles != null) {
                                            Row(
                                                modifier =
                                                    GlanceModifier
                                                        .fillMaxWidth()
                                                        .height(cellHeight),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                for (colIndex in 0 until columnsCount) {
                                                    val tile = rowTiles.getOrNull(colIndex)
                                                    if (tile != null) {
                                                        val resolvedTitle = QsTileRegistry.getTileLabel(
                                                            context,
                                                            tile.serviceClass.name
                                                        )
                                                        val pastelColor =
                                                            ColorUtil.getPastelColorFor(
                                                                resolvedTitle,
                                                            )
                                                        val vibrantColor =
                                                            ColorUtil.getVibrantColorFor(
                                                                resolvedTitle,
                                                            )

                                                        val isActive =
                                                            QsTileRegistry.isTileActive(
                                                                context,
                                                                tile.serviceClass.name,
                                                            )

                                                        val tileBg =
                                                            if (isActive) {
                                                                GlanceTheme.colors.primary
                                                            } else {
                                                                GlanceTheme.colors.widgetBackground
                                                            }
                                                        val tileTextColor =
                                                            if (isActive) {
                                                                GlanceTheme.colors.onPrimary
                                                            } else {
                                                                GlanceTheme.colors.onSurface
                                                            }
                                                        val tileSubtextColor =
                                                            if (isActive) {
                                                                GlanceTheme.colors.onPrimary
                                                            } else {
                                                                GlanceTheme.colors.onSurfaceVariant
                                                            }

                                                        val iconBoxSize = 38.dp
                                                        val iconCornerRadius = 10.dp
                                                        val iconSize = 22.dp
                                                        val fontSize = 13.sp
                                                        val spacerSize = 8.dp

                                                        Box(
                                                            modifier =
                                                                GlanceModifier
                                                                    .defaultWeight()
                                                                    .fillMaxHeight()
                                                                    .padding(
                                                                        horizontal = halfSpacing,
                                                                        vertical = halfSpacing,
                                                                    ),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            val cardModifier =
                                                                GlanceModifier
                                                                    .fillMaxSize()
                                                                    .cornerRadius(16.dp)
                                                                    .background(tileBg)
                                                                    .clickable(
                                                                        actionRunCallback<QsTileClickActionCallback>(
                                                                            actionParametersOf(
                                                                                QsTileClickActionCallback.SERVICE_CLASS_KEY to
                                                                                    tile.serviceClass.name,
                                                                            ),
                                                                        ),
                                                                    )

                                                            val activeIconRes =
                                                                QsTileRegistry.getTileIcon(
                                                                    context,
                                                                    tile.serviceClass.name,
                                                                    tile.iconRes,
                                                                )

                                                            Row(
                                                                modifier =
                                                                    cardModifier.padding(
                                                                        horizontal = 12.dp,
                                                                        vertical = 6.dp,
                                                                    ),
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                Box(
                                                                    modifier =
                                                                        GlanceModifier
                                                                            .size(iconBoxSize)
                                                                            .cornerRadius(
                                                                                iconCornerRadius,
                                                                            ).background(
                                                                                ColorProvider(
                                                                                    pastelColor,
                                                                                ),
                                                                            ),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Image(
                                                                        provider =
                                                                            ImageProvider(
                                                                                activeIconRes,
                                                                            ),
                                                                        contentDescription = resolvedTitle,
                                                                        colorFilter =
                                                                            ColorFilter.tint(
                                                                                ColorProvider(
                                                                                    vibrantColor,
                                                                                ),
                                                                            ),
                                                                        modifier =
                                                                            GlanceModifier.size(
                                                                                iconSize,
                                                                            ),
                                                                    )
                                                                }

                                                                Spacer(
                                                                    modifier =
                                                                        GlanceModifier.width(
                                                                            spacerSize,
                                                                        ),
                                                                )

                                                                val subtitle =
                                                                    QsTileRegistry.getTileSubtitle(
                                                                        context,
                                                                        tile.serviceClass.name,
                                                                    )

                                                                Column(
                                                                    modifier = GlanceModifier.defaultWeight(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                ) {
                                                                    Text(
                                                                        text = resolvedTitle,
                                                                        style =
                                                                            TextStyle(
                                                                                color = tileTextColor,
                                                                                fontSize = fontSize,
                                                                                fontWeight = FontWeight.Normal,
                                                                                fontFamily =
                                                                                    FontFamily(
                                                                                        "google-sans-flex",
                                                                                    ),
                                                                                textAlign = TextAlign.Start,
                                                                            ),
                                                                        maxLines = 1,
                                                                    )
                                                                    if (subtitle.isNotEmpty()) {
                                                                        Text(
                                                                            text = subtitle,
                                                                            style =
                                                                                TextStyle(
                                                                                    color = tileSubtextColor,
                                                                                    fontSize = 11.sp,
                                                                                    fontWeight = FontWeight.Normal,
                                                                                    fontFamily =
                                                                                        FontFamily(
                                                                                            "google-sans-flex",
                                                                                        ),
                                                                                    textAlign = TextAlign.Start,
                                                                                ),
                                                                            maxLines = 1,
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(
                                                            modifier =
                                                                GlanceModifier
                                                                    .defaultWeight()
                                                                    .fillMaxHeight()
                                                                    .padding(
                                                                        horizontal = halfSpacing,
                                                                        vertical = halfSpacing,
                                                                    ),
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(
                                                modifier =
                                                    GlanceModifier
                                                        .fillMaxWidth()
                                                        .height(cellHeight)
                                                        .padding(vertical = halfSpacing),
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
}
