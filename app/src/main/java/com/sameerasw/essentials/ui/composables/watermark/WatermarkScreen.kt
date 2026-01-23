package com.sameerasw.essentials.ui.composables.watermark

import android.graphics.drawable.Icon
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil.performSliderHaptic
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic
import com.sameerasw.essentials.viewmodels.WatermarkUiState
import com.sameerasw.essentials.viewmodels.WatermarkViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WatermarkScreen(
    initialUri: Uri?,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
    viewModel: WatermarkViewModel
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current // For haptics
    var showExifSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showCustomTextSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val options by viewModel.options.collectAsState()
    val previewState by viewModel.previewUiState.collectAsState()
    val saveState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            viewModel.loadPreview(initialUri)
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is WatermarkUiState.Success -> {
                Toast.makeText(context, R.string.watermark_save_success, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is WatermarkUiState.Error -> {
                Toast.makeText(context, (saveState as WatermarkUiState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            ReusableTopAppBar(
                title = R.string.feat_watermark_title,
                hasBack = true,
                onBackClick = {
                    performUIHaptic(view)
                    onBack()
                },
                isSmall = true,
                containerColor = MaterialTheme.colorScheme.background,
                actions = {
                    val pickImageButton = @Composable {
                         // Pick Image Button
                        if (initialUri == null) {
                            // Primary when no image
                            androidx.compose.material3.Button(onClick = {
                                performUIHaptic(view)
                                onPickImage()
                            }) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24),
                                     contentDescription = stringResource(R.string.watermark_pick_image),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.watermark_pick_image))
                            }
                        } else {
                            // Secondary when image is there
                            androidx.compose.material3.OutlinedButton(onClick = {
                                performUIHaptic(view)
                                onPickImage()
                            }) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24),
                                     contentDescription = stringResource(R.string.watermark_pick_image),
                                     modifier = Modifier.size(18.dp)
                                 )
                            }
                        }
                    }

                    pickImageButton()

                    // Save/Share Menu Button
                    if (initialUri != null) {
                        Spacer(Modifier.size(8.dp))
                        var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        Box {
                             // Save Button (Primary)
                             androidx.compose.material3.Button(onClick = { 
                                 performUIHaptic(view)
                                 showMenu = true 
                             }) {
                                 Icon(
                                     painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_save_24),
                                     contentDescription = stringResource(R.string.action_save),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.action_save))
                             }
                             
                             com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
                                 expanded = showMenu,
                                 onDismissRequest = { showMenu = false },
                             ) {
                                 // Share Option
                                 com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_share)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_share_24),
                                             contentDescription = null,
                                             modifier = Modifier.size(20.dp)
                                         )
                                     },
                                     onClick = {
                                         showMenu = false
                                          initialUri.let { uri ->
                                             viewModel.shareImage(uri) { sharedUri ->
                                                 val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                     type = "image/jpeg"
                                                     putExtra(android.content.Intent.EXTRA_STREAM, sharedUri)
                                                     addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                 }
                                                 context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.action_share)))
                                             }
                                        } 
                                     },
                                     enabled = saveState !is WatermarkUiState.Processing
                                 )
                                 
                                 // Save Option
                                 com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_save)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_save_24),
                                             contentDescription = null,
                                              modifier = Modifier.size(20.dp)
                                         )
                                     },
                                     onClick = {
                                         showMenu = false
                                         viewModel.saveImage(initialUri)
                                     },
                                     enabled = saveState !is WatermarkUiState.Processing
                                 )
                             }
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        
        val maxPreviewHeightDp = screenHeightDp * 0.6f
        val minPreviewHeightDp = screenHeightDp * 0.3f
        
        val maxPx = with(density) { maxPreviewHeightDp.toPx() }
        val minPx = with(density) { minPreviewHeightDp.toPx() }
        
        var previewHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(maxPx) }
        
        val nestedScrollConnection = androidx.compose.runtime.remember {
            object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    // Swiping Up (delta < 0): Collapse
                    if (delta < 0) {
                        val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                        val consumed = newHeight - previewHeightPx
                        if (kotlin.math.abs(consumed) > 0.5f) {
                            performSliderHaptic(view)
                        }
                        previewHeightPx = newHeight
                        return androidx.compose.ui.geometry.Offset(0f, consumed)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }

                override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                    val delta = available.y
                    // Swiping Down (delta > 0): Expand
                    if (delta > 0) {
                        val newHeight = (previewHeightPx + delta).coerceIn(minPx, maxPx)
                        val consumedY = newHeight - previewHeightPx
                        if (kotlin.math.abs(consumedY) > 0.5f) {
                            performSliderHaptic(view)
                        }
                        previewHeightPx = newHeight
                        return androidx.compose.ui.geometry.Offset(0f, consumedY)
                    }
                    return androidx.compose.ui.geometry.Offset.Zero
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Preview Area (Variable Height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { previewHeightPx.toDp() })
                    .padding(16.dp)
                    .clip(if (initialUri == null) RoundedCornerShape(24.dp) else androidx.compose.ui.graphics.RectangleShape)
                    .background(if (initialUri == null) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { 
                        performUIHaptic(view)
                        if (initialUri == null) {
                            onPickImage()
                        } else {
                            viewModel.toggleContrast()
                        }
                    }
                    .padding(if (initialUri == null) 32.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (initialUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(
                             painter = androidx.compose.ui.res.painterResource(R.drawable.rounded_add_photo_alternate_24), 
                             contentDescription = null, 
                             modifier = Modifier.size(64.dp),
                             tint = MaterialTheme.colorScheme.primary
                         )
                         Spacer(Modifier.size(8.dp))
                         Text(
                             stringResource(R.string.watermark_pick_image),
                             style = MaterialTheme.typography.bodyLarge,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                } else {

                    
                    // Implementing the "Last Success Persist" logic here locally
                    val current = previewState
                    var lastSuccess by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<WatermarkUiState.Success?>(null) }
                    
                    if (current is WatermarkUiState.Success) {
                        lastSuccess = current
                    }
                    
                    val showBlur = current is WatermarkUiState.Processing
                    
                    val blurRadius by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (showBlur) 16.dp else 0.dp,
                        label = "blur"
                    )
                    
                    val alpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showBlur) 0.6f else 1f,
                        label = "alpha"
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        // Underlying Image
                        if (lastSuccess != null) {
                            Box(
                                modifier = Modifier
                                    .blur(blurRadius)
                                    .alpha(alpha)
                            ) {
                                WatermarkPreview(uiState = lastSuccess!!)
                            }
                        } else {
                            // First load?
                            if (current is WatermarkUiState.Processing) {
                                // First load, show nothing or placeholder maybe
                            } else {
                                WatermarkPreview(uiState = current)
                            }
                        }

                        // Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBlur,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut()
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
            }
            
            // Allow this part to take remaining space and scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Controls Area
                    RoundedCardContainer(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Style Picker
                        SegmentedPicker(
                            items = WatermarkStyle.entries,
                            selectedItem = options.style,
                            onItemSelected = { 
                                performUIHaptic(view)
                                viewModel.setStyle(it) 
                            },
                            labelProvider = { style ->
                                when (style) {
                                    WatermarkStyle.OVERLAY -> context.getString(R.string.watermark_style_overlay)
                                    WatermarkStyle.FRAME -> context.getString(R.string.watermark_style_frame)
                                }
                            },
                            iconProvider = { style ->
                                val iconRes = when (style) {
                                    WatermarkStyle.OVERLAY -> R.drawable.rounded_magnify_fullscreen_24
                                    WatermarkStyle.FRAME -> R.drawable.rounded_window_open_24
                                }
        
                                Icon(
                                     painter = androidx.compose.ui.res.painterResource(id = iconRes),
                                     contentDescription = null,
                                     modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Style-specific options
                        if (options.style == WatermarkStyle.FRAME) {
                             com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                                iconRes = R.drawable.rounded_top_panel_close_24,
                                title = stringResource(R.string.watermark_move_to_top),
                                isChecked = options.moveToTop,
                                onCheckedChange = { viewModel.setMoveToTop(it) }
                            )
                        } else {
                            com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                                iconRes = R.drawable.rounded_position_bottom_left_24,
                                title = stringResource(R.string.watermark_left_align),
                                isChecked = options.leftAlignOverlay,
                                onCheckedChange = { viewModel.setLeftAlign(it) }
                            )
                        }
        
                        // Show Brand Toggle
                        IconToggleItem(
                            iconRes = R.drawable.rounded_mobile_text_2_24,
                            title = stringResource(R.string.watermark_show_brand),
                            isChecked = options.showDeviceBrand,
                            onCheckedChange = { viewModel.setShowBrand(it) }
                        )

                        // Custom Text Entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                                )
                                .heightIn(min = 56.dp)
                                .clickable { 
                                    performUIHaptic(view)
                                    showCustomTextSheet = true 
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.size(2.dp))
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_edit_note_24),
                                contentDescription = stringResource(R.string.watermark_custom_text),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(2.dp))
                            
                            Text(
                                text = stringResource(R.string.watermark_custom_text),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )

                            if (options.showCustomText && options.customText.isNotEmpty()) {
                                Text(
                                    text = options.customText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }
                            
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_chevron_right_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
        
        // Show EXIF Settings (Custom Row with Chevron)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                                )
                                .heightIn(min = 56.dp) // Match standard item height
                                .clickable { 
                                    performUIHaptic(view)
                                    showExifSheet = true 
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.size(2.dp))
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_image_search_24),
                                contentDescription = stringResource(R.string.watermark_show_exif),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(2.dp))
                            
                            Text(
                                text = stringResource(R.string.watermark_show_exif),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.rounded_chevron_right_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Text Size Sliders
                        if (options.showDeviceBrand) {
                            var brandSize by androidx.compose.runtime.remember(options.brandTextSize) { androidx.compose.runtime.mutableFloatStateOf(options.brandTextSize.toFloat()) }
                            
                            ConfigSliderItem(
                                title = stringResource(R.string.watermark_text_size_brand),
                                value = brandSize,
                                onValueChange = { 
                                    brandSize = it 
                                    performSliderHaptic(view)
                                },
                                onValueChangeFinished = { viewModel.setBrandTextSize(brandSize.toInt()) },
                                valueRange = 0f..100f,
                                increment = 5f,
                                valueFormatter = { "${it.toInt()}%" }
                            )
                        }

                        if (options.showExif) {
                            var dataSize by androidx.compose.runtime.remember(options.dataTextSize) { androidx.compose.runtime.mutableFloatStateOf(options.dataTextSize.toFloat()) }
                            
                            ConfigSliderItem(
                                title = stringResource(R.string.watermark_text_size_data),
                                value = dataSize,
                                onValueChange = { 
                                    dataSize = it 
                                    performSliderHaptic(view)
                                },
                                onValueChangeFinished = { viewModel.setDataTextSize(dataSize.toInt()) },
                                valueRange = 0f..100f,
                                increment = 5f,
                                valueFormatter = { "${it.toInt()}%" }
                            )
                        }

                        // Spacing Slider
                        var paddingValue by androidx.compose.runtime.remember(options.padding) { androidx.compose.runtime.mutableFloatStateOf(options.padding.toFloat()) }
                        
                        ConfigSliderItem(
                            title = stringResource(R.string.watermark_spacing),
                            value = paddingValue,
                            onValueChange = { 
                                paddingValue = it 
                                performSliderHaptic(view)
                            },
                            onValueChangeFinished = { viewModel.setPadding(paddingValue.toInt()) },
                            valueRange = 0f..100f,
                            increment = 5f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                    }
                    
                    // Border Section
                    Text(
                        text = "Border",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    RoundedCardContainer(
                         modifier = Modifier.fillMaxWidth()
                    ) {
                        var strokeValue by androidx.compose.runtime.remember(options.borderStroke) { androidx.compose.runtime.mutableFloatStateOf(options.borderStroke.toFloat()) }
                        
                        ConfigSliderItem(
                            title = stringResource(R.string.watermark_border_width),
                            value = strokeValue,
                            onValueChange = { 
                                strokeValue = it 
                                performSliderHaptic(view)
                            },
                            onValueChangeFinished = { viewModel.setBorderStroke(strokeValue.toInt()) },
                            valueRange = 0f..100f,
                            increment = 5f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                        
                        var cornerValue by androidx.compose.runtime.remember(options.borderCorner) { androidx.compose.runtime.mutableFloatStateOf(options.borderCorner.toFloat()) }
                        
                        ConfigSliderItem(
                            title = stringResource(R.string.watermark_border_corners),
                            value = cornerValue,
                            onValueChange = { 
                                cornerValue = it 
                                performSliderHaptic(view)
                            },
                            onValueChangeFinished = { viewModel.setBorderCorner(cornerValue.toInt()) },
                            valueRange = 0f..100f,
                            increment = 5f,
                            valueFormatter = { "${it.toInt()}%" }
                        )
                    }
                    
                    // Bottom spacing for scrolling
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        
        if (showExifSheet) {
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showExifSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.watermark_exif_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    RoundedCardContainer {
                        // Master Toggle
                        com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                            iconRes = R.drawable.rounded_image_search_24,
                            title = stringResource(R.string.watermark_show_exif),
                            isChecked = options.showExif,
                            onCheckedChange = { viewModel.setShowExif(it) }
                        )
                    }
                    
                    if (options.showExif) {
                        RoundedCardContainer {
                            // Granular toggles
                            // Helper for granular
                             val updateExif = { focal: Boolean, aperture: Boolean, iso: Boolean, shutter: Boolean, date: Boolean ->
                                viewModel.setExifSettings(focal, aperture, iso, shutter, date)
                             }
                            
                            IconToggleItem(
                                iconRes = R.drawable.rounded_control_camera_24,
                                title = stringResource(R.string.watermark_exif_focal_length),
                                isChecked = options.showFocalLength,
                                onCheckedChange = { updateExif(it, options.showAperture, options.showIso, options.showShutterSpeed, options.showDate) }
                            )
                            
                            IconToggleItem(
                                iconRes = R.drawable.rounded_camera_24,
                                title = stringResource(R.string.watermark_exif_aperture),
                                isChecked = options.showAperture,
                                onCheckedChange = { updateExif(options.showFocalLength, it, options.showIso, options.showShutterSpeed, options.showDate) }
                            )
                             
                            IconToggleItem(
                                iconRes = R.drawable.rounded_grain_24,
                                title = stringResource(R.string.watermark_exif_iso),
                                isChecked = options.showIso,
                                onCheckedChange = { updateExif(options.showFocalLength, options.showAperture, it, options.showShutterSpeed, options.showDate) }
                            )
                            
                            IconToggleItem(
                                iconRes = R.drawable.rounded_shutter_speed_24,
                                title = stringResource(R.string.watermark_exif_shutter_speed),
                                isChecked = options.showShutterSpeed,
                                onCheckedChange = { updateExif(options.showFocalLength, options.showAperture, options.showIso, it, options.showDate) }
                            )
                            
                            IconToggleItem(
                                iconRes = R.drawable.rounded_date_range_24,
                                title = stringResource(R.string.watermark_exif_date),
                                isChecked = options.showDate,
                                onCheckedChange = { updateExif(options.showFocalLength, options.showAperture, options.showIso, options.showShutterSpeed, it) }
                            )
                        }
                    }
                }
            }
        }
        
        if (showCustomTextSheet) {
            val view = androidx.compose.ui.platform.LocalView.current
            
            // Local state for draft editing
            var isEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(options.showCustomText) }
            var draftText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(options.customText) }
            var draftSize by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(options.customTextSize.toFloat()) }
            
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showCustomTextSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.watermark_custom_text_settings),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    RoundedCardContainer {
                        // Master Toggle
                        com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                            iconRes = R.drawable.rounded_edit_note_24,
                            title = stringResource(R.string.watermark_custom_text),
                            isChecked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }
                    
                    if (isEnabled) {
                         RoundedCardContainer {
                             Column(modifier = Modifier.padding(16.dp)) {
                                 // Text Input
                                 androidx.compose.material3.OutlinedTextField(
                                     value = draftText,
                                     onValueChange = { draftText = it },
                                     label = { Text(stringResource(R.string.watermark_custom_text)) },
                                     placeholder = { Text(stringResource(R.string.watermark_custom_text_hint)) },
                                     modifier = Modifier.fillMaxWidth(),
                                     singleLine = true
                                 )
                                 
                                 Spacer(modifier = Modifier.height(16.dp))
                                 
                                 // Size Slider
                                 val density = androidx.compose.ui.platform.LocalDensity.current
                                 Text(
                                     text = stringResource(R.string.watermark_text_size_custom),
                                     style = MaterialTheme.typography.labelLarge,
                                     color = MaterialTheme.colorScheme.onSurface
                                 )
                                 androidx.compose.material3.Slider(
                                      value = draftSize,
                                      onValueChange = { 
                                          draftSize = it 
                                          performSliderHaptic(view)
                                      },
                                      valueRange = 0f..100f
                                 )
                                 Text(
                                     text = "${draftSize.toInt()}%",
                                     style = MaterialTheme.typography.bodySmall,
                                     modifier = Modifier.align(Alignment.End),
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }
                         }
                    }
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                performUIHaptic(view)
                                showCustomTextSheet = false 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        
                        Button(
                            onClick = {
                                performUIHaptic(view)
                                viewModel.setCustomTextSettings(isEnabled, draftText, draftSize.toInt())
                                showCustomTextSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_save_changes))
                        }
                    }
                }
            }
        }
    }
}
