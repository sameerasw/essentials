/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallThemingSheets.kt
 * Description: Bottom sheets and visual mini-fan canvas samples for Connector Styles and 57 Preset Color Themes.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.essentials.utils.AppIconUtil
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val TILE_COLUMNS = 3
private const val SAMPLE_ITEM_COUNT = 3
private const val SAMPLE_BALL_SIZE = 42f
private const val SAMPLE_BUTTON_SIZE = 44f
private const val SAMPLE_RADIUS = 84f
private const val SAMPLE_ICON_FRACTION = 0.32f

private val SampleLayout = FanLayout.compute(
    count = SAMPLE_ITEM_COUNT,
    buttonSize = SAMPLE_BUTTON_SIZE,
    radius = SAMPLE_RADIUS,
    halfFab = SAMPLE_BALL_SIZE / 2f,
    density = 1f,
)

private val TileShape = RoundedCornerShape(16.dp)
private val SampleHeight = 70.dp
private val SampleStrokeWidth = 1.6.dp

fun DrawScope.drawSampleConnectors(
    layout: FanLayout,
    style: MenuConnectorStyle,
    color: Color,
    center: Offset,
    unit: Float,
    strokeWidth: Float,
    halfBall: Float,
    halfButton: Float,
) {
    if (style == MenuConnectorStyle.NONE) return

    val renderer = ConnectorRenderer(style, color.toArgb(), strokeWidth)
    val isArc = style == MenuConnectorStyle.ARC
    val start = if (isArc) layout.arcRadius * unit else (halfBall + ConnectorRenderer.INSET_DP) * unit

    drawIntoCanvas { composeCanvas ->
        val canvas = composeCanvas.nativeCanvas

        if (isArc) {
            renderer.drawRing(
                canvas, center.x, center.y, start,
                Math.toRadians(180.0 - layout.arcEdgeAngle),
                Math.toRadians(90.0 + layout.arcEdgeAngle),
                opacity = 1f,
            )
        } else if (renderer.hasRing) {
            val (ringStart, ringSweep) = FanLayout.angleRange(layout.offsets)
            renderer.drawRing(canvas, center.x, center.y, start, ringStart, ringSweep, opacity = 1f)
        }

        val bends = FanLayout.curveBends(layout.offsets)

        layout.offsets.forEachIndexed { index, (offsetX, offsetY) ->
            val distance = hypot(offsetX, offsetY)
            if (distance <= 0f) return@forEachIndexed

            renderer.drawConnector(
                canvas = canvas,
                centerX = center.x,
                centerY = center.y,
                directionX = offsetX / distance,
                directionY = offsetY / distance,
                start = start,
                end = (distance - halfButton) * unit,
                opacity = 1f,
                bend = bends.getOrElse(index) { 0f },
            )
        }
    }
}

@Composable
fun ConnectorStyleSample(
    style: MenuConnectorStyle,
    connectorColor: Color,
    itemColor: Color,
    ballColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = SampleStrokeWidth,
    itemBorderColor: Color? = null,
    ballBorderColor: Color? = null,
    itemIconColor: Color? = null,
    ballIconColor: Color? = null,
) {
    Canvas(modifier = modifier) {
        val halfBall = SAMPLE_BALL_SIZE / 2f
        val halfButton = SAMPLE_BUTTON_SIZE / 2f

        val reach = SampleLayout.offsets.maxOf { -it.first } + halfButton
        val halfHeight = max(SampleLayout.offsets.maxOf { abs(it.second) } + halfButton, halfBall)
        val fullWidth = reach + halfBall
        val unit = min(size.width / fullWidth, size.height / (halfHeight * 2f))

        val center = Offset(
            x = (size.width - fullWidth * unit) / 2f + reach * unit,
            y = size.height / 2f,
        )

        drawSampleConnectors(
            layout = SampleLayout,
            style = style,
            color = connectorColor,
            center = center,
            unit = unit,
            strokeWidth = strokeWidth.toPx(),
            halfBall = halfBall,
            halfButton = halfButton,
        )

        val borderStroke = Stroke(width = strokeWidth.toPx())
        val iconRadius = halfButton * unit * SAMPLE_ICON_FRACTION

        SampleLayout.offsets.forEach { (offsetX, offsetY) ->
            val itemCenter = center + Offset(offsetX * unit, offsetY * unit)
            drawCircle(color = itemColor, radius = halfButton * unit, center = itemCenter)
            itemBorderColor?.let {
                drawCircle(it, radius = halfButton * unit - borderStroke.width / 2f, center = itemCenter, style = borderStroke)
            }
            itemIconColor?.let { drawCircle(it, radius = iconRadius, center = itemCenter) }
        }

        drawCircle(color = ballColor, radius = halfBall * unit, center = center)
        ballBorderColor?.let {
            drawCircle(it, radius = halfBall * unit - borderStroke.width / 2f, center = center, style = borderStroke)
        }
        ballIconColor?.let { drawCircle(it, radius = iconRadius, center = center) }
    }
}

