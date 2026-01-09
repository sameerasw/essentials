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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.FreezeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutHandlerActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle shortcut creation from the picker (fallback)
        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            Toast.makeText(this, "Long press an app in the grid to add a shortcut", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AppFreezingActivity::class.java))
            finish()
            return
        }

        val packageName = intent.getStringExtra("package_name")

        if (packageName != null) {
            setContent {
                EssentialsTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(modifier = Modifier.scale(5f))
                    }
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                val isFrozen = FreezeManager.isAppFrozen(this@ShortcutHandlerActivity, packageName)
                if (isFrozen) {
                    FreezeManager.unfreezeApp(packageName)
                    // Small delay to ensure system registers the change
                    delay(500) // Slightly longer delay to show the nice loader if frozen
                }

                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ShortcutHandlerActivity, "App not found", Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }
        } else {
            finish()
        }
    }
}
