package com.sameerasw.essentials.island.plugins.flashlight

import com.sameerasw.essentials.utils.DeviceUtils
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.island.ui.IslandHaptics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        if (DeviceUtils.isTorchAccessRestricted()) return
        cameraId = FlashlightUtil.getCameraId(context)
        try {
            cameraManager.registerTorchCallback(callback, ctx!!.mainHandler)
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        fadeJob?.cancel()
        if (DeviceUtils.isTorchAccessRestricted()) return
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
                    Column(Modifier.padding(scope.spec.expandedOutset).padding(top = scope.spec.expandedTopPadding, bottom = scope.spec.expandedBottomPadding)) {
                        scope.CameraRow(
                            horizontalPadding = 20.dp,
                            start = {},
                            end = { RollingText(percentText) },
                        )
                        TorchBeam(
                            level = if (levels) value / 100f else 1f,
                            adjustable = levels,
                            color = MaterialTheme.colorScheme.primary,
                            onChange = ::setFraction,
                            onLampTap = {
                                turnOff()
                                scope.collapse()
                            },
                            onInteraction = scope::keepAlive,
                            modifier = Modifier.padding(horizontal = 20.dp),
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

@Composable
private fun TorchBeam(
    level: Float,
    adjustable: Boolean,
    color: Color,
    onChange: (Float) -> Unit,
    onLampTap: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var current by remember { mutableFloatStateOf(level.coerceIn(MIN_LEVEL, 1f)) }
    var dragging by remember { mutableStateOf(false) }
    var lastStep by remember { mutableIntStateOf((current * 10).toInt()) }
    var lastInteraction by remember { mutableStateOf(0L) }
    LaunchedEffect(level) {
        if (!dragging) current = level.coerceIn(MIN_LEVEL, 1f)
    }
    val shown by animateFloatAsState(current, label = "torchBeam")

    fun set(value: Float) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastInteraction >= INTERACTION_THROTTLE_MS) {
            lastInteraction = now
            onInteraction()
        }
        val previous = current
        current = value.coerceIn(MIN_LEVEL, 1f)
        onChange(current)
        val hitMilestone =
            current != previous &&
                (current == 1f || current == MIN_LEVEL || (previous - MILESTONE_LEVEL) * (current - MILESTONE_LEVEL) <= 0f)
        val step = (current * 10).toInt()
        if (hitMilestone) {
            lastStep = step
            IslandHaptics.tap(context)
        } else if (step != lastStep) {
            lastStep = step
            IslandHaptics.dragStep(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BEAM_HEIGHT)
            .pointerInput(adjustable) {
                detectTapGestures { offset ->
                    val lamp = LAMP_SIZE.toPx()
                    val onLamp = offset.y >= size.height - lamp &&
                        kotlin.math.abs(offset.x - size.width / 2f) <= lamp
                    if (onLamp) {
                        IslandHaptics.commit(context)
                        onLampTap()
                    } else if (adjustable) {
                        IslandHaptics.touchDown(context)
                        set(1f - offset.y / (size.height - lamp))
                    }
                }
            }
            .then(
                if (adjustable) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                onInteraction()
                                IslandHaptics.touchDown(context)
                            },
                            onDragEnd = {
                                dragging = false
                                onInteraction()
                                IslandHaptics.commit(context)
                            },
                            onDragCancel = { dragging = false },
                        ) { change, dragAmount ->
                            change.consume()
                            set(current - dragAmount / (size.height - LAMP_SIZE.toPx()))
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(BEAM_HEIGHT)
                .blur {
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                    radius =
                        BlurRadiusSpec.verticalGradient(
                            listOf(
                                BlurStop(0f, 40.dp),
                                BlurStop(0.5f, 18.dp),
                                BlurStop(0.85f, 4.dp),
                                BlurStop(1f, 0.dp),
                            ),
                        )
                },
        ) {
            val lampTop = size.height - LAMP_SIZE.toPx() * 0.82f
            val cx = size.width / 2f
            val baseHalf = LAMP_SIZE.toPx() * 0.28f
            val length = (lampTop - BEAM_TOP_CLEARANCE.toPx()) * (0.35f + 0.65f * shown)
            val topY = lampTop - length
            val topHalf = baseHalf + length * 0.5f
            val softness = length * 0.2f
            val alpha = 0.15f + 0.65f * shown
            for (layer in 0 until BEAM_LAYERS) {
                val t = layer / (BEAM_LAYERS - 1f)
                val beam = Path().apply {
                    moveTo(cx - baseHalf - t * softness * 0.15f, lampTop)
                    lineTo(cx - topHalf - t * softness, topY - t * softness * 0.5f)
                    lineTo(cx + topHalf + t * softness, topY - t * softness * 0.5f)
                    lineTo(cx + baseHalf + t * softness * 0.15f, lampTop)
                    close()
                }
                drawPath(
                    beam,
                    Brush.verticalGradient(
                        0f to color.copy(alpha = 0f),
                        1f to color.copy(alpha = alpha / BEAM_LAYERS * 1.6f),
                        startY = topY - softness * 0.5f,
                        endY = lampTop,
                    ),
                )
            }
        }
        IslandIcon(R.drawable.round_flashlight_on_24, size = LAMP_SIZE, tint = color)
    }
}

private const val MIN_LEVEL = 0.01f
private const val MILESTONE_LEVEL = 0.8f
private const val INTERACTION_THROTTLE_MS = 500L
private const val BEAM_LAYERS = 8
private val BEAM_HEIGHT = 200.dp
private val LAMP_SIZE = 72.dp
private val BEAM_TOP_CLEARANCE = 40.dp
