package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.GoogleSansFlex
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.BluetoothBatteryUtils
import android.os.BatteryManager
import android.content.Context
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private enum class DragState {
    Expanded, Partial, Dismissed
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EssentialHubContent(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onProgressChanged: (Float) -> Unit = {}
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    var clockWidth by remember { mutableFloatStateOf(0f) }
    var clockHeight by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance().time
            delay(1000)
        }
    }

    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormatPattern = if (is24Hour) "HH:mm" else "hh:mm"
    val timeFormat = remember(is24Hour) { SimpleDateFormat(timeFormatPattern, Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val density = LocalDensity.current
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = DragState.Partial,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
            confirmValueChange = { newValue: DragState ->
                if (newValue == DragState.Dismissed) {
                    onDismiss()
                }
                true
            }
        )
    }

    LaunchedEffect(dragState.offset) {
        if (!dragState.offset.isNaN()) {
            val anchors = dragState.anchors
            val partialPos = anchors.positionOf(DragState.Partial)
            val dismissedPos = anchors.positionOf(DragState.Dismissed)
            
            if (dismissedPos > partialPos) {
                val progress = ((dragState.offset - partialPos) / (dismissedPos - partialPos)).coerceIn(0f, 1f)
                onProgressChanged(progress)
            }
        }
    }

    val statusBarHeightPx = remember(density) {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (id > 0) context.resources.getDimensionPixelSize(id).toFloat() else 0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        val screenWidth = constraints.maxWidth.toFloat()

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeOut(animationSpec = tween(300))
        ) {
            val offset = if (dragState.offset.isNaN()) screenHeight else dragState.offset
            val anchors = dragState.anchors
            val partialPos = if (anchors.size > 0) anchors.positionOf(DragState.Partial) else screenHeight * 0.25f
            val expandedPos = if (anchors.size > 0) anchors.positionOf(DragState.Expanded) else statusBarHeightPx
            
            val eProgress = if (partialPos != expandedPos) {
                ((partialPos - offset) / (partialPos - expandedPos)).coerceIn(0f, 1f)
            } else 0f

            Box(modifier = Modifier.fillMaxSize()) {
                val dismissalProgress = remember(dragState.offset) {
                    if (!dragState.offset.isNaN()) {
                        val anchors = dragState.anchors
                        val partialPos = try { anchors.positionOf(DragState.Partial) } catch (e: Exception) { 0f }
                        val dismissedPos = try { anchors.positionOf(DragState.Dismissed) } catch (e: Exception) { 0f }
                        if (dismissedPos > partialPos) {
                            ((dragState.offset - partialPos) / (dismissedPos - partialPos)).coerceIn(0f, 1f)
                        } else 0f
                    } else 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = (0.4f * (1f - dismissalProgress)).coerceIn(0f, 1f)
                            )
                        )
                )

                // Interactive Clock Layer
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                        .onSizeChanged { size ->
                            clockWidth = size.width.toFloat()
                            clockHeight = size.height.toFloat()
                        }
                        .graphicsLayer {
                            val targetScale = 24f / 110f
                            val currentScale = 1f - (eProgress * (1f - targetScale))
                            scaleX = currentScale
                            scaleY = currentScale
                            
                            val paddingPx = with(density) { 16.dp.toPx() }
                            val handlePaddingPx = with(density) { 32.dp.toPx() }
                            
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 0f)

                            val startX = (screenWidth - clockWidth) / 2f
                            val endX = paddingPx
                            
                           val startY = offset - handlePaddingPx - clockHeight
                            val currentClockHeight = if (clockHeight > 0) clockHeight * currentScale else 0f
                            val targetClockHeight = if (clockHeight > 0) clockHeight * targetScale else 0f
                            val endY = (statusBarHeightPx - targetClockHeight) / 2f

                            translationX = lerp(startX, endX, eProgress)
                            translationY = lerp(startY, endY, eProgress)
                        }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Date (Fades out)
                        Text(
                            text = dateFormat.format(currentTime),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Light,
                            fontFamily = GoogleSansFlex,
                            letterSpacing = 2.sp,
                            modifier = Modifier.graphicsLayer {
                                alpha = (1f - eProgress).coerceIn(0f, 1f)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time
                        Text(
                            text = timeFormat.format(currentTime),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 110.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = GoogleSansFlex,
                                letterSpacing = (-4).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Sheet Layer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .onSizeChanged { size ->
                            val currentAnchors = DraggableAnchors {
                                DragState.Expanded at statusBarHeightPx
                                DragState.Partial at size.height * 0.25f
                                DragState.Dismissed at size.height.toFloat()
                            }
                            dragState.updateAnchors(currentAnchors)
                        }
                        .offset {
                            IntOffset(
                                x = 0,
                                y = offset.roundToInt().coerceAtLeast(0)
                            )
                        }
                        .anchoredDraggable(dragState, Orientation.Vertical)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column {
                            BatteryWidgetCard()
                        }
                    }
                }
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

@Composable
fun BatteryWidgetCard(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    
    var androidLevel by remember { mutableIntStateOf(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)) }
    var isAndroidCharging by remember { 
        mutableStateOf(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING) 
    }
    var bluetoothDevices by remember { mutableStateOf(BluetoothBatteryUtils.getPairedDevicesBattery(context)) }

    // Refresh every minute
    LaunchedEffect(Unit) {
        while (true) {
            androidLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            isAndroidCharging = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
            bluetoothDevices = BluetoothBatteryUtils.getPairedDevicesBattery(context)
            delay(60000)
        }
    }
    
    val items = remember(androidLevel, isAndroidCharging, viewModel.macBatteryLevel.intValue, viewModel.isMacConnected.value, bluetoothDevices) {
        mutableListOf<BatteryData>().apply {
            // Android
            add(BatteryData(context.getString(R.string.icon_battery), androidLevel, R.drawable.rounded_mobile_24, isAndroidCharging))
            
            // Mac
            if (viewModel.isMacConnected.value && viewModel.macBatteryLevel.intValue != -1) {
                add(BatteryData(context.getString(R.string.app_airsync), viewModel.macBatteryLevel.intValue, R.drawable.rounded_laptop_mac_24, viewModel.isMacBatteryCharging.value))
            } else if (viewModel.macBatteryLevel.intValue != -1) {
                if (viewModel.isAirSyncConnectionEnabled.value) {
                    add(BatteryData(context.getString(R.string.app_airsync), viewModel.macBatteryLevel.intValue, R.drawable.rounded_laptop_mac_24, viewModel.isMacBatteryCharging.value))
                }
            }
            
            // Bluetooth
            bluetoothDevices.take(viewModel.batteryWidgetMaxDevices.intValue - size).forEach { device ->
                val icon = when {
                    device.name.contains("watch", true) -> R.drawable.rounded_watch_24
                    device.name.contains("bud", true) || device.name.contains("pod", true) || 
                    device.name.contains("head", true) -> R.drawable.rounded_headphones_24
                    else -> R.drawable.rounded_bluetooth_24
                }
                add(BatteryData(device.name.split(" ").firstOrNull() ?: "BT", device.level, icon, false))
            }
        }
    }

    RoundedCardContainer(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BatteryItem(item)
            }
        }
    }
}

