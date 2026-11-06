package com.sameerasw.essentials.island.remote

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.island.sdk.BriefCardPayload
import com.sameerasw.island.sdk.IslandSurfaceKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class RemoteBriefCard(
    val packageName: String,
    val payload: BriefCardPayload,
    val assets: Map<String, Bitmap>,
    val handler: RemoteItemHandler,
) {
    val key: String get() = "$packageName:${payload.key}"
    val surfaceKey: String get() = IslandSurfaceKey.brief(payload.key)
    val hasPage: Boolean get() = payload.pageHeightDp != null
}

object RemoteBriefCards {
    private val byPackage = MutableStateFlow<Map<String, List<RemoteBriefCard>>>(emptyMap())

    val cards: StateFlow<Map<String, List<RemoteBriefCard>>> = byPackage

    fun set(packageName: String, cards: List<RemoteBriefCard>) {
        byPackage.update { if (cards.isEmpty()) it - packageName else it + (packageName to cards) }
    }

    fun all(): List<RemoteBriefCard> = byPackage.value.values.flatten()
}

@Composable
fun RemoteBriefCardRow(card: RemoteBriefCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val payload = card.payload
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteIcon(payload.iconId?.let(card.assets::get), payload.tone, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(payload.title, style = IslandTextStyles.title.copy(fontSize = 28.sp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = payload.subtitle.orEmpty(),
            style = IslandTextStyles.body.copy(color = islandTextColor(), fontSize = 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        payload.trailingIconId?.let { id ->
            RemoteIcon(card.assets[id], payload.trailingTone, if (payload.trailingText == null) 20.dp else 16.dp)
        }
        payload.trailingText?.let {
            Spacer(Modifier.width(4.dp))
            Text(it, style = IslandTextStyles.body.copy(fontSize = 12.sp))
        }
    }
}
