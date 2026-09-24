package com.sameerasw.essentials.island.plugins.weather

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.weather.WeatherFormat
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.model.TemperatureUnit
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherError
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherExpanded(
    unit: TemperatureUnit,
    scope: IslandExpandedScope,
    onRefresh: () -> Unit,
) {
    val state by WeatherRepository.state.collectAsState()
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val snapshot = state.snapshot
    val accent = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth().padding(spec.expandedOutset).padding(bottom = spec.expandedPadding)) {
        Spacer(Modifier.height(spec.expandedTopPadding))
        scope.CameraRow(
            horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
            start = {
                IslandIcon(
                    snapshot?.let { WeatherFormat.icon(it.condition, it.isDay) } ?: R.drawable.rounded_cloud_24,
                    tint = accent,
                    size = 22.dp,
                )
                Spacer(Modifier.width(8.dp))
                MarqueeText(
                    text = snapshot?.locationName ?: stringResource(R.string.lock_screen_clock_weather),
                    style = IslandTextStyles.title,
                    modifier = Modifier.weight(1f),
                )
            },
            end = {
                snapshot?.let { Text(WeatherFormat.temperature(it.tempC, unit), style = IslandTextStyles.title.copy(fontSize = 22.sp)) }
            },
        )
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (snapshot == null) {
                if (state.loading) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { LoadingIndicator() }
                } else {
                    Text(errorText(state.error), style = IslandTextStyles.body)
                }
            } else {
                Text(
                    text = listOf(
                        snapshot.conditionText,
                        stringResource(
                            R.string.weather_high_low,
                            WeatherFormat.temperature(snapshot.highC, unit),
                            WeatherFormat.temperature(snapshot.lowC, unit),
                        ),
                        stringResource(R.string.weather_feels_like, WeatherFormat.temperature(snapshot.feelsLikeC, unit)),
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = IslandTextStyles.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                snapshot.activeAlerts().maxByOrNull { it.severity.ordinal }?.let { WeatherAlertCard(it) }
                HourlyStrip(snapshot, unit)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailChip(R.drawable.rounded_water_drop_24, "${snapshot.humidity}%", Modifier.weight(1f))
                    DetailChip(R.drawable.rounded_air_24, WeatherFormat.wind(snapshot.windKph, unit), Modifier.weight(1f))
                    DetailChip(R.drawable.rounded_rainy_24, "${snapshot.chanceOfRain}%", Modifier.weight(1f))
                }
                UpdatedRow(snapshot, state.loading, state.error, scope, onRefresh)
            }
        }
    }
}

@Composable
private fun errorText(error: WeatherError?): String = stringResource(
    when (error) {
        WeatherError.MissingApiKey -> R.string.weather_error_missing_key
        WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
        WeatherError.LocationPermission -> R.string.weather_error_location_permission
        WeatherError.NoLocation -> R.string.weather_error_no_location
        WeatherError.Network -> R.string.weather_error_network
        is WeatherError.Unknown, null -> R.string.weather_error_unknown
    },
)

@Composable
private fun WeatherAlertCard(alert: WeatherAlert) {
    val context = LocalContext.current
    val alertColor = MaterialTheme.colorScheme.error
    val until = alert.expiresMillis?.let { stringResource(R.string.weather_alert_until, formatTime(context, it)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(alertColor.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IslandIcon(R.drawable.rounded_warning_24, tint = alertColor, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                alert.event,
                style = IslandTextStyles.body.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(until, alert.headline.takeIf { it != alert.event }).joinToString(" · "),
                style = IslandTextStyles.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HourlyStrip(snapshot: WeatherSnapshot, unit: TemperatureUnit) {
    val context = LocalContext.current
    val hours = snapshot.hourly.take(6)
    if (hours.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        hours.forEach { hour ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatHour(context, hour.timeMillis), style = IslandTextStyles.body.copy(fontSize = 11.sp))
                IslandIcon(WeatherFormat.icon(hour.condition, hour.isDay), size = 18.dp)
                Text(WeatherFormat.temperature(hour.tempC, unit), style = IslandTextStyles.body.copy(color = Color.White, fontSize = 13.sp))
            }
        }
    }
}

@Composable
private fun DetailChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IslandIcon(icon, tint = MaterialTheme.colorScheme.primary, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, style = IslandTextStyles.body.copy(color = Color.White, fontSize = 12.sp), maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdatedRow(
    snapshot: WeatherSnapshot,
    loading: Boolean,
    error: WeatherError?,
    scope: IslandExpandedScope,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (error != null) errorText(error) else stringResource(R.string.weather_updated_at, formatTime(context, snapshot.updatedAt)),
            style = IslandTextStyles.body.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (loading) {
            LoadingIndicator(Modifier.size(28.dp))
        } else {
            IslandIcon(
                R.drawable.rounded_refresh_24,
                size = 18.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        IslandHaptics.button(context)
                        scope.keepAlive()
                        onRefresh()
                    }
                    .padding(6.dp),
            )
        }
    }
}

private fun formatTime(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}
