/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - QR Code
 * File: QrCodeGenerator.kt
 * Description: Unique Material 3 styled QR Code generator with rounded data modules, precision finder patterns, centered app logo badge, and high scan reliability.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sameerasw.essentials.R
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap
import kotlin.math.max

object QrCodeGenerator {

    /**
     * Generates a unique Material 3 rounded QR code bitmap with high error correction and centered app logo badge.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 600,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        logo: Bitmap? = null,
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            put(EncodeHintType.MARGIN, 2)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val moduleCount = bitMatrix.width
        val moduleSize = size.toFloat() / moduleCount

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foregroundColor
            style = Paint.Style.FILL
        }

        val cornerRadius = moduleSize * 0.35f
        val rect = RectF()

        // Helper to identify finder pattern regions (top-left, top-right, bottom-left 7x7 eyes)
        fun isFinderPattern(x: Int, y: Int): Boolean {
            if (x in 0..6 && y in 0..6) return true
            if (x in (moduleCount - 7) until moduleCount && y in 0..6) return true
            if (x in 0..6 && y in (moduleCount - 7) until moduleCount) return true
            return false
        }

        for (y in 0 until moduleCount) {
            for (x in 0 until moduleCount) {
                if (bitMatrix.get(x, y)) {
                    val left = x * moduleSize
                    val top = y * moduleSize
                    val right = left + moduleSize
                    val bottom = top + moduleSize
                    rect.set(left, top, right, bottom)

                    if (isFinderPattern(x, y)) {
                        // Sharp precision finder modules for instant scanner edge detection
                        canvas.drawRoundRect(rect, moduleSize * 0.15f, moduleSize * 0.15f, paint)
                    } else {
                        // Unique Material 3 rounded modules
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                    }
                }
            }
        }

        // Draw Center App Logo Badge
        if (logo != null) {
            val logoSize = size * 0.20f
            val logoMargin = (size - logoSize) / 2f
            val badgeRect = RectF(logoMargin, logoMargin, logoMargin + logoSize, logoMargin + logoSize)

            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = backgroundColor
                style = Paint.Style.FILL
            }
            val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#26000000")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            val badgeRadius = logoSize * 0.26f
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgeStrokePaint)

            val innerPadding = logoSize * 0.12f
            val innerRect = RectF(
                badgeRect.left + innerPadding,
                badgeRect.top + innerPadding,
                badgeRect.right - innerPadding,
                badgeRect.bottom - innerPadding,
            )
            canvas.drawBitmap(logo, null, innerRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }

        return bitmap
    }

    /**
     * Extracts the app logo as a Bitmap.
     */
    fun getAppLogoBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
            val bitmap = Bitmap.createBitmap(
                max(1, drawable.intrinsicWidth),
                max(1, drawable.intrinsicHeight),
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves QR Bitmap to cache and returns FileProvider content URI for sharing with image attachment.
     */
    fun getShareableQrUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_qr")
            cachePath.mkdirs()
            val file = File(cachePath, "link_qr_code.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (_: Exception) {
            null
        }
    }
}
