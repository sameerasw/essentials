package com.sameerasw.essentials

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.ui.composables.ReusableTopAppBar
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.composables.PermissionCard
import com.sameerasw.essentials.ui.components.dialogs.AboutSection

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EssentialsTheme {
                val context = LocalContext.current
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

                LaunchedEffect(Unit) {
                    viewModel.check(context)
                }

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = "Settings",
                            hasBack = true,
                            hasSearch = false,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    SettingsContent(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.check(this)
    }
}

@Composable
fun SettingsContent(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        PermissionCard(
            iconRes = R.drawable.rounded_settings_accessibility_24,
            title = "Accessibility",
            dependentFeatures = PermissionRegistry.getFeatures("ACCESSIBILITY"),
            actionLabel = if (isAccessibilityEnabled) "Open Accessibility Settings" else "Enable Accessibility",
            isGranted = isAccessibilityEnabled,
            onActionClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(16.dp)
        )

        PermissionCard(
            iconRes = R.drawable.rounded_chevron_right_24,
            title = "Write Secure Settings",
            dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
            actionLabel = "Copy ADB",
            isGranted = isWriteSecureSettingsEnabled,
            onActionClick = {
                val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("adb_command", adbCommand)
                clipboard.setPrimaryClip(clip)
            },
            secondaryActionLabel = "Check",
            onSecondaryActionClick = {
                viewModel.check(context)
            },
            modifier = Modifier.padding(16.dp)
        )

        AboutSection()

    }
}
