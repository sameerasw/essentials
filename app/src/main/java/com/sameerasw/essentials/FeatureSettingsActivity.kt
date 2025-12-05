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
import com.sameerasw.essentials.ui.composables.HapticFeedbackPicker
import com.sameerasw.essentials.ui.composables.ReusableTopAppBar
import com.sameerasw.essentials.ui.composables.SettingsCard
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.ui.composables.StatusBarIconSettingsUI
import com.sameerasw.essentials.ui.composables.CaffeinateSettingsUI

@OptIn(ExperimentalMaterial3Api::class)
class FeatureSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val feature = intent.getStringExtra("feature") ?: "Feature"
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
                            scrollBehavior = scrollBehavior
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
                                SettingsCard(title = "Haptic Feedback") {
                                    HapticFeedbackPicker(
                                        selectedFeedback = selectedHaptic,
                                        onFeedbackSelected = { type ->
                                            prefs.edit().putString("haptic_feedback_type", type.name).commit()
                                            selectedHaptic = type
                                            viewModel.setHapticFeedback(type, context)
                                            if (vibrator != null) {
                                                performHapticFeedback(vibrator, type)
                                            }
                                        }
                                    )
                                }
                            }
                            "Status Bar Icon Control" -> {
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
                                SettingsCard(title = "Haptic Feedback") {
                                    HapticFeedbackPicker(
                                        selectedFeedback = selectedHaptic,
                                        onFeedbackSelected = { type ->
                                            prefs.edit().putString("haptic_feedback_type", type.name).commit()
                                            selectedHaptic = type
                                            viewModel.setHapticFeedback(type, context)
                                            if (vibrator != null) {
                                                performHapticFeedback(vibrator, type)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
