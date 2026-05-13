package com.sameerasw.essentials

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.lifecycle.lifecycleScope
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShutUpShortcutActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra("package_name")
        if (packageName == null) {
            finish()
            return
        }

        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.scale(5f))
                }
            }
        }

        val settingsRepository = SettingsRepository(this)
        val config = settingsRepository.loadShutUpConfigs().find { it.packageName == packageName }

        lifecycleScope.launch {
            // Unfreeze first while Shizuku/Root is still  functional
            if (com.sameerasw.essentials.utils.FreezeManager.isAppFrozen(
                    this@ShutUpShortcutActivity,
                    packageName
                )
            ) {
                com.sameerasw.essentials.utils.FreezeManager.unfreezeApp(
                    this@ShutUpShortcutActivity,
                    packageName
                )
                delay(200) // Small extra delay for system to register unfreeze
            }

            if (config != null && config.isEnabled) {
                if (PermissionUtils.canWriteSecureSettings(this@ShutUpShortcutActivity)) {
                    applyShutUpSettings(config, settingsRepository)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ShutUpShortcutActivity,
                            getString(R.string.shut_up_toast_active),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Delay to ensure system registers the settings changes
            delay(800)

            launchApp(packageName)
            finish()
        }
    }

    private suspend fun applyShutUpSettings(
        config: ShutUpAppConfig,
        repository: SettingsRepository
    ) {
        withContext(Dispatchers.IO) {
            val originalSettings = mutableMapOf<String, String>()

            if (config.disableDevOptions) {
                // Backup all relevant dev settings because disabling the main toggle might reset them
                val secureSettings = listOf(
                    "anr_show_background",
                    "bugreport_in_power_menu",
                    "display_density_forced",
                    "mock_location",
                    "secure_overlay_settings",
                    "usb_audio_automatic_routing_disabled"
                )
                val systemSettings = listOf("show_touches", "show_key_presses")
                val globalSettings = listOf(
                    "adb_allowed_connection_time",
                    "adb_enabled",
                    "adb_wifi_enabled",
                    "always_finish_activities",
                    "animator_duration_scale",
                    "app_standby_enabled",
                    "cached_apps_freezer",
                    "default_install_location",
                    "development_settings_enabled",
                    "disable_window_blurs",
                    "enable_freeform_support",
                    "enable_non_resizable_multi_window",
                    "force_allow_on_external",
                    "force_desktop_mode_on_external_displays",
                    "force_resizable_activities",
                    "mobile_data_always_on",
                    "stay_on_while_plugged_in",
                    "usb_mass_storage_enabled",
                    "wait_for_debugger",
                    "wifi_display_certification_on",
                    "wifi_display_on",
                    "wifi_scan_always_enabled",
                    "window_animation_scale"
                )

                secureSettings.forEach { key ->
                    Settings.Secure.getString(contentResolver, key)
                        ?.let { originalSettings["secure:$key"] = it }
                }
                systemSettings.forEach { key ->
                    Settings.System.getString(contentResolver, key)
                        ?.let { originalSettings["system:$key"] = it }
                }
                globalSettings.forEach { key ->
                    Settings.Global.getString(contentResolver, key)
                        ?.let { originalSettings["global:$key"] = it }
                }

                // Disable dev options
                Settings.Global.putString(
                    contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    "0"
                )
            }

            // Always explicitly disable USB debugging if requested, even if dev options were already disabled
            // as some apps check this specific setting directly.
            if (config.disableUsbDebugging) {
                val current =
                    Settings.Global.getString(contentResolver, Settings.Global.ADB_ENABLED) ?: "0"
                if (current == "1") {
                    if (!originalSettings.containsKey("global:${Settings.Global.ADB_ENABLED}")) {
                        originalSettings["global:${Settings.Global.ADB_ENABLED}"] = "1"
                    }
                    Settings.Global.putString(contentResolver, Settings.Global.ADB_ENABLED, "0")
                }
            }

            if (config.disableWirelessDebugging) {
                val current = Settings.Global.getString(contentResolver, "adb_wifi_enabled") ?: "0"
                if (current == "1") {
                    if (!originalSettings.containsKey("global:adb_wifi_enabled")) {
                        originalSettings["global:adb_wifi_enabled"] = "1"
                    }
                    Settings.Global.putString(contentResolver, "adb_wifi_enabled", "0")
                }
            }

            if (config.disableAccessibility) {
                val current = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (!current.isNullOrEmpty()) {
                    originalSettings["secure:${Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES}"] =
                        current
                    Settings.Secure.putString(
                        contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        ""
                    )
                }
            }

            if (originalSettings.isNotEmpty()) {
                repository.saveShutUpOriginalSettings(originalSettings)
            }
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Could not launch $packageName", Toast.LENGTH_SHORT).show()
        }
    }
}
