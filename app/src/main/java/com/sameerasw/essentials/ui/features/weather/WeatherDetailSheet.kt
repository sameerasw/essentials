package com.sameerasw.essentials.ui.features.weather

import android.content.Context
import com.sameerasw.essentials.weather.effects.WeatherEffectHaptics
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.layout.Layout
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import com.sameerasw.essentials.ui.modifiers.heatHaze
import com.sameerasw.essentials.ui.modifiers.rainOnGlass
import com.sameerasw.essentials.weather.effects.RainSurface
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.foundation.layout.PaddingValues
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.ui.theme.Typography
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.weather.WeatherFormat
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.effects.DeviceWeatherHaptics
import com.sameerasw.essentials.weather.location.DeviceLocationSource
import com.sameerasw.essentials.weather.effects.WeatherEffectSpec
import com.sameerasw.essentials.weather.effects.WeatherEffects
import com.sameerasw.essentials.weather.effects.WeatherSimulation
import com.sameerasw.essentials.weather.model.DailyForecast
import com.sameerasw.essentials.weather.model.TemperatureUnit
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherError
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import com.sameerasw.essentials.weather.provider.WeatherProviders
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal class WeatherPresentation(
    val snapshot: WeatherSnapshot?,
    val palette: WeatherPalette,
    val effectSpec: WeatherEffectSpec,
    val unit: TemperatureUnit,
    val now: Long,
    val haptics: WeatherEffectHaptics?,
)

