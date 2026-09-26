package com.sameerasw.essentials.island.plugins.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ProgressNotificationData
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.services.NotificationListener

class ProgressPlugin : BaseIslandPlugin() {
    override val id = "progress"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_NOTIF_KEEP_PROGRESS)

    private var data: ProgressNotificationData? = null
    private var icon: ImageBitmap? = null

    private val listener = object : NotificationListener.ProgressNotificationListener {
        override fun onProgressNotificationUpdated(data: ProgressNotificationData?) {
            ctx?.mainHandler?.post { update(data) }
        }
    }

    override fun onStart() {
        NotificationListener.addProgressNotificationListener(listener)
    }

    override fun onStop() {
        NotificationListener.removeProgressNotificationListener(listener)
        data = null
        icon = null
    }

    override fun refresh() {
        ctx ?: return
        update(NotificationListener.getLatestProgressNotification())
    }

    private fun update(next: ProgressNotificationData?) {
        if (ctx == null) return
        if (next?.icon !== data?.icon) icon = next?.icon?.asImageBitmap()
        data = next
        render()
    }

    private fun render() {
        val d = data
        if (ctx == null || d == null || !settings.isIslandNotifKeepProgressEnabled()) {
            publish(null)
            return
        }
        val image = icon
        val progress = d.progress / 100f
        val indeterminate = d.isIndeterminate
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.PROGRESS,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("progress.ring") {
                        ProgressRing(progress, indeterminate, image, MaterialTheme.colorScheme.primary)
                    },
                ),
                expanded = ExpandedContent { scope ->
                    ProgressExpanded(
                        icon = d.icon,
                        title = d.title ?: d.appName ?: d.packageName,
                        text = d.text,
                        progress = progress,
                        indeterminate = indeterminate,
                        accent = MaterialTheme.colorScheme.primary,
                        onOpen = { open(d) },
                        scope = scope,
                    )
                },
                onOpen = { open(d) },
                sourcePackage = d.packageName,
            ),
        )
    }

    private fun open(d: ProgressNotificationData) {
        if (!sendPendingIntent(context, d.contentIntent)) launchPackage(context, d.packageName)
    }

    companion object {
        const val ITEM_KEY = "progress"
    }
}

@Composable
private fun ProgressRing(progress: Float, indeterminate: Boolean, icon: ImageBitmap?, color: Color, size: Dp = 24.dp) {
    val sweep by animateFloatAsState(360f * progress.coerceIn(0f, 1f), IslandMotion.float(), label = "progressSweep")
    val rotation by rememberInfiniteTransition(label = "progressSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "progressRotation",
    )
    Box(
        Modifier
            .sizeIn(maxWidth = size, maxHeight = size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension
            val stroke = diameter * 0.1f
            val topLeft = Offset((this.size.width - diameter + stroke) / 2f, (this.size.height - diameter + stroke) / 2f)
            val arcSize = Size(diameter - stroke, diameter - stroke)
            drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (indeterminate) {
                drawArc(color, rotation - 90f, 90f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            } else {
                drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(0.62f).aspectRatio(1f).clip(CircleShape),
            )
        }
    }
}
