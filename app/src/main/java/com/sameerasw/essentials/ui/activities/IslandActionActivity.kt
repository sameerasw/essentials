package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService

class IslandActionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val brief = intent?.action == ACTION_OPEN_BRIEF
        runCatching {
            val island = ScreenOffAccessibilityService.instance?.islandOverlayHandler
            if (brief) island?.openBriefIfEnabled() else island?.openHub()
        }
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ACTION_OPEN_BRIEF = "com.sameerasw.essentials.action.OPEN_BRIEF"
    }
}
