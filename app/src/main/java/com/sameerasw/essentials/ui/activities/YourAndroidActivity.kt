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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.model.DeviceSpecs
import com.sameerasw.essentials.ui.components.DeviceHeroCard
import com.sameerasw.essentials.ui.components.DeviceSpecsCard
import com.sameerasw.essentials.ui.components.EssentialsFloatingToolbar
import com.sameerasw.essentials.ui.modifiers.BlurDirection
import com.sameerasw.essentials.ui.modifiers.progressiveBlur
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.DeviceInfo
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.GSMArenaService
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YourAndroidViewModel : ViewModel() {
    private val _deviceSpecs = MutableStateFlow<DeviceSpecs?>(null)
    val deviceSpecs = _deviceSpecs.asStateFlow()

    private val _isSpecsLoading = MutableStateFlow(true)
    val isSpecsLoading = _isSpecsLoading.asStateFlow()

    var hasRunStartupAnimation = false

    fun loadDeviceSpecs(deviceInfo: DeviceInfo) {
        if (_deviceSpecs.value != null) {
            _isSpecsLoading.value = false
            return
        }

        viewModelScope.launch {
            _isSpecsLoading.value = true
            val specs = withContext(Dispatchers.IO) {
                val manufacturer = deviceInfo.manufacturer
                val model = deviceInfo.model
                val deviceName = deviceInfo.deviceName
                val deviceCodename = deviceInfo.device

                // Generate a prioritized list of search queries
                val queries = mutableListOf<String>()
                
                // 1. Marketing name (Manufacturer + Model)
                if (model.contains(manufacturer, ignoreCase = true)) {
                    queries.add(model)
                } else {
                    queries.add("$manufacturer $model")
                }
                
                // 2. Model number directly if it's different from marketing name
                if (!queries.contains(model)) {
                    queries.add(model)
                }
                
                // 3. User-defined device name (sometimes it's the marketing name)
                if (deviceName.isNotBlank() && !queries.contains(deviceName)) {
                    queries.add(deviceName)
                }
                
                // 4. Device codename (e.g., "shiba", "a51")
                if (deviceCodename.isNotBlank() && !queries.contains(deviceCodename)) {
                    queries.add(deviceCodename)
                }

                GSMArenaService.fetchSpecs(
                    preferredName = manufacturer,
                    preferredModel = model,
                    queries = queries.toTypedArray()
                )
            }
            _deviceSpecs.value = specs
            _isSpecsLoading.value = false
        }
    }
}

class YourAndroidActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawableResource(if (isDarkMode) android.R.color.black else R.color.app_window_background)

        setContent {
            val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val isBlurEnabled by mainViewModel.isBlurEnabled
            
            val viewModel: YourAndroidViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val deviceSpecs by viewModel.deviceSpecs.collectAsState()
            val isSpecsLoading by viewModel.isSpecsLoading.collectAsState()
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
                    override fun isEnabled(viewModel: com.sameerasw.essentials.viewmodels.MainViewModel) = true
                    override fun onToggle(viewModel: com.sameerasw.essentials.viewmodels.MainViewModel, context: android.content.Context, enabled: Boolean) {}
                }
            }

            LaunchedEffect(Unit) {
                mainViewModel.check(context)
                viewModel.loadDeviceSpecs(deviceInfo)
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                val statusBarHeightPx = with(LocalDensity.current) {
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .then(
                            if (isBlurEnabled) {
                                Modifier.progressiveBlur(
                                    blurRadius = 40f,
                                    height = statusBarHeightPx * 1.15f,
                                    direction = BlurDirection.TOP
                                )
                            } else Modifier
                        )
                ) {
                    YourAndroidContent(
                        deviceInfo = deviceInfo,
                        deviceSpecs = deviceSpecs,
                        isSpecsLoading = isSpecsLoading,
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
    deviceSpecs: DeviceSpecs?,
    isSpecsLoading: Boolean,
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

    val mainViewModel: com.sameerasw.essentials.viewmodels.MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val initialImageOffset = (screenHeight / 2) - 240.dp - 64.dp
    val isBlurEnabled by mainViewModel.isBlurEnabled

    val imageOffsetState = animateDpAsState(
        targetValue = if (isStartupAnimationRunning) 0.dp else initialImageOffset,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "imageOffset"
    )

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
            .then(
                if (isBlurEnabled) {
                    Modifier.progressiveBlur(
                        blurRadius = 40f,
                        height = with(LocalDensity.current) { 150.dp.toPx() },
                        direction = BlurDirection.BOTTOM
                    )
                } else Modifier
            )
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeviceHeroCard(
            deviceInfo = deviceInfo,
            deviceSpecs = deviceSpecs,
            imageOffset = { imageOffsetState.value },
            contentAlpha = { contentAlphaState.value },
            contentOffset = { contentOffsetState.value }
        )

        DeviceSpecsCard(
            deviceSpecs = deviceSpecs,
            isLoading = isSpecsLoading,
            modifier = Modifier.graphicsLayer {
                alpha = contentAlphaState.value
                translationY = contentOffsetState.value.toPx()
            }
        )
    }
}
