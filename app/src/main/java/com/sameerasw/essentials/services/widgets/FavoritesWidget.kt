package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
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
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.registry.FeatureRegistry
import com.sameerasw.essentials.utils.ColorUtil

class FavoritesWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    @RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val repository = SettingsRepository(context)
                val pinnedKeys = repository.getPinnedFeatures()

                val featuresMap = FeatureRegistry.ALL_FEATURES.associateBy { it.id }
                val pinnedFeatures = pinnedKeys.mapNotNull { featuresMap[it] }
                val width = LocalSize.current.width
                val height = LocalSize.current.height

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primary)
                ) {
                    if (pinnedFeatures.isEmpty()) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .cornerRadius(16.dp)
                                .background(GlanceTheme.colors.widgetBackground)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.favorites_widget_empty_state),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily("google-sans-flex"),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = GlanceModifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val columnsCount = if (width >= 300.dp) 2 else 1
                        val rowsCount = if (height >= 130.dp) 2 else 1
                        val pageSize = columnsCount * rowsCount
                        val pages = pinnedFeatures.chunked(pageSize)

                        val halfSpacing = 4.dp
                        val cellWidth = width / columnsCount.toFloat()
                        val cellHeight = height / rowsCount.toFloat()

                        LazyColumn(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalScrollMode = VerticalScrollMode.SnapScrollMatchHeight(height)
                        ) {
                            items(pages) { pageFeatures ->
                                Column(
                                    modifier = GlanceModifier.fillMaxSize().padding(halfSpacing),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val rows = pageFeatures.chunked(columnsCount)
                                    for (rowIndex in 0 until rowsCount) {
                                        val rowFeatures = rows.getOrNull(rowIndex)
                                        if (rowFeatures != null) {
                                            Row(
                                                modifier = GlanceModifier.fillMaxWidth().height(cellHeight),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                for (colIndex in 0 until columnsCount) {
                                                    val feature = rowFeatures.getOrNull(colIndex)
                                                    if (feature != null) {
                                                        val resolvedTitle = context.getString(feature.title)
                                                        val pastelColor = ColorUtil.getPastelColorFor(resolvedTitle)
                                                        val vibrantColor = ColorUtil.getVibrantColorFor(resolvedTitle)

                                                        val intent = Intent(context, FeatureSettingsActivity::class.java).apply {
                                                            putExtra("feature", feature.id)
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }

                                                        val isVertical = cellHeight >= (cellWidth - 40.dp)
                                                        val isSmall = cellHeight < 90.dp
                                                        val iconBoxSize = if (isSmall) 36.dp else 48.dp
                                                        val iconCornerRadius = if (isSmall) 8.dp else 12.dp
                                                        val iconSize = if (isSmall) 22.dp else 28.dp
                                                        val fontSize = if (isSmall) 12.sp else 15.sp
                                                        val spacerSize = if (isSmall) 4.dp else 8.dp

                                                        Box(
                                                            modifier = GlanceModifier
                                                                .defaultWeight()
                                                                .fillMaxHeight()
                                                                .padding(horizontal = halfSpacing, vertical = halfSpacing),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            val cardModifier = GlanceModifier
                                                                .fillMaxSize()
                                                                .cornerRadius(16.dp)
                                                                .background(GlanceTheme.colors.widgetBackground)
                                                                .clickable(actionStartActivity(intent))

                                                            if (isVertical) {
                                                                Column(
                                                                    modifier = cardModifier.padding(8.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(
                                                                        modifier = GlanceModifier
                                                                            .size(iconBoxSize)
                                                                            .cornerRadius(iconCornerRadius)
                                                                            .background(pastelColor),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Image(
                                                                            provider = ImageProvider(feature.iconRes),
                                                                            contentDescription = resolvedTitle,
                                                                            colorFilter = ColorFilter.tint(ColorProvider(vibrantColor)),
                                                                            modifier = GlanceModifier.size(iconSize)
                                                                        )
                                                                    }

                                                                    Spacer(modifier = GlanceModifier.height(spacerSize))

                                                                    Text(
                                                                        text = resolvedTitle,
                                                                        style = TextStyle(
                                                                            color = GlanceTheme.colors.onSurface,
                                                                            fontSize = fontSize,
                                                                            fontWeight = FontWeight.Normal,
                                                                            fontFamily = FontFamily("google-sans-flex"),
                                                                            textAlign = TextAlign.Center
                                                                        )
                                                                    )
                                                                }
                                                            } else {
                                                                Row(
                                                                    modifier = cardModifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(
                                                                        modifier = GlanceModifier
                                                                            .size(iconBoxSize)
                                                                            .cornerRadius(iconCornerRadius)
                                                                            .background(pastelColor),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Image(
                                                                            provider = ImageProvider(feature.iconRes),
                                                                            contentDescription = resolvedTitle,
                                                                            colorFilter = ColorFilter.tint(ColorProvider(vibrantColor)),
                                                                            modifier = GlanceModifier.size(iconSize)
                                                                        )
                                                                    }

                                                                    Spacer(modifier = GlanceModifier.width(spacerSize))

                                                                    Text(
                                                                        text = resolvedTitle,
                                                                        style = TextStyle(
                                                                            color = GlanceTheme.colors.onSurface,
                                                                            fontSize = fontSize,
                                                                            fontWeight = FontWeight.Normal,
                                                                            fontFamily = FontFamily("google-sans-flex"),
                                                                            textAlign = TextAlign.Start
                                                                        ),
                                                                        modifier = GlanceModifier.defaultWeight()
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(
                                                            modifier = GlanceModifier
                                                                .defaultWeight()
                                                                .fillMaxHeight()
                                                                .padding(horizontal = halfSpacing, vertical = halfSpacing)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(
                                                modifier = GlanceModifier
                                                    .fillMaxWidth()
                                                    .defaultWeight()
                                                    .padding(vertical = halfSpacing)
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
