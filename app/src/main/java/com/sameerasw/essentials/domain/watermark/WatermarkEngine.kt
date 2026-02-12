package com.sameerasw.essentials.domain.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

enum class ColorMode {
    LIGHT,
    DARK,
    ACCENT_LIGHT,
    ACCENT_DARK
}

data class WatermarkOptions(
    val style: WatermarkStyle = WatermarkStyle.FRAME,
    val showDeviceBrand: Boolean = true,
    val showExif: Boolean = true,
    // Granular EXIF options
    val showFocalLength: Boolean = true,
    val showAperture: Boolean = true,
    val showIso: Boolean = true,
    val showShutterSpeed: Boolean = true,
    val showDate: Boolean = false,
    val outputQuality: Int = 100,
    val colorMode: ColorMode = ColorMode.LIGHT,
    val accentColor: Int = android.graphics.Color.GRAY,
    val moveToTop: Boolean = false,
    val leftAlignOverlay: Boolean = false,
    val brandTextSize: Int = 50,
    val dataTextSize: Int = 50,
    val showCustomText: Boolean = false,
    val customText: String = "",
    val customTextSize: Int = 50,
    val padding: Int = 50,
    val borderStroke: Int = 0,
    val borderCorner: Int = 0,
    val showLogo: Boolean = false,
    val logoResId: Int? = null,
    val logoSize: Int = 50,
    val overriddenBrandText: String? = null,
    val overriddenDateText: String? = null,
    val rotation: Int = 0
)

