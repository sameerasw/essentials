/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers - URL Shortener Tile
 * File: UrlShortenerTileService.kt
 * Description: Quick Settings pull-down tile to instantly shorten web URLs from the clipboard with haptic feedback, auto-copy, and action notifications.
 */

package com.sameerasw.essentials.services.tiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.UrlShortener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class UrlShortenerTileService : BaseTileService() {
    override val isSensitiveTile: Boolean = true

    override fun getTileLabel(): String = getString(R.string.tile_url_shortener)

    override fun getTileSubtitle(): String = getString(R.string.tile_url_shortener_subtitle)

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_link_24)

    override fun getTileState(): Int = Tile.STATE_INACTIVE

    override fun onTileClick() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        val text = clipData?.getItemAt(0)?.text?.toString()?.trim()

        val isUrlLike = !text.isNullOrBlank() && (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true) || (text.contains(".") && !text.contains(" ")))
        if (!isUrlLike) {
            Toast.makeText(this, getString(R.string.shorten_qs_no_url_clipboard), Toast.LENGTH_SHORT).show()
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val shortUrl = UrlShortener.shortenUrl(
                    url = text,
                    expiration = UrlShortener.getDefaultExpiration(this@UrlShortenerTileService),
                    context = this@UrlShortenerTileService,
                )

                withContext(Dispatchers.Main) {
                    try {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Shortened Link", shortUrl))
                    } catch (_: Exception) {}

                    HapticUtil.performHapticForService(this@UrlShortenerTileService, HapticFeedbackType.DOUBLE)
                    Toast.makeText(this@UrlShortenerTileService, getString(R.string.shorten_qs_success, shortUrl), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    HapticUtil.performHapticForService(this@UrlShortenerTileService, HapticFeedbackType.TICK)
                    Toast.makeText(this@UrlShortenerTileService, e.message ?: getString(R.string.shorten_error_generic), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
