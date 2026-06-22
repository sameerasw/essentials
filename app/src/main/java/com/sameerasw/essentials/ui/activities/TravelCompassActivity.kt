package com.sameerasw.essentials.ui.activities

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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.toPath
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.ui.theme.EssentialsTheme
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
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) { viewModel.check(context) }
            val isPitchBlack by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlack) {
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

    var currentTime by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
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
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Pure black background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black)
        }

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

        // Arrow shape — center, rotated to destination
        ArrowShapeCanvas(
            rotationDegrees = animatedRotation,
            color = primaryColor,
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
        )

        // Distance — bottom center
        Text(
            text = distanceText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = primaryColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

@Composable
private fun ArrowShapeCanvas(
    rotationDegrees: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = rotationDegrees
        }
    ) {
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
            color = color,
            style = Stroke(width = 6.dp.toPx())
        )
    }
}
