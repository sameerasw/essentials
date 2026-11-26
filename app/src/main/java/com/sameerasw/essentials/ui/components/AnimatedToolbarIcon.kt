package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class ToolbarIconEffect(val durationMs: Int) {
    POP(450),
    LOGO(900),
    REVEAL(600),
    SHAKE(500),
    FILL(900),
    SHINE(750),
    SPIN(650),
}

@Composable
fun AnimatedToolbarIcon(
    iconRes: Int,
    contentDescription: String?,
    tint: Color,
    selected: Boolean,
    effect: ToolbarIconEffect?,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (selected) {
            if (effect == ToolbarIconEffect.LOGO) {
                progress.animateTo(0f, tween(250, easing = FastOutSlowInEasing))
            } else {
                progress.snapTo(0f)
            }
            if (effect == null) return@LaunchedEffect
            val easing = if (effect == ToolbarIconEffect.REVEAL || effect == ToolbarIconEffect.SPIN) FastOutSlowInEasing else LinearEasing
            progress.animateTo(1f, tween(effect.durationMs, easing = easing))
        }
    }

    val p = progress.value
    val bell = sin(PI * p).toFloat()

    when (effect) {
        ToolbarIconEffect.LOGO -> {
            LayeredLogoIcon(tint = tint, progress = p, modifier = modifier)
            return
        }

        ToolbarIconEffect.FILL -> {
            FillingFlaskIcon(tint = tint, progress = p, modifier = modifier)
            return
        }

        else -> Unit
    }

    val effectModifier =
        when (effect) {
            null, ToolbarIconEffect.LOGO, ToolbarIconEffect.FILL -> Modifier

            ToolbarIconEffect.POP ->
                Modifier.graphicsLayer {
                    val s = 1f + 0.22f * bell
                    scaleX = s
                    scaleY = s
                }

            ToolbarIconEffect.REVEAL ->
                Modifier
                    .graphicsLayer {
                        rotationZ = -60f * (1f - p)
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        if (p >= 1f) {
                            drawContent()
                        } else {
                            val radius = size.maxDimension * 0.75f * p
                            val circle = Path().apply { addOval(Rect(center, radius)) }
                            clipPath(circle) { this@drawWithContent.drawContent() }
                        }
                    }

            ToolbarIconEffect.SHAKE ->
                Modifier.graphicsLayer {
                    val wave = sin(p * PI * 6).toFloat() * (1f - p)
                    translationX = wave * 2.dp.toPx()
                    rotationZ = wave * 8f
                }

            ToolbarIconEffect.SHINE ->
                Modifier
                    .graphicsLayer {
                        val s = 1f + 0.15f * bell
                        scaleX = s
                        scaleY = s
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        if (p > 0f && p < 1f) {
                            val w = size.width
                            val x = -w + 3f * w * p
                            drawRect(
                                brush =
                                    Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                                        start = Offset(x - w * 0.35f, 0f),
                                        end = Offset(x + w * 0.35f, size.height),
                                    ),
                                blendMode = BlendMode.SrcAtop,
                            )
                        }
                    }

            ToolbarIconEffect.SPIN ->
                Modifier.graphicsLayer {
                    rotationZ = 180f * p
                    val s = 1f + 0.1f * bell
                    scaleX = s
                    scaleY = s
                }
        }

    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.then(effectModifier),
    )
}
