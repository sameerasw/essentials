package com.sameerasw.essentials

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.sameerasw.essentials.ui.components.cards.PermissionCard
import com.sameerasw.essentials.ui.components.dialogs.AboutSection
import com.sameerasw.essentials.viewmodels.MainViewModel
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val shizukuPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.check(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Register Shizuku permission listener
        Shizuku.addRequestPermissionResultListener(shizukuPermissionResultListener)
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

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionResultListener)
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 || requestCode == 1002 || requestCode == 1003) {
            viewModel.check(this)
        }
    }
}

@Composable
fun SettingsContent(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isWriteSecureSettingsEnabled by viewModel.isWriteSecureSettingsEnabled
    val isPostNotificationsEnabled by viewModel.isPostNotificationsEnabled
    val isReadPhoneStateEnabled by viewModel.isReadPhoneStateEnabled
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled
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
                actionLabel = if (isAccessibilityEnabled) "Granted" else "Grant Permission",
                isGranted = isAccessibilityEnabled,
                onActionClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
            )

            PermissionCard(
                iconRes = R.drawable.rounded_security_24,
                title = "Write Secure Settings",
                dependentFeatures = PermissionRegistry.getFeatures("WRITE_SECURE_SETTINGS"),
                actionLabel = if (isWriteSecureSettingsEnabled) "Granted" else "Copy ADB Command",
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

            if (isShizukuAvailable) {
                PermissionCard(
                    iconRes = R.drawable.rounded_adb_24,
                    title = "Shizuku",
                    dependentFeatures = listOf("Automatic Write Secure Settings Permission"),
                    actionLabel = if (isShizukuPermissionGranted) "Granted" else "Request Permission",
                    isGranted = isShizukuPermissionGranted,
                    onActionClick = {
                        viewModel.requestShizukuPermission()
                    },
                    secondaryActionLabel = if (isShizukuPermissionGranted && !isWriteSecureSettingsEnabled) "Auto-Grant" else null,
                    onSecondaryActionClick = if (isShizukuPermissionGranted && !isWriteSecureSettingsEnabled) {
                        {
                            viewModel.grantWriteSecureSettingsWithShizuku(context)
                        }
                    } else null,
                )
            }

            PermissionCard(
                iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                title = "Read Phone State",
                dependentFeatures = listOf("Smart Data"),
                actionLabel = if (isReadPhoneStateEnabled) "Granted" else "Grant Permission",
                isGranted = isReadPhoneStateEnabled,
                onActionClick = {
                    viewModel.requestReadPhoneStatePermission(context as ComponentActivity)
                },
            )

            PermissionCard(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = "Post Notifications",
                dependentFeatures = listOf("Caffeinate Show Notification"),
                actionLabel = if (isPostNotificationsEnabled) "Granted" else "Grant Permission",
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

            PermissionCard(
                iconRes = R.drawable.rounded_magnify_fullscreen_24,
                title = "Draw Overlays",
                dependentFeatures = listOf("Edge Lighting"),
                actionLabel = if (isOverlayPermissionGranted) "Granted" else "Grant Permission",
                isGranted = isOverlayPermissionGranted,
                onActionClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
            )

            PermissionCard(
                iconRes = R.drawable.rounded_notification_settings_24,
                title = "Notification Listener",
                dependentFeatures = listOf("Edge Lighting"),
                actionLabel = if (isNotificationListenerEnabled) "Granted" else "Enable listener",
                isGranted = isNotificationListenerEnabled,
                onActionClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))


        RoundedCardContainer {
            AboutSection()
        }

    }
}
