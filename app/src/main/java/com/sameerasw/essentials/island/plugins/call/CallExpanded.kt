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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

class CallActions(
    val answer: () -> Unit,
    val end: () -> Unit,
    val toggleMute: () -> Boolean,
    val toggleSpeaker: () -> Boolean,
    val isMuted: () -> Boolean,
    val isSpeakerOn: () -> Boolean,
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
    actions: CallActions,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    var muted by remember { mutableStateOf(actions.isMuted()) }
    var speaker by remember { mutableStateOf(actions.isSpeakerOn()) }

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
                val items = if (ringing) {
                    listOf(
                        ConnectedItem(actions.end, container = EndRed) {
                            CallButton(R.drawable.rounded_call_end_24, stringResource(R.string.island_call_decline))
                        },
                        ConnectedItem(actions.answer, container = AnswerGreen) {
                            CallButton(R.drawable.rounded_call_24, stringResource(R.string.island_call_answer))
                        },
                    )
                } else {
                    listOf(
                        ConnectedItem({ muted = actions.toggleMute() }, container = if (muted) Color.White.copy(alpha = 0.4f) else null) {
                            CallButton(if (muted) R.drawable.rounded_mic_off_24 else R.drawable.rounded_mic_24, stringResource(R.string.island_call_mute))
                        },
                        ConnectedItem({ speaker = actions.toggleSpeaker() }, container = if (speaker) Color.White.copy(alpha = 0.4f) else null) {
                            CallButton(R.drawable.rounded_volume_up_24, stringResource(R.string.island_call_speaker))
                        },
                        ConnectedItem(actions.end, container = EndRed) {
                            CallButton(R.drawable.rounded_call_end_24, stringResource(R.string.island_call_end))
                        },
                    )
                }
                ConnectedButtonRow(
                    height = 44.dp,
                    modifier = Modifier.padding(horizontal = sidePadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = items,
                )
                Spacer(Modifier.height(spec.expandedPadding * 0.7f))
            }
        }
    }
}

@Composable
private fun CallButton(icon: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IslandIcon(icon, size = 18.dp)
        Text(label, style = IslandTextStyles.line, maxLines = 1)
    }
}
