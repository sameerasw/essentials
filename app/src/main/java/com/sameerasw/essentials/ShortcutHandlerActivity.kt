package com.sameerasw.essentials

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.sameerasw.essentials.utils.FreezeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShortcutHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle shortcut creation from the picker (fallback)
        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            Toast.makeText(this, "Long press an app in the grid to add a shortcut", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AppFreezingActivity::class.java))
            finish()
            return
        }

        val packageName = intent.getStringExtra("package_name")
        
        if (packageName != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val isFrozen = FreezeManager.isAppFrozen(this@ShortcutHandlerActivity, packageName)
                if (isFrozen) {
                    FreezeManager.unfreezeApp(packageName)
                    // Small delay to ensure system registers the change
                    delay(150)
                }
                
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                } else {
                    launch(Dispatchers.Main) {
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
