package com.sameerasw.essentials.domain.watermark

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

data class ExifData(
    val make: String? = null,
    val model: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val date: String? = null,
    val focalLength: String? = null
)

class MetadataProvider(
    private val context: Context
) {
    fun extractExif(uri: Uri): ExifData {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return ExifData()

            val exif = ExifInterface(inputStream)
            
            ExifData(
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                shutterSpeed = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" },
                iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" },
                date = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).let { 
                    if (it > 0) {
                        val formatted = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                        "${formatted}mm"
                    } else null
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExifData()
        } finally {
            inputStream?.close()
        }
    }
}
