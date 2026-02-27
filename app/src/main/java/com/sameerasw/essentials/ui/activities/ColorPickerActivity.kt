package com.sameerasw.essentials.ui.activities

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sheets.ColorPickerBottomSheet
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil

class ColorPickerActivity : ComponentActivity() {

    private var pickedColor by mutableStateOf<Int?>(null)
    private var showBottomSheet by mutableStateOf(false)

    private val eyeDropperLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                result.data?.getIntExtra("android.intent.extra.COLOR", Color.BLACK) ?: Color.BLACK
            } else {
                Color.BLACK
            }

            pickedColor = color
            showBottomSheet = true
            HapticUtil.performHapticForService(this)
        } else {
            // If they cancelled the eye dropper and no bottom sheet is showing, finish
            if (!showBottomSheet) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EssentialsTheme {
                if (showBottomSheet && pickedColor != null) {
                    ColorPickerBottomSheet(
                        colorInt = pickedColor!!,
                        onRetake = {
                            showBottomSheet = false
                            launchColorPicker()
                        },
                        onDismissRequest = {
                            finish()
                        }
                    )
                }
            }
        }
        
        if (savedInstanceState == null) {
            launchColorPicker()
        }
    }

    private fun launchColorPicker() {
        try {
            val intent = Intent("android.intent.action.OPEN_EYE_DROPPER")
            eyeDropperLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_eyedropper_failed), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
