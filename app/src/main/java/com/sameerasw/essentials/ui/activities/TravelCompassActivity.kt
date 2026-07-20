package com.sameerasw.essentials.ui.activities

import android.content.res.Configuration
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.graphics.shapes.toPath
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.min

class TravelCompassActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private val _azimuth = mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Make activity truly full screen (hide status/navigation bars, fit system windows false, allow display cutout)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) { viewModel.check(context) }
            val isPitchBlack by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(darkTheme = true, pitchBlackTheme = isPitchBlack) {
                CompassScreen(
                    azimuth = _azimuth.floatValue,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val rotMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)

        // Check if device is upright (Z axis is near horizontal, rotMatrix[8] represents Z-axis projection on world Z)
        val isUpright = kotlin.math.abs(rotMatrix[8]) < 0.5f
        val remappedMatrix = FloatArray(9)
        if (isUpright) {
            // Remap coordinates so screen Y (upward) behaves like the compass direction pointing towards North/camera view
            SensorManager.remapCoordinateSystem(
                rotMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )
        } else {
            System.arraycopy(rotMatrix, 0, remappedMatrix, 0, 9)
        }

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remappedMatrix, orientation)
        // orientation[0] is azimuth in radians
        val azimuthDeg = ((orientation[0] * 180f / PI) + 360f) % 360f
        _azimuth.floatValue = azimuthDeg.toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun CompassScreen(
    azimuth: Float,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LocationReachedRepository(context) }
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }
    val is24h = remember { DateFormat.is24HourFormat(context) }
    val sharedPrefs = remember {
        context.getSharedPreferences(
            "essentials_prefs",
            android.content.Context.MODE_PRIVATE
        )
    }
    var useIcon by remember {
        mutableStateOf(
            sharedPrefs.getBoolean(
                "location_reached_compass_use_icon",
                false
            )
        )
    }

    var currentTime by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
    var remainingTimeText by remember { mutableStateOf("") }
    var destinationName by remember { mutableStateOf("") }
    // bearing from North to destination
    var destinationBearing by remember { mutableFloatStateOf(0f) }

    // Refresh location + time every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentTime = if (is24h) {
                "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else {
                val hour = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
                val min = cal.get(Calendar.MINUTE)
                "%02d:%02d".format(hour, min)
            }

            val activeId = repository.getActiveAlarmId()
            val alarm = repository.getAlarms().find { it.id == activeId }
            destinationName = alarm?.name ?: ""
            @Suppress("MissingPermission")
            alarm?.let { dest ->
                fusedLocation.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        loc?.let {
                            val results = FloatArray(2)
                            android.location.Location.distanceBetween(
                                it.latitude, it.longitude,
                                dest.latitude, dest.longitude,
                                results
                            )
                            val distM = results[0]
                            distanceText = if (distM < 1000f) {
                                "${distM.toInt()} m"
                            } else {
                                "%.1f km".format(distM / 1000f)
                            }
                            destinationBearing = it.bearingTo(
                                android.location.Location("").also { l ->
                                    l.latitude = dest.latitude
                                    l.longitude = dest.longitude
                                }
                            ).let { b -> (b + 360f) % 360f }

                            // Calculate remaining time (ETA)
                            val startDist = repository.getStartDistance()
                            val startTime = repository.getStartTime()
                            if (startDist > 0f && startTime > 0L) {
                                val elapsed = System.currentTimeMillis() - startTime
                                val travelled = startDist - distM
                                if (travelled > 0f && elapsed > 0L) {
                                    val remainingMillis = (distM * elapsed / travelled).toLong()
                                    val mins = (remainingMillis / 60000).toInt().coerceAtLeast(1)
                                    remainingTimeText = if (mins >= 60) {
                                        val hrs = mins / 60
                                        val rMins = mins % 60
                                        if (rMins > 0) "${hrs}h ${rMins}m remaining" else "${hrs}h remaining"
                                    } else {
                                        "${mins}m remaining"
                                    }
                                } else {
                                    remainingTimeText = ""
                                }
                            } else {
                                remainingTimeText = ""
                            }
                        }
                    }
            }
            delay(5000)
        }
    }

    // Continuous target angle to avoid long-way-round spinning animation when wrapping around 0/360
    var continuousTargetRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(azimuth, destinationBearing) {
        val target = ((destinationBearing - azimuth + 360f) % 360f)
        var diff = target - (continuousTargetRotation % 360f)
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        continuousTargetRotation += diff
    }

    val animatedRotation by animateFloatAsState(
        targetValue = continuousTargetRotation,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "arrowRotation"
    )

    // Normalize for haptics to the 0..360 range
    val normalizedRotation = ((continuousTargetRotation % 360f) + 360f) % 360f

    // Compass haptic feedback
    val view = LocalView.current
    val minorStep = 3f // Tick every 3 degrees
    var lastMinorNotch by remember { mutableStateOf(0) }
    var wasAligned by remember { mutableStateOf(false) }

    LaunchedEffect(normalizedRotation) {
        val isAligned = normalizedRotation < 3f || normalizedRotation > 357f
        if (isAligned && !wasAligned) {
            HapticUtil.performHeavyHaptic(view)
            wasAligned = true
        } else if (!isAligned) {
            wasAligned = false
        }

        val currentMinorNotch = kotlin.math.round(normalizedRotation / minorStep).toInt()
        if (currentMinorNotch != lastMinorNotch) {
            if (!isAligned) {
                HapticUtil.performMicroHaptic(view)
            }
            lastMinorNotch = currentMinorNotch
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val pagerState = rememberPagerState(
        initialPage = sharedPrefs.getInt("location_reached_compass_preset", 0),
        pageCount = { 3 }
    )

    LaunchedEffect(pagerState.currentPage) {
        sharedPrefs.edit().putInt("location_reached_compass_preset", pagerState.currentPage).apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onDismiss() },
                    onLongPress = {
                        HapticUtil.performHeavyHaptic(view)
                        useIcon = !useIcon
                        sharedPrefs.edit().putBoolean("location_reached_compass_use_icon", useIcon)
                            .apply()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Pure black background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val isLandscape =
                    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val arrowSize = if (isLandscape) 180.dp else 220.dp

                when (page) {
                    0 -> { // Default
                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentTime,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = distanceText,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryColor
                                    )
                                    if (remainingTimeText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = remainingTimeText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Light,
                                            color = primaryColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                CompassArrowContainer(
                                    rotationDegrees = animatedRotation,
                                    useIcon = useIcon,
                                    color = primaryColor,
                                    modifier = Modifier.size(arrowSize)
                                )
                            }
                        } else {
                            // Time — top center
                            Text(
                                text = currentTime,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = primaryColor,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 80.dp)
                            )

                            // Arrow container (shape or drawable) — center, rotated to destination
                            CompassArrowContainer(
                                rotationDegrees = animatedRotation,
                                useIcon = useIcon,
                                color = primaryColor,
                                modifier = Modifier
                                    .size(arrowSize)
                                    .align(Alignment.Center)
                            )

                            // Bottom Layout
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = distanceText,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryColor
                                )
                                if (remainingTimeText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = remainingTimeText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Light,
                                        color = primaryColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    1 -> { // Minimal
                        CompassArrowContainer(
                            rotationDegrees = animatedRotation,
                            useIcon = useIcon,
                            color = primaryColor,
                            modifier = Modifier
                                .size(arrowSize)
                                .align(Alignment.Center)
                        )
                    }

                    2 -> { // Most Details
                        if (isLandscape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentTime,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (destinationName.isNotEmpty()) {
                                        Text(
                                            text = destinationName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = primaryColor.copy(alpha = 0.9f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = distanceText,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryColor
                                    )
                                    if (remainingTimeText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = remainingTimeText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Light,
                                            color = primaryColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                CompassArrowContainer(
                                    rotationDegrees = animatedRotation,
                                    useIcon = useIcon,
                                    color = primaryColor,
                                    modifier = Modifier.size(arrowSize)
                                )
                            }
                        } else {
                            // Time — top center
                            Text(
                                text = currentTime,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = primaryColor,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 80.dp)
                            )

                            // Arrow container (shape or drawable) — center, rotated to destination
                            CompassArrowContainer(
                                rotationDegrees = animatedRotation,
                                useIcon = useIcon,
                                color = primaryColor,
                                modifier = Modifier
                                    .size(arrowSize)
                                    .align(Alignment.Center)
                            )

                            // Bottom Layout
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (destinationName.isNotEmpty()) {
                                    Text(
                                        text = destinationName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = primaryColor.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = distanceText,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryColor
                                )
                                if (remainingTimeText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = remainingTimeText,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Light,
                                        color = primaryColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Map Button
        androidx.compose.material3.OutlinedIconButton(
            onClick = {
                HapticUtil.performUIHaptic(view)
                val intent = Intent(context, com.sameerasw.essentials.FeatureSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("feature", "Location reached")
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_map_24),
                contentDescription = "Map Settings",
                tint = primaryColor
            )
        }
    }
}

@Composable
private fun CompassArrowContainer(
    rotationDegrees: Float,
    useIcon: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shapeAlpha by animateFloatAsState(
        targetValue = if (useIcon) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "shapeAlpha"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (useIcon) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "iconAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotationDegrees
        },
        contentAlignment = Alignment.Center
    ) {
        if (shapeAlpha > 0.01f) {
            ArrowShapeCanvas(
                color = color,
                alpha = shapeAlpha,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (iconAlpha > 0.01f) {
            Icon(
                painter = painterResource(id = R.drawable.round_arrow_upward_24),
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = iconAlpha }
            )
        }
    }
}

@Composable
private fun ArrowShapeCanvas(
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val size = min(this.size.width, this.size.height)
        val polygon = androidx.compose.material3.MaterialShapes.Arrow

        // Convert MaterialShapes polygon to Android Path, scaled to canvas size
        val rawPath = polygon.toPath()
        val androidPath = android.graphics.Path()
        androidPath.set(rawPath)

        // The polygon is in [0,1] space — scale to our canvas size and center it
        val matrix = android.graphics.Matrix()
        matrix.postScale(size, size)
        // Polygon is drawn from origin; offset to center in the canvas
        val offsetX = (this.size.width - size) / 2f
        val offsetY = (this.size.height - size) / 2f
        matrix.postTranslate(offsetX, offsetY)
        androidPath.transform(matrix)

        val composePath = Path()
        composePath.addPath(androidPath.asComposePath())

        drawPath(
            path = composePath,
            color = color.copy(alpha = alpha),
            style = Stroke(width = 6.dp.toPx())
        )
    }
}
