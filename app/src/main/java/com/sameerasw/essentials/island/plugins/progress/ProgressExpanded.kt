package com.sameerasw.essentials.island.plugins.progress

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.MarqueeText

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressExpanded(
    icon: Bitmap?,
    title: String,
    text: String?,
    progress: Float,
    indeterminate: Boolean,
    accent: Color,
    actions: List<NotificationActionItem>,
    onAction: (NotificationActionItem) -> Unit,
    onOpen: () -> Unit,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    Box(propagateMinConstraints = true) {
        Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
            Spacer(Modifier.height(spec.expandedTopPadding))
            scope.CameraRow(
                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                start = {
                    IslandBitmap(icon, spec.cellSize, fallbackRes = R.drawable.rounded_downloading_24)
                    MarqueeText(text = title, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                },
                end = {
                    if (!indeterminate) Text("${(progress * 100).toInt()}%", style = IslandTextStyles.body.copy(color = islandTextColor()))
                },
            )
            if (!text.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = IslandTextStyles.body.copy(color = islandTextColor(), fontSize = 15.sp, lineHeight = 20.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
            }
            Spacer(Modifier.height(16.dp))
            val barModifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding)
            if (indeterminate) {
                LinearWavyProgressIndicator(modifier = barModifier, color = accent, trackColor = Color.White.copy(alpha = 0.2f))
            } else {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = barModifier,
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }
            Spacer(Modifier.height(16.dp))
            ConnectedButtonRow(
                height = 44.dp,
                modifier = Modifier.padding(horizontal = sidePadding).padding(bottom = spec.expandedBottomPadding),
                container = Color.White.copy(alpha = 0.2f),
                items = listOf(
                    ConnectedItem({
                        onOpen()
                        scope.collapse()
                    }) { ConnectedTextLabel(stringResource(R.string.island_progress_open)) },
                ) + actions.take(3).map { action ->
                    ConnectedItem({ onAction(action) }) { ConnectedTextLabel(action.title) }
                },
            )
        }
    }
}
