package com.sameerasw.essentials.ui.composables

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.HelpAndGuidesContent
import com.sameerasw.essentials.ui.components.WhatsNewCustomContent
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.CrashReportingPicker
import com.sameerasw.essentials.ui.components.pickers.LanguagePicker
import com.sameerasw.essentials.ui.components.sheets.UpdateBottomSheet
import com.sameerasw.essentials.ui.components.text.SimpleMarkdown
import com.sameerasw.essentials.ui.theme.GoogleSansFlexRounded
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2

enum class OnboardingStep {
    WELCOME,
    ACKNOWLEDGEMENT,
    PREFERENCES,
    FEATURE_INTRODUCTION,
    WHATS_NEW
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WelcomeScreen(
    viewModel: MainViewModel,
    isWhatsNewFlow: Boolean = false,
    onBeginClick: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    val rotationAnimatable = remember { Animatable(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var hasTriggeredEasterEgg by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn(tween(400)))
                        .togetherWith(slideOutHorizontally { -it } + fadeOut(tween(400)))
                } else {
                    (slideInHorizontally { -it } + fadeIn(tween(400)))
                        .togetherWith(slideOutHorizontally { it } + fadeOut(tween(400)))
                }
            },
            label = "OnboardingTransition"
        ) { step ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> {
                        WelcomeStepContent(
                            viewModel = viewModel,
                            isWhatsNewFlow = isWhatsNewFlow,
                            rotationAnimatable = rotationAnimatable,
                            center = center,
                            onCenterChanged = { center = it },
                            hasTriggeredEasterEgg = hasTriggeredEasterEgg,
                            onEasterEggTriggered = { hasTriggeredEasterEgg = true },
                            onNext = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                if (isWhatsNewFlow) {
                                    currentStep = OnboardingStep.WHATS_NEW
                                } else {
                                    currentStep = OnboardingStep.ACKNOWLEDGEMENT
                                }
                            }
                        )
                    }

                    OnboardingStep.ACKNOWLEDGEMENT -> {
                        val sentryMode by viewModel.sentryReportMode
                        AcknowledgementStepContent(
                            sentryMode = sentryMode,
                            onSentryModeSelected = { viewModel.setSentryReportMode(it, context) },
                            onBack = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.WELCOME
                            },
                            onNext = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.PREFERENCES
                            }
                        )
                    }

                    OnboardingStep.PREFERENCES -> {
                        PreferencesStepContent(
                            viewModel = viewModel,
                            onBack = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.ACKNOWLEDGEMENT
                            },
                            onNext = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.FEATURE_INTRODUCTION
                            }
                        )
                    }

                    OnboardingStep.FEATURE_INTRODUCTION -> {
                        FeatureIntroStepContent(
                            onBack = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.PREFERENCES
                            },
                            onFinish = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onBeginClick()
                            }
                        )
                    }

                    OnboardingStep.WHATS_NEW -> {
                        WhatsNewStepContent(
                            viewModel = viewModel,
                            onBack = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                currentStep = OnboardingStep.WELCOME
                            },
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
    viewModel: MainViewModel,
    isWhatsNewFlow: Boolean,
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
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

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
                                lastMajorNotch =
                                    kotlin.math.round(currentRotation / majorStep).toInt()
                                lastMinorNotch =
                                    kotlin.math.round(currentRotation / minorStep).toInt()
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
                                val currentMinorNotch =
                                    kotlin.math.round(currentRotation / minorStep).toInt()
                                if (currentMinorNotch != lastMinorNotch) {
                                    HapticUtil.performMicroHaptic(view)
                                    lastMinorNotch = currentMinorNotch
                                }

                                lastMajorNotch =
                                    kotlin.math.round(currentRotation / majorStep).toInt()

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
                                        val currentMajorNotch =
                                            kotlin.math.round(value / majorStep).toInt()
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
                text = stringResource(if (isWhatsNewFlow) R.string.welcome_back_title else R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.welcome_subtitle),
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
                    text = stringResource(R.string.welcome_developer_attribution),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isWhatsNewFlow) {
                val appLanguage by viewModel.appLanguage
                RoundedCardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LanguagePicker(
                        selectedLanguageCode = appLanguage,
                        onLanguageSelected = { viewModel.setAppLanguage(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .height(56.dp)
        ) {
            Text(
                text = stringResource(if (isWhatsNewFlow) R.string.action_see_whats_new else R.string.action_lets_begin),
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
fun AcknowledgementStepContent(
    sentryMode: String,
    onSentryModeSelected: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.acknowledgement_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.acknowledgement_desc),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.acknowledgement_warning),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))


                Text(
                    text = stringResource(R.string.acknowledgement_footer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RoundedCardContainer {
            CrashReportingPicker(
                selectedMode = sentryMode,
                onModeSelected = onSentryModeSelected
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onBack()
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_i_understand),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureIntroStepContent(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
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
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.action_what_is_this),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.feature_intro_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
            )


            Spacer(modifier = Modifier.height(24.dp))

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isLargeScreen = configuration.screenWidthDp >= 600

            if (isLandscape || isLargeScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GifItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        imageLoader = imageLoader,
                        gifResId = R.drawable.feature_help
                    )
                    GifItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        imageLoader = imageLoader,
                        gifResId = R.drawable.tile_help
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.help_guides_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            HelpAndGuidesContent()


            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.feature_intro_footer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onBack()
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_let_me_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.rounded_mobile_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
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

@Composable
fun WhatsNewStepContent(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val updateInfo by viewModel.updateInfo
    val isCheckingUpdate by viewModel.isCheckingUpdate

    LaunchedEffect(Unit) {
        if (updateInfo == null && !isCheckingUpdate) {
            viewModel.checkForUpdates(context)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Essentials v${com.sameerasw.essentials.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Custom content slot
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                WhatsNewCustomContent()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Release Notes / Markdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                if (isCheckingUpdate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val content = updateInfo?.releaseNotes
                    if (content.isNullOrEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.msg_error_load_release_notes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val webUrl = updateInfo?.releaseUrl
                                ?: "https://github.com/sameerasw/essentials/releases"
                            TextButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val intent = Intent(Intent.ACTION_VIEW, webUrl.toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.action_view_on_web),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        SimpleMarkdown(
                            content = content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onBack()
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_let_me_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.rounded_mobile_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesStepContent(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isAppHapticsEnabled = remember { mutableStateOf(HapticUtil.loadAppHapticsEnabled(context)) }
    val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
    val isBlurSettingEnabled by viewModel.isBlurSettingEnabled
    val isRootEnabled by viewModel.isRootEnabled
    val isAutoUpdateEnabled by viewModel.isAutoUpdateEnabled
    val updateInfo by viewModel.updateInfo
    val isCheckingUpdate by viewModel.isCheckingUpdate
    var showUpdateSheet by remember { mutableStateOf(false) }

    if (showUpdateSheet) {
        UpdateBottomSheet(
            updateInfo = updateInfo,
            isChecking = isCheckingUpdate,
            onDismissRequest = { showUpdateSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.preferences_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.preferences_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Settings Section
            Text(
                text = stringResource(R.string.label_app_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )

            RoundedCardContainer {
                IconToggleItem(
                    iconRes = R.drawable.rounded_mobile_vibrate_24,
                    title = stringResource(R.string.label_haptic_feedback),
                    isChecked = isAppHapticsEnabled.value,
                    onCheckedChange = { isChecked ->
                        isAppHapticsEnabled.value = isChecked
                        HapticUtil.saveAppHapticsEnabled(context, isChecked)
                    }
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_invert_colors_24,
                    title = stringResource(R.string.setting_pitch_black_theme_title),
                    description = stringResource(R.string.setting_pitch_black_theme_desc),
                    isChecked = isPitchBlackThemeEnabled,
                    onCheckedChange = { viewModel.setPitchBlackThemeEnabled(it, context) }
                )
                val isBlurProblematic = remember { DeviceUtils.isBlurProblematicDevice() }
                IconToggleItem(
                    iconRes = R.drawable.rounded_blur_on_24,
                    title = stringResource(R.string.label_use_blur),
                    description = if (isBlurProblematic) {
                        stringResource(R.string.msg_blur_compatibility_error)
                    } else {
                        stringResource(R.string.desc_use_blur)
                    },
                    isChecked = isBlurSettingEnabled,
                    onCheckedChange = { viewModel.setBlurEnabled(it, context) },
                    enabled = !isBlurProblematic
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_numbers_24,
                    title = stringResource(R.string.setting_use_root_title),
                    description = stringResource(R.string.setting_use_root_desc),
                    isChecked = isRootEnabled,
                    onCheckedChange = { viewModel.setRootEnabled(it, context) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Updates Section
            Text(
                text = stringResource(R.string.label_updates),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start
            )

            RoundedCardContainer {
                IconToggleItem(
                    iconRes = R.drawable.rounded_mobile_check_24,
                    title = stringResource(R.string.label_auto_check_updates),
                    description = stringResource(R.string.desc_auto_check_updates),
                    isChecked = isAutoUpdateEnabled,
                    onCheckedChange = { viewModel.setAutoUpdateEnabled(it, context) }
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_check_24,
                    title = stringResource(R.string.action_check_whats_new),
                    isChecked = false,
                    showToggle = false,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.checkForUpdates(context, manual = true)
                        showUpdateSheet = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onBack()
                },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(24.dp)
                )
            }

            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onNext()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_all_set),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    painter = painterResource(id = R.drawable.rounded_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
