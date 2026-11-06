package com.sameerasw.essentials.island.plugins.travel

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.services.LocationReachedService
import com.sameerasw.essentials.ui.activities.TravelCompassActivity

class TravelPlugin : BaseIslandPlugin() {
    override val id = "travel"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_TRAVEL,
        KEY_ACTIVE,
        KEY_NAME,
        KEY_PROGRESS,
        KEY_TIME,
        KEY_DISTANCE,
        KEY_ICON,
        KEY_PAUSED,
        KEY_ARRIVED,
    )

    private class Trip(
        val name: String,
        val progress: Float,
        val time: String,
        val distance: String,
        val iconRes: Int,
        val paused: Boolean,
    )

    override fun refresh() {
        ctx ?: return
        val trip = readTrip()
        if (trip == null || !settings.isIslandShowTravelEnabled()) {
            publish(null)
            return
        }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.TRAVEL,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("travel.icon") { IslandIcon(trip.iconRes, size = 18.dp, tint = islandAccent()) },
                    CompactCell("travel.distance") { RollingText(trip.distance) },
                ),
                line = LineContent(
                    icon = { IslandIcon(trip.iconRes, tint = islandAccent()) },
                    start = trip.name,
                    end = trip.distance,
                ),
                expanded = ExpandedContent { scope ->
                    TravelExpanded(
                        trip = trip,
                        onTogglePause = { sendAction(if (trip.paused) LocationReachedService.ACTION_RESUME else LocationReachedService.ACTION_PAUSE) },
                        onStop = { sendAction(LocationReachedService.ACTION_STOP) },
                        scope = scope,
                    )
                },
                onOpen = ::open,
            ),
        )
    }

    private fun readTrip(): Trip? {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false) || prefs.getBoolean(KEY_ARRIVED, false)) return null
        val iconName = prefs.getString(KEY_ICON, null) ?: DEFAULT_ICON
        val iconRes = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.round_navigation_24
        return Trip(
            name = prefs.getString(KEY_NAME, null).orEmpty(),
            progress = prefs.getFloat(KEY_PROGRESS, 0f).coerceIn(0f, 1f),
            time = prefs.getString(KEY_TIME, null).orEmpty(),
            distance = prefs.getString(KEY_DISTANCE, null).orEmpty(),
            iconRes = iconRes,
            paused = prefs.getBoolean(KEY_PAUSED, false),
        )
    }

    private fun sendAction(action: String) {
        try {
            context.startService(Intent(context, LocationReachedService::class.java).setAction(action))
        } catch (_: Exception) {
        }
    }

    private fun open() {
        try {
            context.startActivity(Intent(context, TravelCompassActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun TravelExpanded(trip: Trip, onTogglePause: () -> Unit, onStop: () -> Unit, scope: IslandExpandedScope) {
        val spec = scope.spec
        val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
        val accent = islandAccent()
        Box(propagateMinConstraints = true) {
            Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        IslandIcon(trip.iconRes, size = 20.dp, tint = accent)
                        MarqueeText(text = trip.name, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = { RollingText(trip.distance) },
                )
                if (trip.time.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = trip.time,
                        style = IslandTextStyles.body.copy(color = islandTextColor(), fontSize = 15.sp, lineHeight = 20.sp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                    )
                }
                Spacer(Modifier.height(16.dp))
                LinearWavyProgressIndicator(
                    progress = { trip.progress },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    amplitude = { if (trip.paused) 0f else 1f },
                )
                Spacer(Modifier.height(16.dp))
                ConnectedButtonRow(
                    height = 44.dp,
                    modifier = Modifier.padding(horizontal = sidePadding).padding(bottom = spec.expandedBottomPadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = listOf(
                        ConnectedItem({ onTogglePause() }) {
                            ConnectedTextLabel(stringResource(if (trip.paused) R.string.island_travel_resume else R.string.island_travel_pause))
                        },
                        ConnectedItem({
                            onStop()
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.action_stop)) },
                        ConnectedItem({
                            open()
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.island_progress_open)) },
                    ),
                )
            }
        }
    }

    companion object {
        const val ITEM_KEY = "travel"
        private const val DEFAULT_ICON = "round_navigation_24"
        private const val KEY_ACTIVE = "travel_active"
        private const val KEY_NAME = "travel_name"
        private const val KEY_PROGRESS = "travel_progress"
        private const val KEY_TIME = "travel_remaining_time"
        private const val KEY_DISTANCE = "travel_remaining_distance"
        private const val KEY_ICON = "travel_icon_name"
        private const val KEY_PAUSED = "travel_is_paused"
        private const val KEY_ARRIVED = "travel_arrived"
    }
}