@Composable
internal fun rememberWeatherPresentation(real: WeatherSnapshot?): WeatherPresentation {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(context) {
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> settingsVersion++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val simulation = remember(settingsVersion) { settings.getSimulatedWeather() }
    val timeOverride = remember(settingsVersion) { settings.getSimulatedTimeOfDay() }
    val tempOverride = remember(settingsVersion) { settings.getSimulatedTempC() }
    val simulated = real?.let { r -> simulation?.let { WeatherSimulation.apply(r, it) } ?: r }
    val snapshot = simulated?.let { WeatherSimulation.withTimeOfDay(it, timeOverride) }?.let { s -> tempOverride?.let { s.copy(tempC = it) } ?: s }
    val unit = remember(settingsVersion) { WeatherFormat.unitFor(settings.getWeatherUnits()) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            clock = System.currentTimeMillis()
        }
    }
    val now = WeatherSimulation.timeFor(snapshot, timeOverride, clock)
    val palette = animatedPalette(
        remember(snapshot?.condition, snapshot?.isDay, snapshot?.extras?.sunriseMillis, snapshot?.extras?.sunsetMillis, now / 60_000L, timeOverride) {
            WeatherPalette.from(snapshot, now, timeOverride)
        },
    )
    val effects = remember(settingsVersion) { settings.isIslandWeatherEffectsEnabled() && !DeviceUtils.isPowerSaveMode(context) }
    val haptics = remember(settingsVersion, effects) {
        DeviceWeatherHaptics(context).takeIf { effects && settings.isIslandWeatherHapticsEnabled() }
    }
    val effectSpec = remember(effects, snapshot?.condition, snapshot?.isDay, snapshot?.windKph, simulation?.id) {
        snapshot?.takeIf { effects }?.let { simulation?.spec ?: WeatherEffectSpec.from(it) } ?: WeatherEffectSpec.None
    }
    return WeatherPresentation(snapshot, palette, effectSpec, unit, now, haptics)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val state by WeatherRepository.state.collectAsState()
    val presentation = rememberWeatherPresentation(state.snapshot)
    val snapshot = presentation.snapshot
    val unit = presentation.unit
    val now = presentation.now
    val palette = presentation.palette
    val effectSpec = presentation.effectSpec
    val effectHaptics = presentation.haptics

    val requestLocation = rememberLocationPermissionRequest { granted ->
        if (granted) scope.launch { WeatherRepository.refresh(context, force = true) }
    }

    LaunchedEffect(Unit) {
        WeatherRepository.ensureLoaded(context)
        val needsLocation = settings.getWeatherLocationMode() != "manual" && !DeviceLocationSource.hasPermission(context)
        if (needsLocation) requestLocation() else if (WeatherRepository.isStale(context)) WeatherRepository.refresh(context)
    }

    val rainSurfaces = remember { androidx.compose.runtime.mutableStateMapOf<String, RainSurfaceSource>() }
    val headerBottom = remember { mutableFloatStateOf(0f) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.Black,
            surface = palette.base,
            onSurface = palette.onBase,
            onSurfaceVariant = palette.onBaseMuted,
            background = palette.base,
            onBackground = palette.onBase,
        ),
        typography = Typography,
        shapes = Shapes,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalRainSurfaces provides rainSurfaces) {
        val glassRain = effectSpec.layers.filterIsInstance<com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Rain>().maxByOrNull { it.intensity }
        val density = LocalDensity.current
        val collapse = remember { mutableFloatStateOf(0f) }
        val scrollTick = remember { intArrayOf(0) }
        val sky = snapshot?.let { skyState(it, now) }
        val skyHeightPx = with(density) { SKY_HEIGHT.toPx() }
        val collapseShiftPx = with(density) { 60.dp.toPx() }
        val fallbackYPx = with(density) { 150.dp.toPx() }
        Box(Modifier.fillMaxSize().rainOnGlass(glassRain?.intensity ?: 0f, glassRain?.slant ?: 0f) {
            if (sky == null) {
                androidx.compose.ui.geometry.Offset(0.5f, fallbackYPx - collapse.floatValue * collapseShiftPx)
            } else {
                val t = sky.first
                androidx.compose.ui.geometry.Offset(
                    0.9f - 0.8f * t,
                    skyHeightPx * (0.66f - 0.4f * kotlin.math.sin(Math.PI.toFloat() * t)) - collapse.floatValue * collapseShiftPx,
                )
            }
        }) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to palette.glow,
                                0.4f to palette.glowSecondary,
                                0.8f to palette.base,
                                1f to palette.base,
                            ),
                        ),
                )
                snapshot?.let { SkyBody(it, now, { collapse.floatValue }) }
                if (!effectSpec.isEmpty) {
                    WeatherEffects(
                        spec = effectSpec,
                        modifier = Modifier.matchParentSize(),
                        strength = 1.7f,
                        haptics = effectHaptics,
                        surfaces = { rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) }.filter { it.rect.top >= headerBottom.floatValue } },
                        scrollTick = { scrollTick[0] },
                    )
                }
                val scrollState = rememberScrollState()
                val maxCollapsePx = with(density) { COLLAPSE_RANGE.toPx() }
                val connection = remember(maxCollapsePx) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y != 0f) scrollTick[0]++
                            if (available.y >= 0f || collapse.floatValue >= 1f) return Offset.Zero
                            val next = (collapse.floatValue - available.y / maxCollapsePx).coerceIn(0f, 1f)
                            val consumed = -(next - collapse.floatValue) * maxCollapsePx
                            collapse.floatValue = next
                            return Offset(0f, consumed)
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            if (available.y <= 0f || collapse.floatValue <= 0f) return Offset.Zero
                            val next = (collapse.floatValue - available.y / maxCollapsePx).coerceIn(0f, 1f)
                            val used = -(next - collapse.floatValue) * maxCollapsePx
                            collapse.floatValue = next
                            return Offset(0f, used)
                        }
                    }
                }
                if (snapshot == null) {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(120.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (state.loading) LoadingIndicator() else Text(errorLabel(context, state.error), color = palette.onBaseMuted)
                        }
                        if (state.error == WeatherError.LocationPermission) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                androidx.compose.material3.FilledTonalButton(onClick = { requestLocation() }) {
                                    Text(stringResource(R.string.weather_grant_location))
                                }
                            }
                        }
                    }
                } else {
                    var topPx by headerBottom
                    val topFadePx = if (topPx > 0f) topPx + with(density) { 40.dp.toPx() } else 0f
                    val bottomFadePx = WindowInsets.navigationBars.getBottom(density) + with(density) { 20.dp.toPx() }
                    Box(Modifier.fillMaxSize().nestedScroll(connection)) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    if (topFadePx > 0f) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startY = 0f,
                                                endY = topFadePx,
                                            ),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
                                            startY = size.height - bottomFadePx,
                                            endY = size.height,
                                        ),
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - bottomFadePx),
                                        size = androidx.compose.ui.geometry.Size(size.width, bottomFadePx),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                                .progressiveBlur(blurRadius = 40f, height = topFadePx, direction = BlurDirection.TOP, showGradientOverlay = false)
                                .progressiveBlur(blurRadius = 14f, height = bottomFadePx, direction = BlurDirection.BOTTOM, showGradientOverlay = false),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(top = with(density) { topPx.toDp() } + 28.dp)
                                    .navigationBarsPadding()
                                    .padding(bottom = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                snapshot.activeAlerts().sortedByDescending { it.severity.ordinal }
                                    .forEach { AlertCard(it, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                                HourlySection(snapshot, unit, palette)
                                snapshot.daily.orEmpty().takeIf { it.isNotEmpty() }
                                    ?.let { DailySection(it, unit, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                                DetailsSection(snapshot, unit, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                SunSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                Footer(
                                    modifier = Modifier.padding(horizontal = SIDE_PADDING),
                                    snapshot = snapshot,
                                    loading = state.loading,
                                    error = state.error,
                                    palette = palette,
                                    onRefresh = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        scope.launch { WeatherRepository.refresh(context, force = true) }
                                    },
                                )
                            }
                        }
                        Column(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .onSizeChanged { topPx = it.height.toFloat() }
                                .statusBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val progress = collapse.floatValue
                            Spacer(Modifier.height(14.dp))
                            Box(Modifier.foldAway(((progress - 0.25f) / 0.5f).coerceIn(0f, 1f))) {
                                LocationChip(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                            }
                            Header(snapshot, unit, palette, progress, Modifier.padding(horizontal = SIDE_PADDING))
                        }
                    }
                }
        }
        }
    }
}

