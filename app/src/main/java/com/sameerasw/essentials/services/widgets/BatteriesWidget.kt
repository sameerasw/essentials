package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.os.BatteryManager
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.sameerasw.essentials.R

class BatteriesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                
                // Use ContextThemeWrapper to ensure we get the App's theme colors (Light/Dark aware)
                val themedContext = android.view.ContextThemeWrapper(context, R.style.Theme_Essentials)

                // Resolve Colors from Attributes
                val primaryColor = resolveColor(themedContext, android.R.attr.colorActivatedHighlight)
                val errorColor = resolveColor(themedContext, android.R.attr.colorError)
                val surfaceVariant = resolveColor(themedContext, android.R.attr.colorPressedHighlight)
                val surfaceColor = resolveColor(themedContext, android.R.attr.colorForeground)
                
                // Icon Tint: User preferred the previous primary color look
                val iconTint = primaryColor
                
                // Custom Warning Color (Amber)
                val warningColor = android.graphics.Color.parseColor("#FFC107")

                val ringColor = when {
                    batteryLevel <= 10 -> errorColor
                    batteryLevel < 20 -> warningColor
                    else -> primaryColor
                }
                
                val trackColor = ColorUtils.setAlphaComponent(surfaceVariant, 76) // ~30% alpha

                // Generate Bitmap
                val bitmapSize = 512
                val deviceIcon = ContextCompat.getDrawable(context, R.drawable.rounded_mobile_text_2_24)

                val bitmap = com.sameerasw.essentials.utils.BatteryRingDrawer.drawBatteryWidget(
                   context,
                   batteryLevel,
                   ringColor,
                   trackColor,
                   iconTint,
                   surfaceColor,
                   deviceIcon,
                   bitmapSize,
                   bitmapSize
                )

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                   Image(
                       provider = ImageProvider(bitmap),
                       contentDescription = "Battery Level $batteryLevel%",
                       modifier = GlanceModifier.fillMaxSize()
                   )
                }
            }
        }
    }

    private fun resolveColor(context: Context, @androidx.annotation.AttrRes attr: Int): Int {
        val typedValue = android.util.TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}