package com.sameerasw.essentials.island.plugins.media

import com.sameerasw.essentials.island.ui.components.LocalIslandIconColor
import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.essentials.island.ui.SurfaceBackdrop
import com.sameerasw.essentials.island.ui.DetachedSurfaceVisuals
import com.sameerasw.essentials.island.plugins.media.particles.GeminiParticlePill
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraGeometry
import com.sameerasw.essentials.island.plugins.media.particles.MediaVisualGeometry
import com.sameerasw.essentials.island.ui.components.ArtworkBackdrop
import com.sameerasw.essentials.island.ui.components.MarqueeText
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.IslandExpandedScope
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.IslandSeekBar
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_SETTLE_MS = 1500L

class MediaActions(
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: (() -> Unit)?,
    val like: (() -> Unit)?,
    val progress: () -> Float,
    val canSeek: () -> Boolean = { false },
    val seekTo: (Float) -> Unit = {},
)

class MediaSnapshot(
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val accent: Color?,
    val playing: Boolean,
    val liked: Boolean,
    val actions: MediaActions,
    val open: () -> Unit,
    val canSkipBack: Boolean = true,
    val canSkipForward: Boolean = true,
    val packageName: String? = null,
    val playerAccent: Color? = null,
    val style: MediaPlayerStyle = MediaPlayerStyle.Default,
)

enum class MediaPlayerStyle(val key: String) {
    Default("default"),
    ArtControls("art_controls"),
    ;