private fun skyState(snapshot: WeatherSnapshot, now: Long): Pair<Float, Boolean>? {
    val rise = snapshot.extras?.sunriseMillis ?: return null
    val set = snapshot.extras?.sunsetMillis ?: return null
    val day = 24 * 60 * 60_000L
    val shift = Math.floorDiv(now - rise, day) * day
    val r = rise + shift
    val s = set + shift
    val isSun = now in r..s
    val t = when {
        isSun -> (now - r).toFloat() / (s - r).coerceAtLeast(1L)
        now > s -> (now - s).toFloat() / ((r + day) - s).coerceAtLeast(1L)
        else -> (now - (s - day)).toFloat() / (r - (s - day)).coerceAtLeast(1L)
    }.coerceIn(0f, 1f)
    return t to isSun
}

private val SKY_HEIGHT = 360.dp

@Composable
private fun SkyBody(snapshot: WeatherSnapshot, now: Long, collapse: () -> Float) {
    val (t, isSun) = skyState(snapshot, now) ?: return
    val hide = (1f - collapse() * 1.6f).coerceIn(0f, 1f)
    if (hide <= 0f) return
    val horizonFade = (minOf(t, 1f - t) / 0.1f).coerceIn(0f, 1f)
    val color = if (isSun) Color(0xFFFFE2A8) else Color(0xFFE6ECFF)
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxWidth()
            .height(SKY_HEIGHT)
            .graphicsLayer { translationY = -collapse() * 60.dp.toPx() },
    ) {
        val x = size.width * (0.9f - 0.8f * t)
        val y = size.height * 0.66f - size.height * 0.4f * kotlin.math.sin(Math.PI.toFloat() * t)
        val alpha = 0.5f * hide * horizonFade
        val glowRadius = 110.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha * 0.55f), Color.Transparent),
                center = Offset(x, y),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(x, y),
        )
        drawCircle(color.copy(alpha = alpha * 0.7f), radius = (if (isSun) 20.dp else 16.dp).toPx(), center = Offset(x, y))
    }
}