@Composable
fun QuickBallConnectorSheet(
    selectedStyleName: String,
    onStyleSelected: (MenuConnectorStyle) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_ball_connector_style),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MenuConnectorStyle.entries.chunked(TILE_COLUMNS).forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowStyles.forEach { style ->
                            val isSelected = style.name.equals(selectedStyleName, ignoreCase = true)
                            val accent = if (isSelected) colors.primary else colors.onSurfaceVariant
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TileShape)
                                    .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceContainerHighest.copy(alpha = 0.6f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f),
                                        shape = TileShape,
                                    )
                                    .clickable(role = Role.RadioButton) {
                                        onStyleSelected(style)
                                        onDismissRequest()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ConnectorStyleSample(
                                    style = style,
                                    connectorColor = accent,
                                    itemColor = colors.surfaceContainerLow,
                                    ballColor = accent,
                                    itemBorderColor = colors.outlineVariant,
                                    ballBorderColor = colors.primary,
                                    itemIconColor = accent.copy(alpha = 0.8f),
                                    ballIconColor = colors.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(SampleHeight),
                                )
                                Text(
                                    text = stringResource(style.titleRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.primary else colors.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        repeat(TILE_COLUMNS - rowStyles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickBallPresetThemeSheet(
    selectedThemeName: String,
    connectorStyle: MenuConnectorStyle,
    onThemeSelected: (QuickBallPresetTheme) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_ball_preset_theme_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickBallPresetTheme.entries.chunked(TILE_COLUMNS).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowThemes.forEach { theme ->
                            val isSelected = theme.name.equals(selectedThemeName, ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TileShape)
                                    .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceContainerHighest.copy(alpha = 0.6f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primary else colors.outlineVariant.copy(alpha = 0.5f),
                                        shape = TileShape,
                                    )
                                    .clickable(role = Role.RadioButton) {
                                        onThemeSelected(theme)
                                        onDismissRequest()
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ConnectorStyleSample(
                                    style = connectorStyle,
                                    connectorColor = Color(theme.connectorColor),
                                    itemColor = Color(theme.menuColor),
                                    ballColor = Color(theme.ballColor),
                                    itemBorderColor = Color(theme.menuBorderColor),
                                    ballBorderColor = Color(theme.ballBorderColor),
                                    itemIconColor = Color(theme.menuIconColor),
                                    ballIconColor = Color(theme.ballIconColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(SampleHeight),
                                )
                                Text(
                                    text = theme.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.primary else colors.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        repeat(TILE_COLUMNS - rowThemes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_PREVIEW_HEIGHT_DP = 210f

@Composable
fun QuickBallMenuPreview(
    actions: List<Action>,
    connectorStyle: MenuConnectorStyle,
    connectorColor: Color,
    connectorWidthDp: Float,
    ballSizeDp: Float,
    buttonSizeDp: Float,
    menuRadiusDp: Float,
    iconSizeDp: Float,
    ballColor: Color,
    ballIconColor: Color,
    buttonColor: Color,
    buttonIconColor: Color,
    isBorderEnabled: Boolean,
    borderWidthDp: Float,
    borderColor: Color,
    ballBorderColor: Color,
    shortcutStyle: QuickBallShortcutStyle = QuickBallShortcutStyle.FULL_BLEED,
    modifier: Modifier = Modifier,
) {
    if (actions.size < 2) return

    val context = LocalContext.current
    val halfFab = ballSizeDp / 2f

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val layout = remember(actions.size, buttonSizeDp, menuRadiusDp, halfFab) {
            FanLayout.compute(
                count = actions.size,
                buttonSize = buttonSizeDp,
                radius = menuRadiusDp,
                halfFab = halfFab,
                density = 1f,
            )
        }

        val reach = layout.offsets.maxOf { -it.first } + buttonSizeDp / 2f
        val halfHeight = max(
            layout.offsets.maxOf { abs(it.second) } + buttonSizeDp / 2f,
            max(halfFab, layout.arcRadius),
        )
        val fullWidth = reach + halfFab
        val fullHeight = halfHeight * 2f
        val scale = min(1f, min(maxWidth.value / fullWidth, MAX_PREVIEW_HEIGHT_DP / fullHeight))

        Box(
            modifier = Modifier.size((fullWidth * scale).dp, (fullHeight * scale).dp),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawSampleConnectors(
                    layout = layout,
                    style = connectorStyle,
                    color = connectorColor,
                    center = Offset((reach * scale).dp.toPx(), (halfHeight * scale).dp.toPx()),
                    unit = scale.dp.toPx(),
                    strokeWidth = (connectorWidthDp * scale).dp.toPx(),
                    halfBall = halfFab,
                    halfButton = buttonSizeDp / 2f,
                )
            }

            // Radial shortcut buttons
            actions.forEachIndexed { index, action ->
                val (offsetX, offsetY) = layout.offsets[index]
                val isApp = action is Action.OpenApp && action.packageName.isNotBlank()
                val effectiveBg = if (isApp && shortcutStyle == QuickBallShortcutStyle.FULL_BLEED) {
                    Color.Transparent
                } else {
                    buttonColor
                }

                val borderModifier = if (isBorderEnabled && borderWidthDp > 0f && effectiveBg != Color.Transparent) {
                    Modifier.border((borderWidthDp * scale).dp, borderColor, CircleShape)
                } else Modifier

                Box(
                    modifier = Modifier
                        .offset(
                            x = ((reach + offsetX - buttonSizeDp / 2f) * scale).dp,
                            y = ((halfHeight + offsetY - buttonSizeDp / 2f) * scale).dp,
                        )
                        .size((buttonSizeDp * scale).dp)
                        .clip(CircleShape)
                        .background(effectiveBg)
                        .then(borderModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    var appBitmap by remember(action, shortcutStyle) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

                    LaunchedEffect(action, shortcutStyle) {
                        if (isApp) {
                            try {
                                val pkg = (action as Action.OpenApp).packageName
                                if (shortcutStyle == QuickBallShortcutStyle.MONOCHROME) {
                                    val monoBmp = AppIconUtil.monochromeAppIcon(context, pkg)
                                    if (monoBmp != null) {
                                        appBitmap = monoBmp.asImageBitmap()
                                    } else {
                                        val d = context.packageManager.getApplicationIcon(pkg)
                                        val bmp = android.graphics.Bitmap.createBitmap(
                                            d.intrinsicWidth.coerceAtLeast(1),
                                            d.intrinsicHeight.coerceAtLeast(1),
                                            android.graphics.Bitmap.Config.ARGB_8888,
                                        )
                                        val c = android.graphics.Canvas(bmp)
                                        d.setBounds(0, 0, c.width, c.height)
                                        d.draw(c)
                                        appBitmap = bmp.asImageBitmap()
                                    }
                                } else {
                                    val d = context.packageManager.getApplicationIcon(pkg)
                                    val bmp = android.graphics.Bitmap.createBitmap(
                                        d.intrinsicWidth.coerceAtLeast(1),
                                        d.intrinsicHeight.coerceAtLeast(1),
                                        android.graphics.Bitmap.Config.ARGB_8888,
                                    )
                                    val c = android.graphics.Canvas(bmp)
                                    d.setBounds(0, 0, c.width, c.height)
                                    d.draw(c)
                                    appBitmap = bmp.asImageBitmap()
                                }
                            } catch (_: Exception) {
                                appBitmap = null
                            }
                        }
                    }

                    val bmp = appBitmap
                    if (bmp != null) {
                        if (shortcutStyle == QuickBallShortcutStyle.FULL_BLEED) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                            )
                        } else if (shortcutStyle == QuickBallShortcutStyle.MONOCHROME) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(buttonIconColor),
                                modifier = Modifier.size((iconSizeDp * scale).dp),
                            )
                        } else {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.size((iconSizeDp * scale).dp),
                            )
                        }
                    } else {
                        val iconRes = QuickBallIconResolver.resolveIcon(action, QuickBallIconPack.CLASSIC)
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size((iconSizeDp * scale).dp),
                        )
                    }
                }
            }

            // Floating Ball (shown expanded with ic_menu_close 'X')
            val ballBorderModifier = if (isBorderEnabled && borderWidthDp > 0f) {
                Modifier.border((borderWidthDp * scale).dp, ballBorderColor, CircleShape)
            } else Modifier

            Box(
                modifier = Modifier
                    .offset(
                        x = ((reach - halfFab) * scale).dp,
                        y = ((halfHeight - halfFab) * scale).dp,
                    )
                    .size((ballSizeDp * scale).dp)
                    .clip(CircleShape)
                    .background(ballColor)
                    .then(ballBorderModifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_close),
                    contentDescription = null,
                    tint = ballIconColor,
                    modifier = Modifier.size(((ballSizeDp - 16f).coerceAtLeast(18f) * scale).dp),
                )
            }
        }
    }
}