    companion object {
        fun from(key: String?): MediaPlayerStyle = entries.firstOrNull { it.key == key } ?: Default
    }
}

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
    canSkipBack: Boolean = false,
    canSkipForward: Boolean = false,
    drawBackground: Boolean = true,
    packageName: String? = null,
    style: MediaPlayerStyle = MediaPlayerStyle.Default,
    layered: Boolean = false,
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
        if (drawBackground && layered) {
            val surfaceAccent = rememberMediaArtworkColors(artwork).primary
            val detached = DetachedSurfaceVisuals { frame ->
                val geometry = { frame().let { MediaVisualGeometry(it.bounds, it.cornerRadius) } }
                val camera = { frame().let { MediaCameraGeometry(it.cameraCenter, it.cameraRadius) } }
                Box(Modifier.fillMaxSize()) {
                    MediaSurface(surfaceAccent, spec.expandedCorner, { frame().pulse }, geometry, camera, Modifier.matchParentSize())
                    if (spec.neuralExpressive) {
                        GeminiParticlePill(
                            modifier = Modifier.matchParentSize(),
                            color = surfaceAccent,
                            cornerRadius = spec.expandedCorner,
                            activation = { frame().pulse },
                            geometry = geometry,
                            protectedCamera = camera,
                        )
                    }
                }
            }
            if (!detached && !SurfaceBackdrop { MediaSurfaceBackdrop(surfaceAccent, spec.expandedCorner, Modifier.fillMaxSize()) }) {
                MediaSurfaceBackdrop(surfaceAccent, spec.expandedCorner, Modifier.matchParentSize())
            }
        } else if (drawBackground && !SurfaceBackdrop { scope.ArtworkBackdrop(image, Modifier.fillMaxSize()) }) {
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
                        .clip(RoundedCornerShape((spec.expandedCorner - spec.expandedOutset - 12.dp).coerceAtLeast(6.dp)))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            IslandHaptics.button(context)
                            val like = actions.like
                            when {
                                style == MediaPlayerStyle.ArtControls -> actions.playPause()
                                like != null -> like()
                                else -> scope.openApp()
                            }
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
                modifier = Modifier.padding(horizontal = 2.dp),
                onInteraction = scope::keepAlive,
                onValueChangeFinished = { target ->
                    actions.seekTo(target)
                    pendingSeek = target to SystemClock.elapsedRealtime()
                    progress = target
                },
            )
        }
        val buttonContainer = accent
        val buttonInk = Color.Black
        val artOnEnd = spec.growDirection > 0
        val appIcon = remember(packageName) { packageName?.let { MediaSessionSource.appIcon(context, it)?.asImageBitmap() } }
        val artBlock: @Composable () -> Unit = {
            Box {
                art(ART_SIZE)
                when {
                    style == MediaPlayerStyle.ArtControls -> ArtPlaybackMark(playing, accent, Modifier.align(Alignment.Center))
                    actions.like != null -> LikeMark(liked, accent, Modifier.align(Alignment.Center))
                }
                if (appIcon != null) {
                    Box(
                        Modifier
                            .align(if (artOnEnd) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(x = if (artOnEnd) (-4).dp else 4.dp, y = 4.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .padding(3.dp)
                            .clip(CircleShape),
                    ) {
                        Image(appIcon, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                }
            }
        }
        val titleText: @Composable () -> Unit = {
            MarqueeText(
                text = title,
                style = IslandTextStyles.title.copy(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val details: @Composable ColumnScope.() -> Unit = {
            if (style == MediaPlayerStyle.ArtControls) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    actions.like?.let { like ->
                        HeartButton(
                            liked = liked,
                            fill = accent,
                            onClick = like,
                            modifier = Modifier.align(if (spec.growDirection < 0) Alignment.TopStart else Alignment.TopEnd),
                        )
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 6.dp),
                    ) {
                        titleText()
                        MarqueeText(
                            text = artist,
                            style = IslandTextStyles.body,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        titleText()
                        MarqueeText(
                            text = artist,
                            style = IslandTextStyles.body,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    PlayerButton(
                        onClick = actions.playPause,
                        container = buttonContainer,
                        modifier = Modifier.size(width = 64.dp, height = 48.dp),
                    ) {
                        AnimatedContent(
                            targetState = playing,
                            transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
                            label = "playPause",
                        ) {
                            ButtonGlyph(if (it) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24, buttonInk, 28.dp)
                        }
                    }
                }
            }
        }
        val info: @Composable RowScope.() -> Unit = {
            Column(
                Modifier
                    .weight(1f)
                    .height(ART_SIZE),
            ) {
                details()
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions.previous?.let { previous ->
                        SkipButton(R.drawable.rounded_skip_previous_24, buttonContainer, canSkipBack, previous)
                    }
                    Box(Modifier.weight(1f)) { seekBar() }
                    SkipButton(R.drawable.rounded_skip_next_24, buttonContainer, canSkipForward, actions.next)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spec.expandedOutset)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (artOnEnd) {
                info()
                artBlock()
            } else {
                artBlock()
                info()
            }
        }
    }
}

private val ART_SIZE = 136.dp
private const val PLAYBACK_FLASH_MS = 900L
private val ART_MARK_OUTLINE = Color.Black.copy(alpha = 0.35f)

@Composable
private fun PlayerButton(
    onClick: () -> Unit,
    container: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier
            .clip(CircleShape)
            .background(container)
            .clickable {
                IslandHaptics.button(context)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun LikeMark(liked: Boolean, fill: Color, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (liked) 1f else 0.9f, IslandMotion.expressiveFastSpatial(), label = "likeMark")
    Box(
        modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = liked,
            transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
            label = "likeMarkIcon",
        ) {
            ButtonGlyph(if (it) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24, if (it) fill else Color.White, 32.dp)
        }
    }
}

@Composable
private fun ArtPlaybackMark(playing: Boolean, color: Color, modifier: Modifier = Modifier) {
    var flash by remember { mutableStateOf<Boolean?>(null) }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(playing) {
        if (!seen) {
            seen = true
            return@LaunchedEffect
        }
        flash = playing
        delay(PLAYBACK_FLASH_MS)
        flash = null
    }
    Box(modifier.size(52.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = flash,
            transitionSpec = { (fadeIn(IslandMotion.contentIn()) + scaleIn(IslandMotion.expressiveFastSpatial(), initialScale = 0.7f)) togetherWith fadeOut(IslandMotion.contentOut()) },
            label = "artPlayback",
        ) { shown ->
            when (shown) {
                null -> EqualizerBars(playing, color, size = 30.dp, outline = ART_MARK_OUTLINE)
                true -> OutlinedGlyph(R.drawable.rounded_play_arrow_24, color, 40.dp)
                false -> OutlinedGlyph(R.drawable.rounded_pause_24, color, 40.dp)
            }
        }
    }
}

@Composable
private fun OutlinedGlyph(icon: Int, tint: Color, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        listOf(-1, 1).forEach { dx ->
            listOf(-1, 1).forEach { dy ->
                Box(Modifier.offset(dx.dp, dy.dp)) { ButtonGlyph(icon, ART_MARK_OUTLINE, size) }
            }
        }
        ButtonGlyph(icon, tint, size)
    }
}

@Composable
private fun HeartButton(liked: Boolean, fill: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scale by animateFloatAsState(if (liked) 1f else 0.9f, IslandMotion.expressiveFastSpatial(), label = "heartButton")
    Box(
        modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable {
                IslandHaptics.button(context)
                onClick()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = liked,
            transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
            label = "heartButtonIcon",
        ) {
            ButtonGlyph(if (it) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24, if (it) fill else Color.White, 24.dp)
        }
    }
}

@Composable
private fun ButtonGlyph(icon: Int, ink: Color, size: Dp) {
    Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(size))
}

@Composable
private fun SkipButton(icon: Int, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) {
                IslandHaptics.button(context)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        ButtonGlyph(icon, tint.copy(alpha = if (enabled) 1f else 0.38f), 20.dp)
    }
}

@Composable
fun mediaAccent(artAccent: Color?): Color = artAccent ?: LocalIslandIconColor.current ?: Color.White