private val LocalRainSurfaces = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateMap<String, RainSurfaceSource>?> { null }

private class RainSurfaceSource(val corner: Float, val mask: (() -> androidx.compose.ui.geometry.Rect)?) {
    var coordinates: androidx.compose.ui.layout.LayoutCoordinates? = null

    fun resolve(key: String): RainSurface? {
        val c = coordinates?.takeIf { it.isAttached } ?: return null
        val position = c.positionInRoot()
        val local = androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Size(c.size.width.toFloat(), c.size.height.toFloat()))
        val visible = (mask?.invoke()?.intersect(local) ?: local).translate(position)
        if (visible.width <= 0f || visible.height <= 0f) return null
        return RainSurface(key, visible, corner, position.x, local.width)
    }
}

@Composable
private fun Modifier.rainSurface(key: String, corner: androidx.compose.ui.unit.Dp = 28.dp, mask: (() -> androidx.compose.ui.geometry.Rect)? = null): Modifier {
    val registry = LocalRainSurfaces.current ?: return this
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val source = remember(key, cornerPx) { RainSurfaceSource(cornerPx, mask) }
    androidx.compose.runtime.DisposableEffect(key, source) {
        registry[key] = source
        onDispose { registry.remove(key) }
    }
    return this.onGloballyPositioned { source.coordinates = it }
}

private val SIDE_PADDING = 20.dp
private val COLLAPSE_RANGE = 520.dp
private const val COMBINE_AT = 0.9f
private const val HEADER_EXTRAS_DP = 170f

@Composable
private fun LocationChip(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier) {
    val place = listOf(snapshot.locationName, snapshot.region).filter { it.isNotBlank() }.joinToString(", ")
    if (place.isBlank()) return
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(place, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
        leadingIcon = { Icon(painterResource(R.drawable.rounded_location_on_24), null, modifier = Modifier.size(20.dp)) },
        shape = CircleShape,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = palette.card,
            labelColor = palette.onBase,
            leadingIconContentColor = palette.accent,
        ),
    )
}

internal fun Modifier.foldAway(fraction: Float): Modifier =
    this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, (placeable.height * (1f - fraction)).roundToInt()) { placeable.placeRelative(0, 0) }
        }
        .graphicsLayer { alpha = (1f - fraction * 1.4f).coerceIn(0f, 1f) }

private val temperatureFonts = java.util.concurrent.ConcurrentHashMap<Int, FontFamily>()

@OptIn(ExperimentalTextApi::class)
internal fun temperatureFont(widthAxis: Int, weightAxis: Int): FontFamily =
    temperatureFonts.getOrPut(widthAxis * 10_000 + weightAxis) {
        FontFamily(
            Font(
                R.font.google_sans_flex,
                weight = FontWeight(weightAxis),
                variationSettings = FontVariation.Settings(
                    FontVariation.width(widthAxis.toFloat()),
                    FontVariation.weight(weightAxis),
                    FontVariation.Setting("ROND", 100f),
                ),
            ),
        )
    }

