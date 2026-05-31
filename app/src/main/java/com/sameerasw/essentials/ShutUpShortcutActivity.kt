package com.sameerasw.essentials


import android.content.Intent
import android.os.Bundle

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

import com.sameerasw.essentials.ui.theme.EssentialsTheme

import com.sameerasw.essentials.utils.ShutUpManager
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
                    ShutUpManager.applyShutUpSettings(this@ShutUpShortcutActivity, config)
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