data class BatteryData(
    val name: String,
    val level: Int,
    val icon: Int,
    val isCharging: Boolean
)

@Composable
fun BatteryItem(data: BatteryData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            // Progress Ring
            BatteryRing(
                progress = data.level / 100f,
                isCharging = data.isCharging,
                modifier = Modifier.fillMaxSize()
            )
            
            // Icon
            Icon(
                painter = painterResource(id = data.icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (data.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BatteryRing(
    progress: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    
    val ringColor = when {
        isCharging -> primaryColor
        progress <= 0.15f -> errorColor
        else -> primaryColor
    }

    Box(modifier = modifier.padding(4.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            
            // Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Charging Indicator Bubble
            if (isCharging) {
                val bubbleRadius = 8.dp.toPx()
                val angleRad = Math.toRadians(-90.0).toFloat()
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = (size.width / 2)
                
                val bubbleX = centerX + radius * Math.cos(angleRad.toDouble()).toFloat()
                val bubbleY = centerY + radius * Math.sin(angleRad.toDouble()).toFloat()
                
                drawCircle(
                    color = ringColor,
                    radius = bubbleRadius,
                    center = androidx.compose.ui.geometry.Offset(bubbleX, bubbleY)
                )
            }
        }
        
        if (isCharging) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_flash_on_24),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .size(16.dp),
                tint = surfaceColor
            )
        }
    }
}
