/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Application Activities
 * File: MeDropActivity.kt
 */

package com.sameerasw.essentials.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.ui.features.system.sheets.MeDropBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.MeDropContactPickerHelper
import com.sameerasw.essentials.utils.MeDropNfcManager
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MeDropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val isPitchBlackThemeEnabled by mainViewModel.isPitchBlackThemeEnabled
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                mainViewModel.check(context)
            }

            val contactPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        scope.launch {
                            val contact = MeDropContactPickerHelper.processResult(uri, context)
                            mainViewModel.setMeDropContact(context, contact)
                        }
                    }
                }
            }

            val currentContact by mainViewModel.meDropContact
            LaunchedEffect(currentContact) {
                val activity = context as? android.app.Activity
                if (currentContact != null && activity != null) {
                    MeDropNfcManager.startBroadcast(activity, currentContact!!)
                }
            }

            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                MeDropBottomSheet(
                    viewModel = mainViewModel,
                    showSettings = false,
                    onPickContact = {
                        contactPickerLauncher.launch(MeDropContactPickerHelper.buildPickIntent())
                    },
                    onDismissRequest = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle NFC intents if needed, though HCE is primary here
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            MeDropNfcManager.stopBroadcast(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MeDropNfcManager.stopBroadcast(this)
    }
}
