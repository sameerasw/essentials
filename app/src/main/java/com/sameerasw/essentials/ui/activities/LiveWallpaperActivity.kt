package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class LiveWallpaperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, WallpaperActivity::class.java).apply {
            putExtra("tab", "live")
        }
        startActivity(intent)
        finish()
    }
}
