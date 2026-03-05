package com.sameerasw.essentials.ui.composables

import android.content.Intent
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

enum class OnboardingStep {
    WELCOME,
    ACKNOWLEDGEMENT,
    FEATURE_INTRODUCTION
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    onBeginClick: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val rotationAnimatable = remember { Animatable(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var hasTriggeredEasterEgg by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                // Professional right-to-left push animation
                (slideInHorizontally { it } + fadeIn(tween(400)))
                    .togetherWith(slideOutHorizontally { -it } + fadeOut(tween(400)))
            },
            label = "OnboardingTransition"
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> {
                        WelcomeStepContent(
                            rotationAnimatable = rotationAnimatable,
                            center = center,
                            onCenterChanged = { center = it },
                            hasTriggeredEasterEgg = hasTriggeredEasterEgg,
                            onEasterEggTriggered = { hasTriggeredEasterEgg = true },
                            onNext = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.ACKNOWLEDGEMENT
                            }
                        )
                    }

                    OnboardingStep.ACKNOWLEDGEMENT -> {
                        AcknowledgementStepContent(
                            onNext = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.FEATURE_INTRODUCTION
                            }
                        )
                    }

                    OnboardingStep.FEATURE_INTRODUCTION -> {
                        FeatureIntroStepContent(
                            onFinish = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onBeginClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStepContent(
    rotationAnimatable: Animatable<Float, *>,
    center: Offset,
    onCenterChanged: (Offset) -> Unit,
    hasTriggeredEasterEgg: Boolean,
    onEasterEggTriggered: () -> Unit,
    onNext: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                    onCenterChanged(Offset(it.width / 2f, it.height / 2f))
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

                            // Easter Egg logic
                            if (!hasTriggeredEasterEgg && kotlin.math.abs(currentRotation) >= 3600f) {
                                onEasterEggTriggered()
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

        Button(
            onClick = onNext,
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

@Composable
fun AcknowledgementStepContent(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Acknowledgement",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "This app is a collection of utilities that can interact deeply with your device system. Using some features might modify system settings or behavior in unexpected ways. \n\nYou only need to grant necessary permissions which are required for selected features you are using giving you full control over the app's behavior. \n\nFurther more, the app does not track or store any of your personal data, I don't need them... Keep to yourself safe. You can refer to the source code for more information. \n\nThis app is fully open source and is and always will be free to use. Do not pay or install from unknown sources.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "WARNING: Proceed with caution. The developer takes no responsibility for any system instability, data loss, or other issues caused by the use of this app. By proceeding, you acknowledge these risks.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "I know you didn't even read this carefully but, in case you need any help, feel free to reach out the developer or the community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "I Understand",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FeatureIntroStepContent(onFinish: () -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What is this?",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Anytime you are clueless on a feature or a Quick Settings Tile on what it does and what permissions may necessary for it, just long press it and pick 'What is this?' to learn more.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can report bugs or find helpful guides anytime in the app settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GifItem(
                modifier = Modifier.weight(1f),
                imageLoader = imageLoader,
                gifResId = R.drawable.feature_help
            )
            GifItem(
                modifier = Modifier.weight(1f),
                imageLoader = imageLoader,
                gifResId = R.drawable.tile_help
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Let Me in Already",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GifItem(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    gifResId: Int
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(gifResId)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
