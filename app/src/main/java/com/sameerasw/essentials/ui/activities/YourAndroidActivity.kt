package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.DeviceHeroCard
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.DeviceInfo
import com.sameerasw.essentials.utils.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YourAndroidViewModel : ViewModel() {
    var hasRunStartupAnimation = false
}

class YourAndroidActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDarkMode =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)

        setContent {
            val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by mainViewModel.isBlurEnabled

            val viewModel: YourAndroidViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

            val context = androidx.compose.ui.platform.LocalContext.current
            val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }
            var showHelpSheet by remember { mutableStateOf(false) }

            val yourAndroidFeature = remember {
                object : com.sameerasw.essentials.domain.model.Feature(
                    id = "Your Android",
                    title = R.string.tab_your_android,
                    iconRes = R.drawable.rounded_android_24,
                    category = R.string.cat_system,
                    description = R.string.about_desc_your_android,
                    aboutDescription = R.string.about_desc_your_android,
                    showToggle = false,
                    hasMoreSettings = false
                ) {
                    override fun isEnabled(viewModel: com.sameerasw.essentials.viewmodels.MainViewModel) =
                        true

                    override fun onToggle(
                        viewModel: com.sameerasw.essentials.viewmodels.MainViewModel,
                        context: android.content.Context,
                        enabled: Boolean
                    ) {
                    }
                }
            }

            LaunchedEffect(Unit) {
                mainViewModel.check(context)
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val statusBarHeightPx = with(LocalDensity.current) {
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .progressiveBlur(
                            blurRadius = if (isBlurEnabled) 40f else 0f,
                            height = statusBarHeightPx * 1.15f,
                            direction = BlurDirection.TOP
                        )
                ) {
                    YourAndroidContent(
                        deviceInfo = deviceInfo,
                        hasRunStartupAnimation = viewModel.hasRunStartupAnimation,
                        onAnimationRun = { viewModel.hasRunStartupAnimation = true },
                        modifier = Modifier.fillMaxSize()
                    )

                    EssentialsFloatingToolbar(
                        title = stringResource(R.string.tab_your_android),
                        onBackClick = { finish() },
                        onHelpClick = { showHelpSheet = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                    )

                    if (showHelpSheet) {
                        com.sameerasw.essentials.ui.components.sheets.FeatureHelpBottomSheet(
                            onDismissRequest = { showHelpSheet = false },
                            feature = yourAndroidFeature
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YourAndroidContent(
    deviceInfo: DeviceInfo,
    hasRunStartupAnimation: Boolean,
    onAnimationRun: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var isStartupAnimationRunning by remember { mutableStateOf(hasRunStartupAnimation) }

    LaunchedEffect(hasRunStartupAnimation) {
        if (!hasRunStartupAnimation) {
            delay(100)
            isStartupAnimationRunning = true
            onAnimationRun()
        }
    }

    val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isBlurEnabled by mainViewModel.isBlurEnabled


    val contentAlphaState = animateFloatAsState(
        targetValue = if (isStartupAnimationRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 750, delayMillis = 350, easing = LinearEasing),
        label = "contentAlpha"
    )

    val contentOffsetState = animateDpAsState(
        targetValue = if (isStartupAnimationRunning) 0.dp else 40.dp,
        animationSpec = tween(
            durationMillis = 750,
            delayMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "contentOffset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .progressiveBlur(
                blurRadius = if (isBlurEnabled) 40f else 0f,
                height = with(LocalDensity.current) { 150.dp.toPx() },
                direction = BlurDirection.BOTTOM
            )
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding(),
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeviceHeroCard(
            deviceInfo = deviceInfo,
            contentAlpha = { contentAlphaState.value },
            contentOffset = { contentOffsetState.value }
        )
    }
}
