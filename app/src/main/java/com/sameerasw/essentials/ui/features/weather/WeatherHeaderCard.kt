package com.sameerasw.essentials.ui.features.weather

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.activities.WeatherDetailActivity
import com.sameerasw.essentials.weather.WeatherFormat
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.effects.WeatherEffects
import com.sameerasw.essentials.weather.location.DeviceLocationSource
import kotlin.math.roundToInt

@Composable
fun WeatherHeaderCard(
    height: Dp,
    minHeight: Dp,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by WeatherRepository.state.collectAsState()
    val presentation = rememberWeatherPresentation(state.snapshot)
    val snapshot = presentation.snapshot
    val palette = presentation.palette

    LaunchedEffect(Unit) {
        WeatherRepository.ensureLoaded(context)
        val settings = SettingsRepository(context)
        val canLocate = settings.getWeatherLocationMode() == "manual" || DeviceLocationSource.hasPermission(context)
        if (canLocate && WeatherRepository.isStale(context)) WeatherRepository.refresh(context)
    }

    val expansion = ((height - minHeight) / (maxHeight - minHeight)).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Brush.verticalGradient(0f to palette.glow, 0.55f to palette.glowSecondary, 1f to palette.base))
            .clickable {
                context.startActivity(Intent(context, WeatherDetailActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!presentation.effectSpec.isEmpty) {
            WeatherEffects(spec = presentation.effectSpec, modifier = Modifier.matchParentSize(), strength = 1.5f, haptics = presentation.haptics)
        }
        if (snapshot == null) {
            if (state.loading) {
                LoadingIndicator()
            } else {
                Icon(painterResource(R.drawable.rounded_cloud_24), null, tint = palette.onBaseMuted, modifier = Modifier.size(48.dp))
            }
        } else {
            val number = WeatherFormat.temperature(snapshot.tempC, presentation.unit).removeSuffix("\u00b0")
            val digitSize = lerp(if (number.length >= 3) 70f else 84f, if (number.length >= 3) 120f else 150f, expansion).sp
            val font = temperatureFont(lerp(125f, 52f, expansion).roundToInt(), lerp(400f, 60f, expansion).roundToInt())
            val place = listOf(snapshot.locationName, snapshot.region).filter { it.isNotBlank() }.joinToString(", ")
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (place.isNotBlank()) {
                    Row(
                        Modifier
                            .foldAway((1f - (expansion - 0.3f) / 0.5f).coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(palette.card)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painterResource(R.drawable.rounded_location_on_24), null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Text(place, color = palette.onBase, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("\u00b0", color = androidx.compose.ui.graphics.Color.Transparent, fontFamily = font, fontSize = digitSize * 0.42f, lineHeight = digitSize * 0.42f, maxLines = 1, softWrap = false)
                    Text(
                        number,
                        color = palette.onBase,
                        fontFamily = font,
                        fontSize = digitSize,
                        lineHeight = digitSize * 0.86f,
                        letterSpacing = lerp(-1f, -10f, expansion).sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text("\u00b0", color = palette.onBase, fontFamily = font, fontSize = digitSize * 0.42f, lineHeight = digitSize * 0.42f, maxLines = 1, softWrap = false)
                }
                Row(
                    Modifier.graphicsLayer { alpha = ((expansion - 0.15f) / 0.6f).coerceIn(0f, 1f) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(WeatherFormat.icon(snapshot.condition, snapshot.isDay)), null, tint = palette.accent, modifier = Modifier.size(24.dp))
                    Text(snapshot.conditionText, color = palette.onBase, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
