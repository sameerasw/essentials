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
import androidx.activity.result.contract.ActivityResultContracts
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

class ColorPickerActivity : ComponentActivity() {

    private val eyeDropperLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Intent.EXTRA_COLOR is usually for Android 15/16+ (API 35/36+)
                // Using a fallback for the constant name if not yet in SDK
                result.data?.getIntExtra("android.intent.extra.COLOR", Color.BLACK) ?: Color.BLACK
            } else {
                Color.BLACK
            }

            copyToClipboard(color)
            HapticUtil.performHapticForService(this)
            
            val hexColor = String.format("#%06X", 0xFFFFFF and color)
            Toast.makeText(this, "Color $hexColor copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchColorPicker()
    }

    private fun launchColorPicker() {
        try {
            // Intent.ACTION_OPEN_EYE_DROPPER is usually for Android 16/17+ (API 36/37+)
            // Using literal string for potential future compatibility if constant is not in this SDK version
            val intent = Intent("android.intent.action.OPEN_EYE_DROPPER")
            eyeDropperLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_eyedropper_failed), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun copyToClipboard(color: Int) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val hexColor = String.format("#%06X", 0xFFFFFF and color)
        val clip = ClipData.newPlainText("Picked Color", hexColor)
        clipboard.setPrimaryClip(clip)
    }
}
