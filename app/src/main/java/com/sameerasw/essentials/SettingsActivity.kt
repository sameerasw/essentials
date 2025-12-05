package com.sameerasw.essentials

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.dialogs.AboutSection
import com.sameerasw.essentials.viewmodels.MainViewModel

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

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 || requestCode == 1002) {
            viewModel.check(this)
        }
    }
}

@Composable
fun SettingsContent(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        RoundedCardContainer {
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
            )

            PermissionCard(
                iconRes = R.drawable.rounded_chevron_right_24,
                title = "Write Secure Settings",
                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                actionLabel = "Copy ADB",
                isGranted = isWriteSecureSettingsEnabled,
                onActionClick = {
                    val adbCommand = "adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("adb_command", adbCommand)
                    clipboard.setPrimaryClip(clip)
                },
                secondaryActionLabel = "Check",
                onSecondaryActionClick = {
                    viewModel.check(context)
                },
            )

            PermissionCard(
                iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                title = "Read Phone State",
                dependentFeatures = listOf("Smart Data"),
                actionLabel = "Grant Permission",
                isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED,
                onActionClick = {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        context as ComponentActivity,
                        arrayOf(Manifest.permission.READ_PHONE_STATE),
                        1001
                    )
                },
            )

            PermissionCard(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = "Post Notifications",
                dependentFeatures = listOf("Caffeinate Show Notification"),
                actionLabel = "Grant Permission",
                isGranted = isPostNotificationsEnabled,
                onActionClick = {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        context as ComponentActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1002
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        RoundedCardContainer {
            AboutSection()
        }

    }
}