@Composable
private fun Header(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, progress: Float, modifier: Modifier) {
    val number = WeatherFormat.temperature(snapshot.tempC, unit).removeSuffix("°")
    
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val fontScale = LocalDensity.current.fontScale
    
    val heightCap = ((screenHeightDp * 0.6f - HEADER_EXTRAS_DP) / 0.86f / fontScale).coerceAtLeast(64f)
    val expanded = minOf(if (number.length >= 3) 290f else 390f, heightCap)
    val size = lerp(expanded, 44f, progress).sp
    
    val font = temperatureFont(lerp(52f, 125f, progress).roundToInt(), lerp(1f, 800f, progress).roundToInt())
    val hazeStrength = ((snapshot.tempC - 30.0) / 12.0).toFloat().coerceIn(0f, 1f) * (1f - progress * 5f).coerceIn(0f, 1f)
    var combined by remember { mutableStateOf(false) }
    if (progress >= COMBINE_AT) combined = true else if (progress < COMBINE_AT - 0.08f) combined = false
    val morph by animateFloatAsState(if (combined) 1f else 0f, tween(320), label = "weatherHeaderMorph")
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(lerp(20f, 0f, progress).dp))
        TemperatureAndCondition(
            progress = morph,
            gap = lerp(4f, 16f, morph).dp,
            digits = {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.heatHaze(hazeStrength)) {
                    DegreeSign(size, font, Color.Transparent)
                    Text(
                        number,
                        color = palette.onBase,
                        fontFamily = font,
                        fontSize = size,
                        lineHeight = size * 0.86f,
                        letterSpacing = lerp(-14f, -1f, progress).sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                    DegreeSign(size, font, palette.onBase)
                }
            },
            condition = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        painterResource(WeatherFormat.icon(snapshot.condition, snapshot.isDay)),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(snapshot.conditionText, color = palette.onBase, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
        )
        Column(Modifier.foldAway((progress / 0.5f).coerceIn(0f, 1f))) {
            Spacer(Modifier.height(6.dp))
            Text(
                listOf(
                    stringResource(R.string.weather_high_low, WeatherFormat.temperature(snapshot.highC, unit), WeatherFormat.temperature(snapshot.lowC, unit)),
                    stringResource(R.string.weather_feels_like, WeatherFormat.temperature(snapshot.feelsLikeC, unit)),
                ).joinToString(" · "),
                color = palette.onBaseMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun TemperatureAndCondition(
    progress: Float,
    gap: androidx.compose.ui.unit.Dp,
    digits: @Composable () -> Unit,
    condition: @Composable () -> Unit,
) {
    Layout(content = { digits(); condition() }) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val d = measurables[0].measure(loose)
        val cMax = lerp(constraints.maxWidth.toFloat(), (constraints.maxWidth - d.width - gapPx).coerceAtLeast(0).toFloat(), progress).roundToInt()
        val c = measurables[1].measure(loose.copy(maxWidth = cMax))
        val width = constraints.maxWidth
        val expandedHeight = d.height + gapPx + c.height
        val collapsedHeight = maxOf(d.height, c.height)
        val height = lerp(expandedHeight.toFloat(), collapsedHeight.toFloat(), progress).roundToInt()
        val total = d.width + gapPx + c.width
        val collapsedStart = (width - total) / 2
        layout(width, height) {
            val dx = lerp(((width - d.width) / 2).toFloat(), collapsedStart.toFloat(), progress).roundToInt()
            val dy = lerp(0f, ((collapsedHeight - d.height) / 2).toFloat(), progress).roundToInt()
            val cx = lerp(((width - c.width) / 2).toFloat(), (collapsedStart + d.width + gapPx).toFloat(), progress).roundToInt()
            val cy = lerp((d.height + gapPx).toFloat(), ((collapsedHeight - c.height) / 2).toFloat(), progress).roundToInt()
            d.placeRelative(dx, dy)
            c.placeRelative(cx, cy)
        }
    }
}

@Composable
private fun DegreeSign(digitSize: androidx.compose.ui.unit.TextUnit, font: FontFamily, color: Color) {
    val size = digitSize * 0.42f
    Text(
        "°",
        color = color,
        fontFamily = font,
        fontSize = size,
        lineHeight = size,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
private fun AlertCard(alert: WeatherAlert, palette: WeatherPalette, modifier: Modifier) {
    val context = LocalContext.current
    val alertColor = MaterialTheme.colorScheme.error
    Column(
        modifier
            .rainSurface("alert:${alert.id}")
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(alertColor.copy(alpha = 0.22f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.rounded_warning_24), null, tint = alertColor, modifier = Modifier.size(24.dp))
            Text(alert.event, color = palette.onBase, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        alert.expiresMillis?.let {
            Text(stringResource(R.string.weather_alert_until, formatTime(context, it)), color = palette.onBaseMuted, style = MaterialTheme.typography.bodyMedium)
        }
        if (alert.headline != alert.event) Text(alert.headline, color = palette.onBase, style = MaterialTheme.typography.bodyMedium)
        if (alert.description.isNotBlank()) Text(alert.description, color = palette.onBaseMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourlySection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette) {
    if (snapshot.hourly.isEmpty()) return
    val context = LocalContext.current
    val hours = snapshot.hourly
    val carouselState = rememberCarouselState { hours.size }
    Column {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 104.dp,
            itemSpacing = 4.dp,
            contentPadding = PaddingValues(horizontal = SIDE_PADDING),
            modifier = Modifier.fillMaxWidth().height(176.dp),
        ) { index ->
            val hour = hours[index]
            Column(
                Modifier
                    .rainSurface("hourly:$index", mask = { carouselItemDrawInfo.maskRect })
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .background(palette.card)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatHour(context, hour.timeMillis), color = palette.onBaseMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                Icon(
                    painterResource(WeatherFormat.icon(hour.condition, hour.isDay)),
                    null,
                    tint = palette.accent,
                    modifier = Modifier.size(32.dp),
                )
                Text(WeatherFormat.temperature(hour.tempC, unit), color = palette.onBase, style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                Text(
                    if (hour.chanceOfRain > 0) "${hour.chanceOfRain}%" else " ",
                    color = palette.accent,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun DailySection(days: List<DailyForecast>, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val low = days.minOf { it.lowC }
    val high = days.maxOf { it.highC }
    val span = (high - low).coerceAtLeast(1.0)
    Column(modifier) {
        RoundedCardContainer(modifier = Modifier.rainSurface("daily"), spacing = 2.dp, cornerRadius = 28.dp) {
            days.forEachIndexed { index, day ->
                Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            if (index == 0) stringResource(R.string.weather_detail_today) else dayName(day.dayMillis),
                            color = palette.onBase,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(64.dp),
                            maxLines = 1,
                        )
                        Icon(painterResource(WeatherFormat.icon(day.condition, true)), null, tint = palette.accent, modifier = Modifier.size(26.dp))
                        Text(
                            if (day.chanceOfRain > 0) "${day.chanceOfRain}%" else "",
                            color = palette.accent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(36.dp),
                        )
                        Text(WeatherFormat.temperature(day.lowC, unit), color = palette.onBaseMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                            val start = ((day.lowC - low) / span).toFloat().coerceIn(0f, 1f)
                            val end = ((day.highC - low) / span).toFloat().coerceIn(start, 1f)
                            Row(Modifier.matchParentSize()) {
                                if (start > 0f) Spacer(Modifier.weight(start))
                                Box(Modifier.weight((end - start).coerceAtLeast(0.05f)).fillMaxHeight().clip(CircleShape).background(palette.accent))
                                if (end < 1f) Spacer(Modifier.weight(1f - end))
                            }
                        }
                        Text(WeatherFormat.temperature(day.highC, unit), color = palette.onBase, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp))
                    }
                }
            }
        }
    }
}

private class Detail(val icon: Int, val label: Int, val value: String)

@Composable
private fun DetailsSection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val imperial = unit == TemperatureUnit.FAHRENHEIT
    val extras = snapshot.extras
    val details = buildList {
        add(Detail(R.drawable.rounded_water_drop_24, R.string.weather_detail_humidity, "${snapshot.humidity}%"))
        add(
            Detail(
                R.drawable.rounded_air_24,
                R.string.weather_detail_wind,
                WeatherFormat.wind(snapshot.windKph, unit) + (extras?.windDirectionDeg?.let { " ${compass(it)}" } ?: ""),
            ),
        )
        extras?.windGustKph?.let { add(Detail(R.drawable.rounded_air_24, R.string.weather_detail_gusts, WeatherFormat.wind(it, unit))) }
        add(Detail(R.drawable.rounded_rainy_24, R.string.weather_detail_rain_chance, "${snapshot.chanceOfRain}%"))
        extras?.precipitationMm?.let {
            add(Detail(R.drawable.rounded_rainy_24, R.string.weather_detail_precipitation, if (imperial) "%.2f in".format(Locale.US, it / 25.4) else "%.1f mm".format(Locale.US, it)))
        }
        extras?.uvIndex?.let { add(Detail(R.drawable.rounded_wb_sunny_24, R.string.weather_detail_uv, it.roundToInt().toString())) }
        extras?.pressureHpa?.let {
            add(Detail(R.drawable.rounded_cloud_24, R.string.weather_detail_pressure, if (imperial) "%.2f inHg".format(Locale.US, it * 0.02953) else "${it.roundToInt()} hPa"))
        }
        extras?.visibilityKm?.let {
            add(Detail(R.drawable.rounded_visibility_24, R.string.weather_detail_visibility, if (imperial) "%.1f mi".format(Locale.US, it / 1.609344) else "%.1f km".format(Locale.US, it)))
        }
        extras?.dewPointC?.let { add(Detail(R.drawable.rounded_water_drop_24, R.string.weather_detail_dew_point, WeatherFormat.temperature(it, unit))) }
        extras?.cloudCover?.let { add(Detail(R.drawable.rounded_cloud_24, R.string.weather_detail_cloud_cover, "$it%")) }
    }
    Column(modifier) {
        Surface(color = palette.card, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.rainSurface("details").fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                details.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { DetailTile(it, palette, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTile(detail: Detail, palette: WeatherPalette, modifier: Modifier) {
    Row(
        modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(detail.icon), null, tint = palette.accent, modifier = Modifier.size(26.dp))
        Column {
            Text(stringResource(detail.label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(detail.value, color = palette.onBase, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SunSection(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier) {
    val rise = snapshot.extras?.sunriseMillis ?: return
    val set = snapshot.extras?.sunsetMillis ?: return
    val context = LocalContext.current
    Column(modifier) {
        RoundedCardContainer(modifier = Modifier.rainSurface("sun"), spacing = 2.dp, cornerRadius = 28.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SunTile(R.string.weather_detail_sunrise, formatTime(context, rise), palette, Modifier.weight(1f))
                SunTile(R.string.weather_detail_sunset, formatTime(context, set), palette, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SunTile(label: Int, time: String, palette: WeatherPalette, modifier: Modifier) {
    Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(time, color = palette.onBase, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Footer(
    modifier: Modifier,
    snapshot: WeatherSnapshot,
    loading: Boolean,
    error: WeatherError?,
    palette: WeatherPalette,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(snapshot.updatedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val age = DateUtils.getRelativeTimeSpanString(
        snapshot.updatedAt,
        maxOf(now, snapshot.updatedAt),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = error?.let { errorLabel(context, it) } ?: "${WeatherProviders.byId(snapshot.providerId).displayName} - $age",
            color = palette.onBaseMuted,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (loading) {
            LoadingIndicator(Modifier.size(28.dp))
        } else {
            Icon(
                painterResource(R.drawable.rounded_refresh_24),
                contentDescription = null,
                tint = palette.onBase,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onRefresh).padding(8.dp).size(20.dp),
            )
        }
    }
}

private fun errorLabel(context: Context, error: WeatherError?): String {
    val base = context.getString(
        when (error) {
            WeatherError.MissingApiKey -> R.string.weather_error_missing_key
            WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
            WeatherError.LocationPermission -> R.string.weather_error_location_permission
            WeatherError.NoLocation -> R.string.weather_error_no_location
            WeatherError.Network -> R.string.weather_error_network
            is WeatherError.Unknown, null -> R.string.weather_error_unknown
        },
    )
    val detail = (error as? WeatherError.Unknown)?.message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$base: $detail" else base
}

private fun compass(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[(((degrees % 360 + 360) % 360) / 45.0).roundToInt() % 8]
}

private fun dayName(millis: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))

private fun formatTime(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}
