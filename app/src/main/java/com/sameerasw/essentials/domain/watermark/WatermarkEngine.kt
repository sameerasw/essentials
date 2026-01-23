package com.sameerasw.essentials.domain.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import com.sameerasw.essentials.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

enum class WatermarkStyle {
    OVERLAY,
    FRAME
}

data class WatermarkOptions(
    val style: WatermarkStyle = WatermarkStyle.FRAME,
    val showDeviceBrand: Boolean = true,
    val showExif: Boolean = true,
    val customText: String = "",
    val outputQuality: Int = 100,
    val useDarkTheme: Boolean = false
)

class WatermarkEngine(
    private val context: Context,
    private val metadataProvider: MetadataProvider
) {
    suspend fun processImage(uri: Uri, options: WatermarkOptions): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw IllegalStateException("Cannot open input stream")
        
        // Decode bitmap - mutable to allow drawing if Overlay
        val originalBitmap = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }) ?: throw IllegalStateException("Failed to decode bitmap")
        
        inputStream.close() // Close stream after decoding

        val resultBitmap = processBitmap(originalBitmap, uri, options)

        // Save to cache dir
        val file = File(context.cacheDir, "watermarked_${System.currentTimeMillis()}.jpg")
        val outStream = FileOutputStream(file)
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, options.outputQuality, outStream)
        outStream.flush()
        outStream.close()
        
        // Copy EXIF data
        try {
            val inputStreamExif = context.contentResolver.openInputStream(uri)
            if (inputStreamExif != null) {
                val oldExif = androidx.exifinterface.media.ExifInterface(inputStreamExif)
                val newExif = androidx.exifinterface.media.ExifInterface(file)
                
                // Copy all tags
                val attributes = arrayOf(
                    androidx.exifinterface.media.ExifInterface.TAG_DATETIME,
                    androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED,
                    androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
                    androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME,
                    androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER,
                    androidx.exifinterface.media.ExifInterface.TAG_FLASH,
                    androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD,
                    androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP,
                    androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                    androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                    androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS,
                    androidx.exifinterface.media.ExifInterface.TAG_SUBSEC_TIME,
                    androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE
                )

                for (attr in attributes) {
                    val value = oldExif.getAttribute(attr)
                    if (value != null) {
                        newExif.setAttribute(attr, value)
                    }
                }
                
                // Add essentials tag
                newExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, "Watermark by Essentials")
                newExif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT, "Watermark by Essentials")
                
                newExif.saveAttributes()
                inputStreamExif.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Recycle bitmaps
        if (resultBitmap != originalBitmap) originalBitmap.recycle()
        file
    }

    suspend fun processBitmap(bitmap: Bitmap, uri: Uri, options: WatermarkOptions): Bitmap = withContext(Dispatchers.Default) {
        val exifData = metadataProvider.extractExif(uri)
        when (options.style) {
            WatermarkStyle.OVERLAY -> drawOverlay(bitmap, exifData, options)
            WatermarkStyle.FRAME -> drawFrame(bitmap, exifData, options)
        }
    }

    private fun drawOverlay(bitmap: Bitmap, exifData: ExifData, options: WatermarkOptions): Bitmap {
        val canvas = Canvas(bitmap)
        val useDark = options.useDarkTheme
        val textColor = if (useDark) Color.BLACK else Color.WHITE
        val shadowColor = if (useDark) Color.WHITE else Color.BLACK

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = bitmap.width * 0.03f // 3% of width
            setShadowLayer(4f, 2f, 2f, shadowColor)
        }

        val margin = bitmap.width * 0.05f
        var yPos = bitmap.height - margin

        if (options.showExif) {
            val exifString = buildExifString(exifData)
            if (exifString.isNotEmpty()) {
                val textBounds = Rect()
                paint.getTextBounds(exifString, 0, exifString.length, textBounds)
                canvas.drawText(exifString, bitmap.width - margin - textBounds.width(), yPos, paint)
                yPos -= textBounds.height() * 1.5f
            }
        }

        if (options.showDeviceBrand) {
            val brandString = buildBrandString(exifData)
            val brandPaint = Paint(paint).apply {
                typeface = Typeface.DEFAULT_BOLD
            }
            val textBounds = Rect()
            brandPaint.getTextBounds(brandString, 0, brandString.length, textBounds)
            canvas.drawText(brandString, bitmap.width - margin - textBounds.width(), yPos, brandPaint)
        }

        return bitmap
    }

    private fun drawFrame(bitmap: Bitmap, exifData: ExifData, options: WatermarkOptions): Bitmap {
        val frameHeight = (bitmap.height * 0.10f).roundToInt() // 10% chin
        val newHeight = bitmap.height + frameHeight
        
        val finalBitmap = Bitmap.createBitmap(bitmap.width, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        
        val useDark = options.useDarkTheme
        val bgColor = if (useDark) Color.BLACK else Color.WHITE
        val textColor = if (useDark) Color.WHITE else Color.BLACK
        val secondaryTextColor = if (useDark) Color.LTGRAY else Color.GRAY

        // Draw background
        canvas.drawColor(bgColor)
        
        // Draw original image
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        // Draw Text in Chin
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = frameHeight * 0.25f
        }
        
        val leftMargin = bitmap.width * 0.05f
        val centerY = bitmap.height + (frameHeight / 2f) + (paint.textSize / 3f)

        // Left side: Brand / Model
        if (options.showDeviceBrand) {
            val brandPaint = Paint(paint).apply {
                typeface = Typeface.DEFAULT_BOLD
                textSize = frameHeight * 0.3f
            }
            val brandString = buildBrandString(exifData)
            canvas.drawText(brandString, leftMargin, centerY, brandPaint)
        }

        // Right side: EXIF
        if (options.showExif) {
            val exifString = buildExifString(exifData)
            val exifPaint = Paint(paint).apply {
                color = secondaryTextColor
                textSize = frameHeight * 0.2f
            }
            val textBounds = Rect()
            exifPaint.getTextBounds(exifString, 0, exifString.length, textBounds)
            canvas.drawText(exifString, bitmap.width - leftMargin - textBounds.width(), centerY, exifPaint)
        }

        return finalBitmap
    }

    private fun buildBrandString(exif: ExifData): String {
        return if (!exif.make.isNullOrEmpty() && !exif.model.isNullOrEmpty()) {
            if (exif.model.contains(exif.make, ignoreCase = true)) {
                exif.model
            } else {
                "${exif.make} ${exif.model}"
            }
        } else {
            exif.model ?: exif.make ?: "Shot on Device"
        }
    }

    private fun buildExifString(exif: ExifData): String {
        val parts = mutableListOf<String>()
        exif.focalLength?.let { parts.add(it) }
        exif.aperture?.let { parts.add(it) }
        exif.shutterSpeed?.let { parts.add(it) }
        exif.iso?.let { parts.add(it) }
        return parts.joinToString(" • ")
    }
}
