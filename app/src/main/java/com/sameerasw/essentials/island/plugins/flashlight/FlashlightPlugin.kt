package com.sameerasw.essentials.island.plugins.flashlight

import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.InteractionOverrides
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.utils.FlashlightUtil
import com.sameerasw.essentials.island.ui.IslandHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FlashlightPlugin : BaseIslandPlugin() {
    override val id = "flashlight"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_FLASHLIGHT)

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private var cameraId: String? = null
    private var torchOn = false
    private var percent = 100
    private var fadeJob: Job? = null

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            torchOn = enabled
            refresh()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onTorchStrengthLevelChanged(id: String, level: Int) {
            if (id != cameraId) return
            percent = toPercent(level)
            render()
        }
    }

    override fun onStart() {
        cameraId = FlashlightUtil.getCameraId(context)
        try {
            cameraManager.registerTorchCallback(callback, ctx!!.mainHandler)
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        fadeJob?.cancel()
        try {
            cameraManager.unregisterTorchCallback(callback)
        } catch (_: Exception) {
        }
    }

    override fun refresh() {
        val id = cameraId
        if (ctx != null && id != null && torchOn) percent = toPercent(FlashlightUtil.getCurrentLevel(context, id))
        render()
    }

    private fun maxLevel(): Int = cameraId?.let { FlashlightUtil.getMaxLevel(context, it) } ?: 1

    private fun toPercent(level: Int): Int = (level * 100f / maxLevel().coerceAtLeast(1)).toInt().coerceIn(1, 100)

    private fun render() {
        if (ctx == null || !torchOn || cameraId == null || !settings.isIslandShowFlashlightEnabled()) {
            publish(null)
            return
        }
        val levels = maxLevel() > 1
        val value = percent
        val percentText = "$value%"
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.FLASHLIGHT,
                placement = CompactPlacement.Dynamic,
                compact = if (levels) {
                    listOf(
                        CompactCell("flash.icon") { IslandIcon(R.drawable.rounded_flashlight_on_24, size = 18.dp, tint = MaterialTheme.colorScheme.primary) },
                        CompactCell("flash.level") { RollingText(percentText) },
                    )
                } else {
                    listOf(CompactCell("flash.icon") { IslandIcon(R.drawable.rounded_flashlight_on_24, size = 18.dp, tint = MaterialTheme.colorScheme.primary) })
                },
                line = LineContent(icon = { IslandIcon(R.drawable.rounded_flashlight_on_24, tint = MaterialTheme.colorScheme.primary) }, start = "", end = percentText),
                expanded = ExpandedContent { scope ->
                    Column(Modifier.padding(scope.spec.expandedOutset).padding(top = scope.spec.expandedTopPadding, bottom = scope.spec.expandedPadding * 0.7f)) {
                        scope.CameraRow(
                            horizontalPadding = 20.dp,
                            start = { IslandIcon(R.drawable.rounded_flashlight_on_24, size = 20.dp, tint = MaterialTheme.colorScheme.primary) },
                            end = { RollingText(percentText) },
                        )
                        if (levels) {
                            Spacer(Modifier.height(6.dp))
                            LevelPill(
                                initial = value / 100f,
                                height = scope.spec.cellSize,
                                onChange = ::setFraction,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        ConnectedButtonRow(
                            height = 36.dp,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            container = Color.White.copy(alpha = 0.2f),
                            items = listOf(
                                ConnectedItem({
                                    turnOff()
                                    scope.collapse()
                                }) { ConnectedTextLabel(stringResource(R.string.action_turn_off)) },
                            ),
                        )
                    }
                },
                dismissible = true,
                onDismiss = { turnOff() },
                interactions = InteractionOverrides(onLongPress = { turnOff() }),
            ),
        )
    }

    private fun setFraction(fraction: Float) {
        val id = cameraId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        fadeJob?.cancel()
        val max = maxLevel()
        try {
            cameraManager.turnOnTorchWithStrengthLevel(id, (fraction * max).toInt().coerceIn(1, max))
        } catch (_: Exception) {
        }
    }

    private fun turnOff() {
        val id = cameraId ?: return
        val fade = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE).getBoolean("flashlight_fade_enabled", false)
        if (fade && FlashlightUtil.isIntensitySupported(context, id)) {
            fadeJob?.cancel()
            fadeJob = ctx?.scope?.launch { FlashlightUtil.fadeFlashlight(context, id, targetOn = false) }
            return
        }
        try {
            cameraManager.setTorchMode(id, false)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ITEM_KEY = "flashlight"
    }
}

// Drag or tap anywhere on the pill to set the torch level, like the old canvas slider.
@Composable
private fun LevelPill(initial: Float, height: Dp, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var level by remember { mutableFloatStateOf(initial.coerceIn(0.01f, 1f)) }
    var lastHapticStep by remember { mutableIntStateOf((level * 10).toInt()) }
    fun update(x: Float, width: Int) {
        level = (x / width.coerceAtLeast(1)).coerceIn(0.01f, 1f)
        onChange(level)
        val step = (level * 10).toInt()
        if (step != lastHapticStep) {
            lastHapticStep = step
            IslandHaptics.sliderStep(context)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color.White.copy(alpha = 0.14f))
            .pointerInput(Unit) { detectTapGestures { update(it.x, size.width) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); update(change.position.x, size.width) } },
    ) {
        Box(
            Modifier
                .fillMaxWidth(level)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(Color.White),
        )
    }
}
