package com.sameerasw.essentials.island.plugins.call

import com.sameerasw.essentials.island.ui.components.ArtworkBackdrop
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText

enum class CallButtonStyle { Answer, End, Neutral }

class CallButtonSpec(
    val key: String,
    val icon: Int?,
    val label: String,
    val style: CallButtonStyle = CallButtonStyle.Neutral,
    val initialOn: Boolean = false,
    val onIcon: Int? = null,
    val onClick: () -> Boolean?,
)

private val AnswerGreen = Color(0xFF2E7D32)
private val EndRed = Color(0xFFC62828)

@Composable
fun CallExpanded(
    caller: String,
    number: String?,
    photo: Bitmap?,
    ringing: Boolean,
    status: String,
    buttons: List<CallButtonSpec>,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val on = remember(buttons.map { it.key }) { mutableStateListOf(*buttons.map { it.initialOn }.toTypedArray()) }

    val image = remember(photo) { photo?.asImageBitmap() }
    Box(propagateMinConstraints = true) {
        scope.ArtworkBackdrop(image, Modifier.matchParentSize())
        Column(
            modifier = Modifier.fillMaxWidth().padding(spec.expandedOutset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        IslandBitmap(photo, spec.cellSize, circle = true, fallbackRes = R.drawable.rounded_call_24)
                        MarqueeText(text = caller, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = {
                        if (ringing) {
                            Text(status, style = IslandTextStyles.body, maxLines = 1)
                        } else {
                            RollingText(status)
                        }
                    },
                )
                if (number != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = number,
                        style = IslandTextStyles.body.copy(fontSize = 15.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = sidePadding),
                    )
                }
            }
            Column {
                Spacer(Modifier.height(14.dp))
                val items = buttons.mapIndexed { index, spec ->
                    val container = when (spec.style) {
                        CallButtonStyle.Answer -> AnswerGreen
                        CallButtonStyle.End -> EndRed
                        CallButtonStyle.Neutral -> if (on.getOrElse(index) { false }) Color.White.copy(alpha = 0.4f) else null
                    }
                    ConnectedItem(
                        onClick = { spec.onClick()?.let { if (index < on.size) on[index] = it } },
                        container = container,
                    ) {
                        val icon = if (on.getOrElse(index) { false }) spec.onIcon ?: spec.icon else spec.icon
                        CallButton(icon, spec.label)
                    }
                }
                ConnectedButtonRow(
                    height = 44.dp,
                    modifier = Modifier.padding(horizontal = sidePadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = items,
                )
                Spacer(Modifier.height(spec.expandedBottomPadding))
            }
        }
    }
}

@Composable
private fun CallButton(icon: Int?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) IslandIcon(icon, size = 18.dp)
        Text(label, style = IslandTextStyles.line, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
