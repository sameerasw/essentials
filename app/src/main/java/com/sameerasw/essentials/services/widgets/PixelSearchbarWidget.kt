package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.appwidget.cornerRadius
import androidx.glance.unit.ColorProvider
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PixelSearchbarWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsRepository = SettingsRepository(context)
        val type = settingsRepository.getPixelSearchbarType()
        val tapActionEnabled = settingsRepository.getPixelSearchbarTapActionEnabled()
        val paddingH = settingsRepository.getPixelSearchbarWidgetPaddingH().dp
        val paddingV = settingsRepository.getPixelSearchbarWidgetPaddingV().dp
        val revision = settingsRepository.getPixelSearchbarWidgetRevision()

        val musicBitmap = if (type == "music") {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(context.filesDir, "music_artwork.png")
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }.getOrNull()
            }
        } else null

        val globalTapAction = if (tapActionEnabled)
            actionStartActivity(com.sameerasw.essentials.ui.activities.PixelSearchbarTapActivity::class.java)
        else null

        provideContent {
            GlanceTheme {
                val boxModifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(horizontal = paddingH, vertical = paddingV)
                    .then(
                        if (globalTapAction != null) {
                            GlanceModifier.clickable(globalTapAction)
                        } else {
                            GlanceModifier
                        }
                    )

                when (type) {
                    "empty" -> {
                        Box(
                            modifier = boxModifier
                        ) {}
                    }

                    "widget" -> {
                        val remoteViews = WidgetScraperService.currentRemoteViews
                        Box(
                            modifier = boxModifier.cornerRadius(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (remoteViews != null) {
                                AndroidRemoteViews(
                                    remoteViews = remoteViews,
                                    modifier = GlanceModifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "—",
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = androidx.glance.text.FontFamily("google_sans_flex_round"),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }

                    "music" -> {
                        val title = settingsRepository.getPixelSearchbarMusicTitle()
                        val artist = settingsRepository.getPixelSearchbarMusicArtist()

                        if (title.isNotEmpty() || artist.isNotEmpty()) {
                            Box(
                                modifier = boxModifier
                                    .cornerRadius(28.dp)
                                    .background(GlanceTheme.colors.background)
                                    .clickable(actionRunCallback<MusicClickActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (musicBitmap != null) {
                                    Image(
                                        provider = ImageProvider(musicBitmap),
                                        contentDescription = null,
                                        modifier = GlanceModifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxSize()
                                            .background(Color(0, 0, 0, 120))
                                    ) {}
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = GlanceModifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = title,
                                        style = TextStyle(
                                            color = if (musicBitmap != null) {
                                                ColorProvider(Color.White)
                                            } else {
                                                GlanceTheme.colors.onSurface
                                            },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = androidx.glance.text.FontFamily("google_sans_flex_round"),
                                            textAlign = TextAlign.Center
                                        ),
                                        maxLines = 1
                                    )
                                    if (artist.isNotEmpty()) {
                                        Text(
                                            text = artist,
                                            style = TextStyle(
                                                color = if (musicBitmap != null) {
                                                    ColorProvider(Color.LightGray)
                                                } else {
                                                    GlanceTheme.colors.onSurfaceVariant
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                fontFamily = androidx.glance.text.FontFamily("google_sans_flex_round"),
                                                textAlign = TextAlign.Center
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = boxModifier
                            ) {}
                        }
                    }

                    else -> { // "date" (default)
                        val dateFormat = settingsRepository.getPixelSearchbarDateFormat()
                        val hasPill = settingsRepository.getPixelSearchbarBackgroundPill()
                        val dateStr = SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date())
                        Box(
                            modifier = boxModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasPill) {
                                Box(
                                    modifier = GlanceModifier
                                        .background(GlanceTheme.colors.background)
                                        .cornerRadius(28.dp)
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dateStr,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSecondaryContainer,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = androidx.glance.text.FontFamily("google_sans_flex_round"),
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = dateStr,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = androidx.glance.text.FontFamily("google_sans_flex_round"),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
