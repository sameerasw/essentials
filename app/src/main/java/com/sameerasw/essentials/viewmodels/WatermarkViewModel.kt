package com.sameerasw.essentials.viewmodels

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.MetadataProvider
import com.sameerasw.essentials.domain.watermark.WatermarkEngine
import com.sameerasw.essentials.domain.watermark.WatermarkOptions
import com.sameerasw.essentials.domain.watermark.WatermarkRepository
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
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
                return WatermarkViewModel(engine, repository, appContext) as T
            }
        }
    }

    private val _uiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val uiState: StateFlow<WatermarkUiState> = _uiState.asStateFlow()

    private val _previewUiState = MutableStateFlow<WatermarkUiState>(WatermarkUiState.Idle)
    val previewUiState: StateFlow<WatermarkUiState> = _previewUiState.asStateFlow()

    private val _options = MutableStateFlow(WatermarkOptions())
    val options: StateFlow<WatermarkOptions> = _options.asStateFlow()
    
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
                    updatePreview()
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
                
                val result = watermarkEngine.processBitmap(workingBitmap, uri, _options.value)

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

    fun toggleContrast() {
        viewModelScope.launch {
            watermarkRepository.updateUseDarkTheme(!_options.value.useDarkTheme)
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

    fun saveImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = WatermarkUiState.Processing
            try {
                // Process image to a temporary file first
                val tempFile = watermarkEngine.processImage(uri, _options.value)

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
                // Process image to a temporary file
                val tempFile = watermarkEngine.processImage(uri, _options.value)
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
