package com.sameerasw.essentials.island.plugins.media

import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.essentials.island.ui.SurfaceBackdrop
import com.sameerasw.essentials.island.ui.components.ArtworkBackdrop
import com.sameerasw.essentials.island.ui.components.cameraClearance
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import com.sameerasw.essentials.island.ui.components.MarqueeText
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.IslandExpandedScope
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.IslandSeekBar
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_SETTLE_MS = 1500L

class MediaActions(
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: (() -> Unit)?,
    val like: () -> Unit,
    val progress: () -> Float,
    val canSeek: () -> Boolean = { false },
    val seekTo: (Float) -> Unit = {},
)

class MediaSnapshot(
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val accent: Color,
    val playing: Boolean,
    val liked: Boolean,
    val actions: MediaActions,
    val open: () -> Unit,
    val canLike: Boolean = false,
    val canSkipBack: Boolean = true,
    val canSkipForward: Boolean = true,
)

object IslandMediaState {
    val current = kotlinx.coroutines.flow.MutableStateFlow<MediaSnapshot?>(null)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaExpanded(
    title: String,
    artist: String,
    artwork: Bitmap?,
    accent: Color,
    playing: Boolean,
    liked: Boolean,
    actions: MediaActions,
    scope: IslandExpandedScope,
    canLike: Boolean = true,
    canSkipBack: Boolean = false,
    canSkipForward: Boolean = false,
    drawBackground: Boolean = true,
) {
    val spec = scope.spec
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(actions.progress()) }
    var pendingSeek by remember { mutableStateOf<Pair<Float, Long>?>(null) }
    val canSeek = remember(playing, title) { actions.canSeek() }
    LaunchedEffect(playing) {
        while (true) {
            val actual = actions.progress()
            val pending = pendingSeek
            progress =
                if (pending != null && SystemClock.elapsedRealtime() - pending.second < SEEK_SETTLE_MS && abs(actual - pending.first) > 0.02f) {
                    pending.first
                } else {
                    pendingSeek = null
                    actual
                }
            if (!playing) break
            delay(200L)
        }
    }
    val image = remember(artwork) { artwork?.asImageBitmap() }

    Box {
        if (drawBackground && !SurfaceBackdrop { scope.ArtworkBackdrop(image, Modifier.fillMaxSize()) }) {
            scope.ArtworkBackdrop(image, Modifier.matchParentSize())
        }
        val art: @Composable (Dp) -> Unit = { artSize ->
            AnimatedContent(
                targetState = image,
                transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
                label = "playerArt",
            ) { bitmap ->
                Box(
                    Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            IslandHaptics.button(context)
                            scope.openApp()
                        },
                ) {
                    if (bitmap != null) {
                        Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                }
            }
        }
        val seekBar: @Composable () -> Unit = {
            IslandSeekBar(
                value = progress,
                color = accent,
                enabled = canSeek,
                wavy = playing,
                modifier = Modifier.padding(horizontal = 4.dp),
                onInteraction = scope::keepAlive,
                onValueChangeFinished = { target ->
                    actions.seekTo(target)
                    pendingSeek = target to SystemClock.elapsedRealtime()
                    progress = target
                },
            )
        }
        val controls: @Composable (Dp) -> Unit = { rowHeight ->
            ConnectedButtonRow(
                height = rowHeight,
                items = listOfNotNull(
                    ConnectedItem(actions.like, enabled = likable) {
                        IslandIcon(
                            if (liked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
                            tint = if (!likable) Color.LightGray else {if (liked) accent else Color.White},
                            size = 24.dp,
                        )
                    },
                    actions.previous?.let {
                        ConnectedItem(it, enabled = canSkipBack) {
                            IslandIcon(R.drawable.rounded_skip_previous_24, size = 24.dp)
                        }
                    },
                    ConnectedItem(actions.playPause) {
                        AnimatedContent(
                            targetState = playing,
                            transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
                            label = "playPause",
                        ) {
                            IslandIcon(if (it) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24, size = 26.dp)
                        }
                    },
                    ConnectedItem(
                        actions.next, enabled = canSkipForward) {
                            IslandIcon(R.drawable.rounded_skip_next_24, size = 24.dp
                        )
                    },
                ),
            )
        }
        if (spec.landscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spec.expandedOutset)
                    .padding(start = 16.dp, end = 16.dp, top = spec.expandedTopPadding + 12.dp, bottom = spec.expandedBottomPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    art(72.dp)
                    Column(Modifier.weight(1f)) {
                        MarqueeText(
                            text = title,
                            style = IslandTextStyles.title.copy(fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(2.dp))
                        MarqueeText(
                            text = artist,
                            style = IslandTextStyles.body,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    seekBar()
                    Spacer(Modifier.height(4.dp))
                    controls(44.dp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spec.expandedOutset)
                    .padding(start = 12.dp, end = 12.dp, bottom = spec.expandedBottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(spec.cameraDiameter + 28.dp + spec.expandedTopPadding))
                art(88.dp)
                Spacer(Modifier.height(12.dp))
                MarqueeText(
                    text = title,
                    style = IslandTextStyles.title.copy(fontSize = 16.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(4.dp))
                MarqueeText(
                    text = artist,
                    style = IslandTextStyles.body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(4.dp))
                seekBar()
                Spacer(Modifier.height(4.dp))
                controls(52.dp)
            }
        }
    }
}
