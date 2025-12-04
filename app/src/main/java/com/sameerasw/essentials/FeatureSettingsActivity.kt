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
import com.sameerasw.essentials.ui.composables.HapticFeedbackPicker
import com.sameerasw.essentials.ui.composables.ReusableTopAppBar
import com.sameerasw.essentials.ui.composables.SettingsCard
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel

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

                // Get ViewModel and load persisted preferences into it
                val viewModel: MainViewModel = viewModel()
                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }

                // Local UI state backed by SharedPreferences so the picker reflects persisted value immediately
                var selectedHaptic by remember {
                    val name = prefs.getString("haptic_feedback_type", HapticFeedbackType.SUBTLE.name)
                    mutableStateOf(
                        try {
                            HapticFeedbackType.valueOf(name ?: HapticFeedbackType.SUBTLE.name)
                        } catch (e: Exception) {
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

                        SettingsCard(title = "Haptic Feedback") {
                            HapticFeedbackPicker(
                                selectedFeedback = selectedHaptic,
                                onFeedbackSelected = { type ->
                                    // persist selection to SharedPreferences synchronously to avoid races
                                    prefs.edit().putString("haptic_feedback_type", type.name).commit()
                                    // update local UI state and ViewModel
                                    selectedHaptic = type
                                    viewModel.setHapticFeedback(type, context)
                                    // preview haptic
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
