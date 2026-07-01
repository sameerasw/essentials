package com.sameerasw.essentials.services.widgets

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PixelSearchbarWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settingsRepository = SettingsRepository(context)

        provideContent {
            GlanceTheme {
                val type = settingsRepository.getPixelSearchbarType()

                when (type) {
                    "empty" -> {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(android.graphics.Color.TRANSPARENT)
                        ) {}
                    }
                    else -> { // Default / "date"
                        val dateStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(android.graphics.Color.TRANSPARENT)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateStr,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
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
