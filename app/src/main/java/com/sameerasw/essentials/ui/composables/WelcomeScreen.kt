package com.sameerasw.essentials.ui.composables

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    onBeginClick: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rotationAnimatable = remember { Animatable(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var hasTriggeredEasterEgg by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .onSizeChanged {
                            center = Offset(it.width / 2f, it.height / 2f)
                        }
                        .pointerInput(Unit) {
                            val majorStep = 60f
                            val minorStep = 2f

                            var currentRotation = 0f
                            var lastMajorNotch = 0
                            var lastMinorNotch = 0

                            detectDragGestures(
                                onDragStart = {
                                    scope.launch { rotationAnimatable.stop() }
                                    currentRotation = rotationAnimatable.value
                                    lastMajorNotch = kotlin.math.round(currentRotation / majorStep).toInt()
                                    lastMinorNotch = kotlin.math.round(currentRotation / minorStep).toInt()
                                },
                                onDrag = { change, _ ->
                                    val oldAngle = atan2(
                                        change.previousPosition.y - center.y,
                                        change.previousPosition.x - center.x
                                    )
                                    val newAngle = atan2(
                                        change.position.y - center.y,
                                        change.position.x - center.x
                                    )
                                    var delta = (newAngle - oldAngle) * 180 / PI
                                    
                                    if (delta > 180) delta -= 360
                                    if (delta < -180) delta += 360
                                    
                                    currentRotation += delta.toFloat()
                                    
                                    // I wonder what this does (｀∇´)
                                    if (!hasTriggeredEasterEgg && kotlin.math.abs(currentRotation) >= 3600f) {
                                        hasTriggeredEasterEgg = true
                                        val rickRollUrl = "https://youtu.be/dQw4w9WgXcQ"
                                        val intent = Intent(Intent.ACTION_VIEW, rickRollUrl.toUri())
                                        context.startActivity(intent)
                                    }

                                    // Minor notches - Subtle texture only during drag
                                    val currentMinorNotch = kotlin.math.round(currentRotation / minorStep).toInt()
                                    if (currentMinorNotch != lastMinorNotch) {
                                        HapticUtil.performMicroHaptic(view)
                                        lastMinorNotch = currentMinorNotch
                                    }

                                    // Update major notch tracker without triggering haptic
                                    lastMajorNotch = kotlin.math.round(currentRotation / majorStep).toInt()

                                    scope.launch {
                                        rotationAnimatable.snapTo(currentRotation)
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        rotationAnimatable.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) {
                                            // Major notches - Gear heads only during return animation
                                            val currentMajorNotch = kotlin.math.round(value / majorStep).toInt()
                                            if (currentMajorNotch != lastMajorNotch) {
                                                HapticUtil.performMediumHaptic(view)
                                                lastMajorNotch = currentMajorNotch
                                            }
                                        }
                                        currentRotation = 0f
                                        lastMajorNotch = 0
                                        lastMinorNotch = 0
                                    }
                                }
                            )
                        }
                        .graphicsLayer {
                            rotationZ = rotationAnimatable.value
                        },
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Welcome to Essentials",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "A Toolbox for Android Nerds",
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                        .clickable {
                            val websiteUrl = "https://sameerasw.com"
                            val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                            context.startActivity(intent)
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.avatar),
                        contentDescription = "Developer Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "by sameerasw.com",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(0.3f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onBeginClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Let's Begin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_forward_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
