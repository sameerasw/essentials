package com.sameerasw.essentials.ui.composables.configs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.sameerasw.essentials.services.ScreenOffAccessibilityService
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.viewmodels.MainViewModel
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun EdgeLightingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnabled by viewModel.isEdgeLightingEnabled

    // Track overlay permission and refresh on lifecycle resume so UI updates when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember { mutableStateOf(try { Settings.canDrawOverlays(context) } catch (_: Exception) { false }) }

    // Check accessibility service enabled state
    var hasAccessibilityEnabled by remember { mutableStateOf(try { 
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
        enabled?.contains(serviceName) == true
    } catch (_: Exception) { false }) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = try { Settings.canDrawOverlays(context) } catch (_: Exception) { false }
                // re-evaluate accessibility
                hasAccessibilityEnabled = try {
                    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    val serviceName = "${context.packageName}/${ScreenOffAccessibilityService::class.java.name}"
                    enabled?.contains(serviceName) == true
                } catch (_: Exception) { false }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Edge lighting")
            Switch(checked = isEnabled, onCheckedChange = { checked ->
                if (checked && !hasOverlayPermission) {
                    // If enabling but permission not granted, open the app overlay settings so user can grant it first
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } else {
                    viewModel.setEdgeLightingEnabled(checked, context)
                }
            })
        }

        Text(text = if (hasOverlayPermission) "Overlay permission: Granted" else "Overlay permission: Not granted")

        // Accessibility elevation guidance
        Text(text = if (hasAccessibilityEnabled) "Accessibility: Enabled (allows elevated overlays)" else "Accessibility: Disabled — enable for higher overlay privileges")
        if (!hasAccessibilityEnabled) {
            Button(onClick = {
                val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)
            }) {
                Text(text = "Open accessibility settings")
            }
        }

        Button(onClick = {
            // Open app-specific overlay permission screen
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }) {
            Text(text = "Open overlay permission for this app")
        }

        Button(onClick = {
            // Open global special app access list (where draw over apps are listed)
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }) {
            Text(text = "Open overlay apps list")
        }

        Button(onClick = { viewModel.triggerEdgeLighting(context) }) {
            Text(text = "Show test overlay")
        }

        if (!hasOverlayPermission) {
            TextButton(onClick = {
                // also open app details as a fallback
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i)
            }) {
                Text(text = "Open app details (fallback)")
            }
        }
    }
}