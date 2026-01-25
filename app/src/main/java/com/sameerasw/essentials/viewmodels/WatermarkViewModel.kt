package com.sameerasw.essentials.viewmodels

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.ColorMode
import com.sameerasw.essentials.domain.watermark.ExifData
import com.sameerasw.essentials.domain.watermark.MetadataProvider
import com.sameerasw.essentials.domain.watermark.WatermarkEngine
import com.sameerasw.essentials.domain.watermark.WatermarkOptions
import com.sameerasw.essentials.domain.watermark.WatermarkRepository
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed class WatermarkUiState {
    data object Idle : WatermarkUiState()
    data object Processing : WatermarkUiState()
    data class Success(val file: File) : WatermarkUiState()
    data class Error(val message: String) : WatermarkUiState()
}

class WatermarkViewModel(
    private val watermarkEngine: WatermarkEngine,
    private val watermarkRepository: WatermarkRepository,
    private val metadataProvider: MetadataProvider,
    private val context: Context
) : ViewModel() {

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val metadataProvider = MetadataProvider(appContext)
                val engine = WatermarkEngine(appContext, metadataProvider)
                val repository = WatermarkRepository(appContext)
                return WatermarkViewModel(engine, repository, metadataProvider, appContext) as T
            }
        }
    }

    private val _uiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    private val _previewUiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val previewUiState: StateFlow<WatermarkUiState> = _previewUiState.asStateFlow()

    private val _options = MutableStateFlow(WatermarkOptions())
    val options: StateFlow<WatermarkOptions> = _options.asStateFlow()
    
    // Transient overrides (resets on image load)
    private val _currentBrandText = MutableStateFlow<String?>(null)
    val currentBrandText: StateFlow<String?> = _currentBrandText.asStateFlow()
    
    private val _currentCustomText = MutableStateFlow("")
    val currentCustomText: StateFlow<String> = _currentCustomText.asStateFlow()
    
    private val _currentDateText = MutableStateFlow<String?>(null)
    val currentDateText: StateFlow<String?> = _currentDateText.asStateFlow()
    
    // Transient logo state (not persisted, depends on image EXIF)
    private val _logoResId = MutableStateFlow<Int?>(null)
    val logoResId: StateFlow<Int?> = _logoResId.asStateFlow()
    
    private val _showLogo = MutableStateFlow(false)
    val showLogo: StateFlow<Boolean> = _showLogo.asStateFlow()

    private var previewSourceBitmap: android.graphics.Bitmap? = null
    private var currentUri: Uri? = null

    init {
        viewModelScope.launch {
            watermarkRepository.watermarkOptions.collectLatest { savedOptions ->
                _options.value = savedOptions
                updatePreview()
            }
        }
    }

    private fun detectOemLogo(exif: ExifData): Int? {
        val make = exif.make?.lowercase() ?: ""
        val model = exif.model?.lowercase() ?: ""
        
        return when {
            make.contains("apple") || model.contains("iphone") -> R.drawable.apple
            make.contains("google") || model.contains("pixel") -> R.drawable.google
            make.contains("samsung") -> R.drawable.samsung
            make.contains("xiaomi") || make.contains("redmi") || make.contains("poco") -> R.drawable.xiaomi
            make.contains("oppo") -> R.drawable.oppo
            make.contains("vivo") -> R.drawable.vivo
            make.contains("sony") -> R.drawable.sony
            make.contains("nothing") -> R.drawable.nothing
            make.contains("cmf") -> R.drawable.cmf
            make.contains("motorola") || make.contains("moto") -> R.drawable.moto
            else -> null
        }
    }

    fun loadPreview(uri: Uri) {
        currentUri = uri
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Decode scaled version
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = android.graphics.BitmapFactory.Options()
                options.inJustDecodeBounds = true
                android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                // Calculate sample size to fit around 1080p
                val reqWidth = 1080
                val reqHeight = 1080
                var inSampleSize = 1
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    this.inMutable = true // Ensure mutable
                }
                
                val is2 = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(is2, null, decodeOptions)
                is2?.close()
                
                if (bitmap != null) {
                    previewSourceBitmap = bitmap
                    
                    // Always derive logo from EXIF on load
                    val exif = metadataProvider.extractExif(uri)
                    val detected = detectOemLogo(exif)
                    _logoResId.value = detected
                    _showLogo.value = detected != null
                    
                    // Initialize transient text
                    _currentBrandText.value = buildBrandString(exif)
                    _currentCustomText.value = _options.value.customText
                    _currentDateText.value = exif.date
                    
                    extractColorFromUri(uri)
                    updatePreview()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    fun formatDate(dateString: String): String {
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

    private fun extractColorFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = 2 
                }
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                if (bitmap != null) {
                    androidx.palette.graphics.Palette.from(bitmap)
                        .maximumColorCount(32)
                        .clearFilters()
                        .generate { palette ->
                            val color = palette?.vibrantSwatch?.rgb
                                ?: palette?.mutedSwatch?.rgb
                                ?: palette?.lightVibrantSwatch?.rgb
                                ?: palette?.darkVibrantSwatch?.rgb
                                ?: palette?.dominantSwatch?.rgb
                                ?: android.graphics.Color.GRAY
                            
                            viewModelScope.launch {
                                watermarkRepository.updateAccentColor(color)
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePreview() {
        val bitmap = previewSourceBitmap ?: return
        val uri = currentUri ?: return
        viewModelScope.launch {
            _previewUiState.value = WatermarkUiState.Processing
            try {
                kotlinx.coroutines.delay(600)
                val workingBitmap = bitmap.copy(bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888, true)
                
                // Merge transient logo settings with base options
                val currentOptions = _options.value.copy(
                    logoResId = _logoResId.value,
                    showLogo = _showLogo.value,
                    overriddenBrandText = _currentBrandText.value,
                    customText = _currentCustomText.value,
                    showCustomText = _currentCustomText.value.isNotEmpty(),
                    overriddenDateText = _currentDateText.value
                )
                
                val result = watermarkEngine.processBitmap(workingBitmap, uri, currentOptions)

                val timestamp = System.currentTimeMillis()
                val file = File(context.cacheDir, "preview_watermark_$timestamp.jpg")
                val out = java.io.FileOutputStream(file)
                result.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                out.close()
                
                (_previewUiState.value as? WatermarkUiState.Success)?.file?.let { oldFile ->
                    if (oldFile.exists() && oldFile.name.startsWith("preview_watermark_")) {
                        oldFile.delete()
                    }
                }
                
                _previewUiState.value = WatermarkUiState.Success(file)
            } catch (e: Exception) {
                e.printStackTrace()
                _previewUiState.value = WatermarkUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setStyle(style: WatermarkStyle) {
        viewModelScope.launch {
            watermarkRepository.updateStyle(style)
        }
    }

    fun setShowBrand(show: Boolean) {
        viewModelScope.launch {
            watermarkRepository.updateShowBrand(show)
        }
    }

    fun setShowExif(show: Boolean) {
        viewModelScope.launch {
            watermarkRepository.updateShowExif(show)
        }
    }

    fun setExifSettings(
        focalLength: Boolean,
        aperture: Boolean,
        iso: Boolean,
        shutterSpeed: Boolean,
        date: Boolean
    ) {
        viewModelScope.launch {
            watermarkRepository.updateExifSettings(focalLength, aperture, iso, shutterSpeed, date)
            // Trigger preview update
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setColorMode(mode: ColorMode) {
        viewModelScope.launch {
            watermarkRepository.updateColorMode(mode)
        }
    }

    fun setMoveToTop(move: Boolean) {
        viewModelScope.launch {
            watermarkRepository.updateMoveToTop(move)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setLeftAlign(left: Boolean) {
        viewModelScope.launch {
            watermarkRepository.updateLeftAlign(left)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setBrandTextSize(size: Int) {
        viewModelScope.launch {
            watermarkRepository.updateBrandTextSize(size)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setDataTextSize(size: Int) {
        viewModelScope.launch {
            watermarkRepository.updateDataTextSize(size)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setCustomTextSettings(show: Boolean, text: String, size: Int) {
        viewModelScope.launch {
            watermarkRepository.updateCustomTextSettings(show, text, size)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setCustomTextSize(size: Int) {
        viewModelScope.launch {
            watermarkRepository.updateCustomTextSize(size)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setPadding(padding: Int) {
        viewModelScope.launch {
            watermarkRepository.updatePadding(padding)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setBorderStroke(stroke: Int) {
        viewModelScope.launch {
            watermarkRepository.updateBorderStroke(stroke)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setBorderCorner(corner: Int) {
        viewModelScope.launch {
            watermarkRepository.updateBorderCorner(corner)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun setLogoSettings(show: Boolean, resId: Int?, size: Int) {
        _showLogo.value = show
        _logoResId.value = resId
        viewModelScope.launch {
            watermarkRepository.updateLogoSize(size)
            watermarkRepository.updateLogoShow(show)
            updatePreview()
        }
    }

    fun setShowLogo(show: Boolean) {
        _showLogo.value = show
        viewModelScope.launch {
            watermarkRepository.updateLogoShow(show)
            updatePreview()
        }
    }

    fun setLogoResId(resId: Int?) {
        _logoResId.value = resId
        updatePreview()
    }

    fun setLogoSize(size: Int) {
        viewModelScope.launch {
            watermarkRepository.updateLogoSize(size)
            previewSourceBitmap?.let { updatePreview() }
        }
    }

    fun updateOverriddenTexts(brand: String, custom: String, date: String?) {
        _currentBrandText.value = brand
        _currentCustomText.value = custom
        _currentDateText.value = date
        updatePreview()
    }

    fun saveImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = WatermarkUiState.Processing
            try {
                // Merge transient logo options 
                val finalOptions = _options.value.copy(
                    logoResId = _logoResId.value,
                    showLogo = _showLogo.value,
                    overriddenBrandText = _currentBrandText.value,
                    customText = _currentCustomText.value,
                    showCustomText = _currentCustomText.value.isNotEmpty(),
                    overriddenDateText = _currentDateText.value
                )
                // Process image to a temporary file first
                val tempFile = watermarkEngine.processImage(uri, finalOptions)

                // Save to MediaStore (Gallery)
                val values = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        "WM_${System.currentTimeMillis()}.jpg"
                    )
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    // RELATIVE_PATH is available on Android 10+ (API 29)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(
                            android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                            "Pictures/Essentials"
                        )
                    }
                }

                val resolver = context.contentResolver
                val collection =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }

                val resultUri = resolver.insert(collection, values)

                if (resultUri != null) {
                    resolver.openOutputStream(resultUri)?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    _uiState.value =
                        WatermarkUiState.Success(tempFile) // Or success with URI? State expects File, but it's just for success message.
                } else {
                    throw Exception("Failed to create MediaStore entry")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = WatermarkUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun shareImage(uri: Uri, onShareReady: (Uri) -> Unit) {
        viewModelScope.launch {
            _uiState.value = WatermarkUiState.Processing
            try {
                // Merge transient logo options 
                val finalOptions = _options.value.copy(
                    logoResId = _logoResId.value,
                    showLogo = _showLogo.value,
                    overriddenBrandText = _currentBrandText.value,
                    customText = _currentCustomText.value,
                    showCustomText = _currentCustomText.value.isNotEmpty(),
                    overriddenDateText = _currentDateText.value
                )
                // Process image to a temporary file
                val tempFile = watermarkEngine.processImage(uri, finalOptions)
                val savedUri = saveToMediaStore(tempFile)
                if (savedUri != null) {
                    _uiState.value = WatermarkUiState.Idle
                    onShareReady(savedUri)
                } else {
                     _uiState.value = WatermarkUiState.Error("Failed to prepare image for sharing")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = WatermarkUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun saveToMediaStore(sourceFile: File): Uri? {
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "WM_SHARE_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Essentials/Watermarks")
                }
            }
            val resolver = context.contentResolver
            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            val resultUri = resolver.insert(collection, values) ?: return null
            
            resolver.openOutputStream(resultUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            return resultUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    fun resetState() {
        _uiState.value = WatermarkUiState.Idle
    }
}
