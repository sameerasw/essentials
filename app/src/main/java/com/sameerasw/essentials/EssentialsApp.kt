package com.sameerasw.essentials

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.sameerasw.essentials.services.ScreenOffReceiver
import com.sameerasw.essentials.utils.ShizukuUtils

class EssentialsApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    private val screenOffReceiver = ScreenOffReceiver()

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        ShizukuUtils.initialize()
        com.sameerasw.essentials.utils.LogManager.init(this)
        
        // Init Automation
        com.sameerasw.essentials.domain.diy.DIYRepository.init(this)
        com.sameerasw.essentials.services.automation.AutomationManager.init(this)
        com.sameerasw.essentials.services.CalendarSyncManager.init(this)

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, intentFilter)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(screenOffReceiver)
    }
}

