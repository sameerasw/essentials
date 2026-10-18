package com.sameerasw.essentials.island.plugins.notifications

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.sameerasw.essentials.R

object OtpClipboard {
    fun copy(context: Context, code: String): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        val clip = ClipData.newPlainText(context.getString(R.string.island_otp_clip_label), code)
        clip.description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        return try {
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }
}
