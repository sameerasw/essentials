package com.sameerasw.essentials.island.plugins.consciousgate

import com.sameerasw.essentials.island.ui.components.MarqueeText
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.RollingText
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import java.util.Locale

class ConsciousGatePlugin : BaseIslandPlugin() {
    override val id = "conscious_gate"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_CONSCIOUS_GATE)

    private var cachedPkg: String? = null
    private var cachedIcon: Bitmap? = null
    private var lastHapticSecond = -1L
    private val tick = Runnable { refresh() }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(tick)
        lastHapticSecond = -1L
    }

    override fun refresh() {
        val c = ctx ?: return
        c.mainHandler.removeCallbacks(tick)
        val session = if (settings.isIslandShowConsciousGateEnabled()) {
            ScreenOffAccessibilityService.instance?.getActiveConsciousGateSession()
        } else {
            null
        }
        val remaining = session?.let {
            (it.durationMillis - (System.currentTimeMillis() - it.startTimeMillis).coerceAtLeast(0L)).coerceAtLeast(0L)
        } ?: 0L
        if (session == null || remaining <= 0L) {
            lastHapticSecond = -1L
            publish(null)
            return
        }

        val totalSec = remaining / 1000L
        val timer = String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60L, totalSec % 60L)
        if (cachedPkg != session.packageName || cachedIcon == null) {
            cachedPkg = session.packageName
            cachedIcon = try {
                AppUtil.drawableToBitmap(context.packageManager.getApplicationIcon(session.packageName))
            } catch (_: Exception) {
                null
            }
        }
        val icon = cachedIcon
        val label = context.getString(R.string.conscious_gate_island_wasting_time)
        val pkg = session.packageName

        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.CONSCIOUS_GATE,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("gate.icon") { IslandBitmap(icon, 20.dp, circle = true) },
                    CompactCell("gate.timer") { RollingText(timer) },
                ),
                line = LineContent(icon = { IslandBitmap(icon, 24.dp, circle = true) }, start = label, end = timer),
                expanded = ExpandedContent { scope ->
                    Column(Modifier.padding(top = scope.spec.expandedTopPadding, bottom = scope.spec.expandedPadding)) {
                        scope.CameraRow(
                            start = {
                                IslandBitmap(icon, scope.spec.cellSize, circle = true)
                                MarqueeText(text = label, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                            },
                        )
                        RollingText(
                            text = timer,
                            style = IslandTextStyles.compact.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = scope.spec.expandedPadding + scope.spec.expandedCorner * 0.35f),
                        )
                    }
                },
                onOpen = { launchPackage(context, pkg) },
            ),
        )

        if (lastHapticSecond != totalSec) {
            lastHapticSecond = totalSec
            val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("conscious_gate_feel_every_second", false)) {
                HapticUtil.performHapticForService(context, HapticFeedbackType.SUBTLE)
            }
        }
        val delay = (remaining % 1000L).let { if (it == 0L) 1000L else it }.coerceIn(50L, 1000L)
        c.mainHandler.postDelayed(tick, delay)
    }

    companion object {
        const val ITEM_KEY = "conscious_gate"
    }
}
