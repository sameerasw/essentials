package com.sameerasw.essentials

import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.composables.configs.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.configs.CaffeinateSettingsUI
import com.sameerasw.essentials.ui.composables.configs.ScreenOffWidgetSettingsUI
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val feature = intent.getStringExtra("feature") ?: "Feature"
        val featureDescriptions = mapOf(
            "Screen off widget" to "Invisible widget to turn the screen off",
            "Statusbar icons" to "Control the visibility of statusbar icons",
            "Caffeinate" to "Keep the screen awake"
        )
        val description = featureDescriptions[feature] ?: ""
        setContent {
            EssentialsTheme {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("essentials_prefs", MODE_PRIVATE)

                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }

                val viewModel: MainViewModel = viewModel()
                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }

                var selectedHaptic by remember {
                    val name = prefs.getString("haptic_feedback_type", HapticFeedbackType.SUBTLE.name)
                    mutableStateOf(
                        try {
                            HapticFeedbackType.valueOf(name ?: HapticFeedbackType.SUBTLE.name)
                        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
                            HapticFeedbackType.SUBTLE
                        }
                    )
                }

                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = feature,
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            subtitle = description
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (feature) {
                            "Screen off widget" -> {
                                ScreenOffWidgetSettingsUI(
                                    viewModel = viewModel,
                                    selectedHaptic = selectedHaptic,
                                    onHapticSelected = { type -> selectedHaptic = type },
                                    vibrator = vibrator,
                                    prefs = prefs,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Statusbar icons" -> {
                                val statusBarViewModel: StatusBarIconViewModel = viewModel()
                                LaunchedEffect(Unit) {
                                    statusBarViewModel.check(context)
                                }
                                StatusBarIconSettingsUI(
                                    viewModel = statusBarViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            "Caffeinate" -> {
                                val caffeinateViewModel: CaffeinateViewModel = viewModel()
                                LaunchedEffect(Unit) {
                                    caffeinateViewModel.check(context)
                                }
                                CaffeinateSettingsUI(
                                    viewModel = caffeinateViewModel,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            else -> {
                                ScreenOffWidgetSettingsUI(
                                    viewModel = viewModel,
                                    selectedHaptic = selectedHaptic,
                                    onHapticSelected = { type -> selectedHaptic = type },
                                    vibrator = vibrator,
                                    prefs = prefs,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