class WatermarkEngine(
    private val context: Context,
    private val metadataProvider: MetadataProvider
) {
    suspend fun processImage(uri: Uri, options: WatermarkOptions): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw IllegalStateException("Cannot open input stream")
        
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
                    androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
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
        
        // Apply Rotation
        val rotated = if (options.rotation != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(options.rotation.toFloat()) }
            val rb = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rb != bitmap) bitmap.recycle()
            rb
        } else {
            bitmap
        }

        val result = when (options.style) {
            WatermarkStyle.OVERLAY -> drawOverlay(rotated, exifData, options)
            WatermarkStyle.FRAME -> drawFrame(rotated, exifData, options)
        }
        
        applyBorder(result, options)
    }

    private fun drawOverlay(bitmap: Bitmap, exifData: ExifData, options: WatermarkOptions): Bitmap {
        val canvas = Canvas(bitmap)
        
        // Derive Colors
        val colors = deriveColors(options)
        val shadowColor = colors.shadowColor
        val overlayTextColor = colors.overlayTextColor
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlayTextColor
            textSize = bitmap.width * 0.03f // 3% of width
            setShadowLayer(4f, 2f, 2f, shadowColor)
        }

        val strokeSpacer = (bitmap.width * (options.borderStroke / 1000f))
        val margin = (bitmap.width * (options.padding / 1000f)) + strokeSpacer
        var yPos = bitmap.height - margin

        // Apply scaling
        val brandScale = 0.5f + (options.brandTextSize / 100f)
        val dataScale = 0.5f + (options.dataTextSize / 100f)
        val baseSize = bitmap.width * 0.03f
        
        // Logo Calculation
        var logoBitmap: Bitmap? = null
        var logoWidth = 0f
        var logoHeight = 0f
        
        if (options.showLogo && options.logoResId != null) {
            val logoScale = options.logoSize / 100f
            val logoSizePx = baseSize * 4f * logoScale
            logoBitmap = loadVectorBitmap(context, options.logoResId, overlayTextColor)
            if (logoBitmap != null) {
                val ratio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                logoHeight = logoSizePx
                logoWidth = logoHeight * ratio
            }
        }

        // Draw EXIF
        if (options.showExif) {
            val exifPaint = Paint(paint).apply { textSize = baseSize * dataScale }
            val exifItems = buildExifList(exifData, options)
            if (exifItems.isNotEmpty()) {
                val maxWidth = bitmap.width - (margin * 2)
                val rows = wrapExifItems(exifItems, exifPaint, maxWidth)
                val reversedRows = rows.reversed()
                
                for (row in reversedRows) {
                    val rowWidth = measureRowWidth(row, exifPaint)
                    val rowHeight = measureRowHeight(row, exifPaint)
                    val xPos = if (options.leftAlignOverlay) margin else (bitmap.width - margin - rowWidth)
                    drawExifRow(canvas, row, xPos, yPos, exifPaint, shadowColor)
                    yPos -= rowHeight * 1.2f
                }
            }
        }

        // Draw Custom Text
        if (options.showCustomText && options.customText.isNotEmpty()) {
            val customScale = 0.5f + (options.customTextSize / 100f)
            val customPaint = Paint(paint).apply {
                textSize = baseSize * customScale
                typeface = Typeface.DEFAULT 
            }
            val textBounds = Rect()
            customPaint.getTextBounds(options.customText, 0, options.customText.length, textBounds)
            if (options.showExif) yPos -= (customPaint.textSize * 0.5f)
            val xPos = if (options.leftAlignOverlay) margin else (bitmap.width - margin - textBounds.width())
            canvas.drawText(options.customText, xPos, yPos, customPaint)
            yPos -= customPaint.textSize * 1.2f
        }

        // Draw Brand
        if (options.showDeviceBrand) {
            val brandString = buildBrandString(exifData, options)
            val brandPaint = Paint(paint).apply {
                typeface = Typeface.DEFAULT_BOLD
                textSize = baseSize * brandScale
            }
            val textBounds = Rect()
            brandPaint.getTextBounds(brandString, 0, brandString.length, textBounds)
            val xPos = if (options.leftAlignOverlay) margin else (bitmap.width - margin - textBounds.width())
            canvas.drawText(brandString, xPos, yPos, brandPaint)
            yPos -= brandPaint.textSize * 1.2f
        }

        // Draw the Logo at the TOP of text content
        if (logoBitmap != null) {
            val logoX = if (options.leftAlignOverlay) margin else (bitmap.width - margin - logoWidth)
            val logoY = yPos - logoHeight - (baseSize * 0.2f) // Increased spacing
            
            val destRect = RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight)
            
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(shadowColor!!, android.graphics.PorterDuff.Mode.SRC_IN)
                alpha = 128
            }
            val shadowRect = RectF(destRect).apply { offset(2f, 2f) }
            canvas.drawBitmap(logoBitmap, null, shadowRect, shadowPaint)
            
            canvas.drawBitmap(logoBitmap, null, destRect, null)
        }

        return bitmap
    }

    private fun drawFrame(bitmap: Bitmap, exifData: ExifData, options: WatermarkOptions): Bitmap {
        var baseFrameHeight = (bitmap.height * 0.10f).roundToInt()
        
        // Derive Colors
        val colors = deriveColors(options)
        val bgColor = colors.bgColor
        val textColor = colors.textColor
        val secondaryTextColor = colors.secondaryTextColor
        
        // Setup paints early to measure
        // Setup paints early to measure
        val brandScale = 0.5f + (options.brandTextSize / 100f)
        val dataScale = 0.5f + (options.dataTextSize / 100f)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
             color = textColor
             textSize = (baseFrameHeight * 0.3f) * brandScale
             typeface = Typeface.DEFAULT_BOLD
        }

        val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
             color = secondaryTextColor
             textSize = (baseFrameHeight * 0.2f) * dataScale
        }
        
        val margin = bitmap.width * (options.padding / 1000f) // 0 to 10%
        
        val maxAvailableWidth = if (options.showDeviceBrand) {
            (bitmap.width - margin * 2) * 0.6f
        } else {
            (bitmap.width - margin * 2)
        }
        
        var exifRows: List<List<ExifItem>> = emptyList()
        var totalExifHeight = 0f
        
        if (options.showExif) {
             val exifItems = buildExifList(exifData, options)
             if (exifItems.isNotEmpty()) {
                 exifRows = wrapExifItems(exifItems, exifPaint, maxAvailableWidth)
                 totalExifHeight = exifRows.size * (exifPaint.textSize * 1.5f)
             }
        }
        
        // Dynamic Height Calculation 
        var leftSideHeight = 0f
        if (options.showDeviceBrand) {
            leftSideHeight += brandPaint.textSize
        }
        if (options.showCustomText && options.customText.isNotEmpty()) {
            val customTextPaint = Paint(brandPaint).apply {
                val customScale = 0.5f + (options.customTextSize / 100f)
                textSize = (baseFrameHeight * 0.3f) * customScale
                typeface = Typeface.DEFAULT
            }
            if (options.showDeviceBrand) {
                leftSideHeight += (baseFrameHeight * 0.1f)
            }
            leftSideHeight += customTextPaint.textSize
        }

        val contentHeightLeft = leftSideHeight
        val contentHeightRight = totalExifHeight
        
        val minHeight = max(brandPaint.textSize, exifPaint.textSize) * 2f
        
        val strokeSpacer = (bitmap.width * (options.borderStroke / 1000f)).toInt()
        val calculatedHeight = max(contentHeightLeft, contentHeightRight) + (margin * 2) + strokeSpacer
        
        val finalFrameHeight = max(minHeight.roundToInt(), calculatedHeight.roundToInt())
        
        val newHeight = bitmap.height + finalFrameHeight
        
        val finalBitmap = Bitmap.createBitmap(bitmap.width, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        
        // Draw background
        canvas.drawColor(bgColor)
        
        // Create rounded version of source bitmap if needed
        val sourceToDraw = if (options.borderCorner > 0) {
             val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
             val srcCanvas = Canvas(output)
             val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
             val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            
             val minDim = kotlin.math.min(bitmap.width, bitmap.height)
             val radius = minDim * (options.borderCorner / 1000f) 
             
             srcCanvas.drawRoundRect(rect, radius, radius, srcPaint)
             srcPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
             srcCanvas.drawBitmap(bitmap, 0f, 0f, srcPaint)
             output
        } else {
             bitmap
        }

        // Draw Image and Text
        if (options.moveToTop) {
            // Draw Image shifted down by frameHeight
            canvas.drawBitmap(sourceToDraw, 0f, finalFrameHeight.toFloat(), null)
            
            // Draw Text in "Forehead"
            val centerY = (finalFrameHeight - strokeSpacer) / 2f
            drawFrameContent(
                canvas, exifData, options, margin, centerY, 
                brandPaint, exifPaint, exifRows, bitmap.width
            )
            
        } else {
            // Draw Image at 0,0
            canvas.drawBitmap(sourceToDraw, 0f, 0f, null)
            
            // Draw Text in "Chin"
            val centerY = bitmap.height + strokeSpacer + ((finalFrameHeight - strokeSpacer) / 2f)
            drawFrameContent(
                canvas, exifData, options, margin, centerY, 
                brandPaint, exifPaint, exifRows, bitmap.width
            )
        }
        
        if (sourceToDraw != bitmap) {
            sourceToDraw.recycle()
        }

        return finalBitmap
    }

    private fun drawFrameContent(
        canvas: Canvas, exifData: ExifData, options: WatermarkOptions,
        margin: Float, centerY: Float,
        brandPaint: Paint, exifPaint: Paint,
        exifRows: List<List<ExifItem>>, canvasWidth: Int

    ) {
        val colors = deriveColors(options)
        val textColor = colors.textColor
        
        // Logo Calculation
        var logoBitmap: Bitmap? = null
        var logoWidth = 0f
        var logoHeight = 0f
        val logoSpacing = brandPaint.textSize * 0.5f
        
        if (options.showLogo && options.logoResId != null) {
            val logoScale = options.logoSize / 100f
            val logoSizePx = brandPaint.textSize * 2.5f * logoScale
            logoBitmap = loadVectorBitmap(context, options.logoResId, textColor)
            if (logoBitmap != null) {
                val ratio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                logoHeight = logoSizePx
                logoWidth = logoHeight * ratio
            }
        }

        val logoAreaWidth = if (logoBitmap != null) logoWidth + logoSpacing else 0f

        // Brand & Custom Text on Left
        var currentLeftY = centerY
        var totalLeftHeight = 0f
        val customScale =  0.5f + (options.customTextSize / 100f)
        val customPaint = Paint(brandPaint).apply {
            textSize = brandPaint.textSize * (customScale / (0.5f + (options.brandTextSize / 100f)))
            typeface = Typeface.DEFAULT
        }

        if (options.showDeviceBrand) totalLeftHeight += brandPaint.textSize
        if (options.showCustomText && options.customText.isNotEmpty()) {
             if (options.showDeviceBrand) totalLeftHeight += (brandPaint.textSize * 0.2f)
             totalLeftHeight += customPaint.textSize
        }

        currentLeftY = centerY - (totalLeftHeight / 2f) + (brandPaint.textSize / 1.5f)
        
        // Draw Logo
        if (logoBitmap != null) {
            val logoX = margin
            val logoY = centerY - (logoHeight / 2f)
            canvas.drawBitmap(logoBitmap, null, RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight), null)
        }

        val textX = margin + logoAreaWidth

        if (options.showDeviceBrand) {
            val brandString = buildBrandString(exifData, options)
            canvas.drawText(brandString, textX, currentLeftY, brandPaint)
            currentLeftY += (brandPaint.textSize * 0.2f) + customPaint.textSize
        } else if (options.showCustomText && options.customText.isNotEmpty()) {
             currentLeftY = centerY - (totalLeftHeight / 2f) + customPaint.textSize
        }

        if (options.showCustomText && options.customText.isNotEmpty()) {
             canvas.drawText(options.customText, textX, currentLeftY, customPaint)
        }
        
        // Exif on Right
        if (options.showExif && exifRows.isNotEmpty()) {
            val lineHeight = exifPaint.textSize * 1.5f
            val centeringOffset = (exifRows.size - 1) * lineHeight / 2f
            var currentY = (centerY + exifPaint.textSize / 3f) - centeringOffset
            
            for (row in exifRows) {
                val rowWidth = measureRowWidth(row, exifPaint)
                val xPos = canvasWidth - margin - rowWidth
                drawExifRow(canvas, row, xPos, currentY, exifPaint, null) 
                currentY += lineHeight
            }
        }
    }
    
    private fun wrapExifItems(items: List<ExifItem>, paint: Paint, maxWidth: Float): List<List<ExifItem>> {
        val rows = mutableListOf<List<ExifItem>>()
        if (items.isEmpty()) return rows
        
        var currentRow = mutableListOf<ExifItem>()
        var currentWidth = 0f
        val separatorWidth = 0f
        val itemSpacing = paint.textSize * 0.8f

        for (item in items) {
            val itemWidth = measureItemWidth(item, paint)
            
            if (currentRow.isEmpty()) {
                currentRow.add(item)
                currentWidth += itemWidth
            } else {
                if (currentWidth + itemSpacing + itemWidth <= maxWidth) {
                     currentRow.add(item)
                     currentWidth += itemSpacing + itemWidth
                } else {
                    rows.add(currentRow)
                    currentRow = mutableListOf(item)
                    currentWidth = itemWidth
                }
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        return rows
    }
    
    private fun measureItemWidth(item: ExifItem, paint: Paint): Float {
        // Icon + Padding + Text
        val iconSize = paint.textSize * 1.2f
        val padding = paint.textSize * 0.4f
        val textWidth = paint.measureText(item.text)
        return iconSize + padding + textWidth
    }
    
    private fun measureRowWidth(row: List<ExifItem>, paint: Paint): Float {
        var width = 0f
        val itemSpacing = paint.textSize * 0.8f
        for (i in row.indices) {
            width += measureItemWidth(row[i], paint)
            if (i < row.size - 1) width += itemSpacing
        }
        return width
    }
    
    private fun measureRowHeight(row: List<ExifItem>, paint: Paint): Float {
        return paint.textSize * 1.5f // Use standard height
    }

    private fun drawExifRow(
        canvas: Canvas, row: List<ExifItem>, 
        xStart: Float, yPos: Float, 
        paint: Paint, shadowColor: Int?
    ) {
        var currentX = xStart
        val iconSize = paint.textSize * 1.2f
        val padding = paint.textSize * 0.4f
        val itemSpacing = paint.textSize * 0.8f
        
        val iconY = yPos - (paint.textSize / 2f) - (iconSize / 2f)
        
        for (item in row) {
            // Draw Icon
            val iconBitmap = loadVectorBitmap(context, item.iconRes, paint.color)
            if (iconBitmap != null) {
                val destRect = Rect(
                    currentX.toInt(), 
                    iconY.toInt(), 
                    (currentX + iconSize).toInt(), 
                    (iconY + iconSize).toInt()
                )
                
                if (shadowColor != null) {
                     val shadowPaint = Paint(paint).apply { 
                         color = shadowColor
                         colorFilter = android.graphics.PorterDuffColorFilter(shadowColor, android.graphics.PorterDuff.Mode.SRC_IN)
                     }
                     val shadowRect = Rect(destRect)
                     shadowRect.offset(2, 2)
                     canvas.drawBitmap(iconBitmap, null, shadowRect, shadowPaint)
                }
                
                canvas.drawBitmap(iconBitmap, null, destRect, null) // Already tinted if we created it tinted
            }
            
            currentX += iconSize + padding
            
            // Draw Text
            canvas.drawText(item.text, currentX, yPos, paint)
            
            currentX += paint.measureText(item.text) + itemSpacing
        }
    }

    // Cache for bitmaps
    private val iconCache = mutableMapOf<Int, Bitmap>()
    
    private fun loadVectorBitmap(context: Context, resId: Int, color: Int): Bitmap? {
        
        try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId) ?: return null
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.setTint(color)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            return null
        }
    }

    private fun buildBrandString(exif: ExifData, options: WatermarkOptions): String {
        options.overriddenBrandText?.let { return it }
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

    private data class ExifItem(val text: String, val iconRes: Int)

    private fun buildExifList(exif: ExifData, options: WatermarkOptions): List<ExifItem> {
        val list = mutableListOf<ExifItem>()
        
        if (options.showFocalLength) exif.focalLength?.let { 
            list.add(ExifItem(it, R.drawable.rounded_control_camera_24)) 
        }
        if (options.showAperture) exif.aperture?.let { 
            list.add(ExifItem(it, R.drawable.rounded_camera_24)) 
        }
        if (options.showShutterSpeed) exif.shutterSpeed?.let { 
            list.add(ExifItem(formatShutterSpeed(it), R.drawable.rounded_shutter_speed_24)) 
        }
        if (options.showIso) exif.iso?.let { 
            list.add(ExifItem(it, R.drawable.rounded_grain_24)) 
        }
        if (options.showDate) (options.overriddenDateText ?: exif.date)?.let { 
            list.add(ExifItem(formatDate(it), R.drawable.rounded_date_range_24)) 
        }
        
        return list
    }

    private fun formatDate(dateString: String): String {
        try {
            // Input format: yyyy:MM:dd HH:mm:ss
            val inputFormat = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
            val date = inputFormat.parse(dateString) ?: return dateString
            
            // Output format components
            val dayFormat = java.text.SimpleDateFormat("d", java.util.Locale.US)
            val monthYearFormat = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US)
            
            // Use system time format (12/24h)
            val timeFormat = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
            
            val day = dayFormat.format(date).toInt()
            val suffix = getDaySuffix(day)
            
            return "$day$suffix ${monthYearFormat.format(date)}, ${timeFormat.format(date)}"
        } catch (e: Exception) {
            return dateString
        }
    }
    
    private fun getDaySuffix(n: Int): String {
        if (n in 11..13) return "th"
        return when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    private fun formatShutterSpeed(raw: String): String {
        // raw usually comes as "0.02s" or "1/100s" from MetadataProvider due to appended "s" in provider
        val value = raw.removeSuffix("s")
        
        // If it's already a fraction, keep it
        if (value.contains("/")) return raw
        
        return try {
            val exposureTime = value.toDouble()
            
            if (exposureTime <= 0) return raw
            
            if (exposureTime >= 1.0) {
                // For 1.0s or more, show as integer if possible, else 1 decimal
                val formatted = if (exposureTime % 1.0 == 0.0) {
                    exposureTime.toInt().toString()
                } else {
                    java.lang.String.format(java.util.Locale.US, "%.1f", exposureTime)
                }
                "${formatted}s"
            } else {
                // For sub-second, convert to 1/N format
                val denominator = (1.0 / exposureTime).roundToInt()
                "1/${denominator}s"
            }
        } catch (e: Exception) {
            raw
        }
    }

    private fun applyBorder(bitmap: Bitmap, options: WatermarkOptions): Bitmap {
        if (options.borderStroke == 0 && options.borderCorner == 0) return bitmap

        val roundedBitmap = if (options.borderCorner > 0 && options.style != WatermarkStyle.FRAME) {
             val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
             val canvas = Canvas(output)
             val paint = Paint(Paint.ANTI_ALIAS_FLAG)
             val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            
             // Mapping: 0-100slider -> 0-10% of min dimension
             val minDim = kotlin.math.min(bitmap.width, bitmap.height)
             val radius = minDim * (options.borderCorner / 1000f) // Max 10%
             
             canvas.drawRoundRect(rect, radius, radius, paint)
             paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
             canvas.drawBitmap(bitmap, 0f, 0f, paint)
             
             if (bitmap != output) bitmap.recycle()
             output
        } else {
             bitmap
        }
        
        // Border Stroke  (Expand Canvas)
        val finalBitmap = if (options.borderStroke > 0) {
             val strokeWidth = (bitmap.width * (options.borderStroke / 1000f)).toInt()
             
             val newWidth = roundedBitmap.width + (strokeWidth * 2)
             val newHeight = roundedBitmap.height + (strokeWidth * 2)
             
             val output = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
             val canvas = Canvas(output)
             
             val colors = deriveColors(options)
             val bgColor = colors.bgColor
             
             canvas.drawColor(bgColor)
             
             canvas.drawBitmap(roundedBitmap, strokeWidth.toFloat(), strokeWidth.toFloat(), null)
             
             if (roundedBitmap != output) roundedBitmap.recycle()
             output
        } else {
             roundedBitmap
        }
        
        return finalBitmap
    }

    private data class DerivedColors(
        val bgColor: Int, 
        val textColor: Int, 
        val secondaryTextColor: Int,
        val shadowColor: Int,
        val overlayTextColor: Int
    )

    private fun deriveColors(options: WatermarkOptions): DerivedColors {
        return when (options.colorMode) {
            ColorMode.LIGHT -> DerivedColors(Color.WHITE, Color.BLACK, Color.GRAY, Color.BLACK, Color.WHITE)
            ColorMode.DARK -> DerivedColors(Color.BLACK, Color.WHITE, Color.LTGRAY, Color.WHITE, Color.BLACK)
            ColorMode.ACCENT_LIGHT -> getAccentColors(options.accentColor, false)
            ColorMode.ACCENT_DARK -> getAccentColors(options.accentColor, true)
        }
    }

    private fun getAccentColors(baseColor: Int, dark: Boolean): DerivedColors {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(baseColor, hsl)
        
        return if (dark) {
            // Accent Dark: Dark BG, Light Text
            hsl[2] = 0.15f // Dark BG
            val bgColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            hsl[2] = 0.9f // Light Text
            val textColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            hsl[2] = 0.7f // Secondary Text
            val secondaryTextColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            
            DerivedColors(bgColor, textColor, secondaryTextColor, Color.WHITE, bgColor)
        } else {
            // Accent Light: Light BG, Dark Text
            hsl[2] = 0.95f // Light BG
            val bgColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            hsl[2] = 0.15f // Dark Text
            val textColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            hsl[2] = 0.4f // Secondary Text
            val secondaryTextColor = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
            
            DerivedColors(bgColor, textColor, secondaryTextColor, Color.BLACK, bgColor)
        }
    }
}
