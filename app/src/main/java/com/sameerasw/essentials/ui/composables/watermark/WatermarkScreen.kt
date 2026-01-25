package com.sameerasw.essentials.ui.composables.watermark

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.ColorMode
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
import com.sameerasw.essentials.ui.components.ReusableTopAppBar
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil.performSliderHaptic
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic
import com.sameerasw.essentials.viewmodels.WatermarkUiState
import com.sameerasw.essentials.viewmodels.WatermarkViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun WatermarkScreen(
    initialUri: Uri?,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
    viewModel: WatermarkViewModel
) {
    val context = LocalContext.current
    val view = LocalView.current // For haptics
    var showExifSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ReusableTopAppBar(
                title = R.string.feat_watermark_title,
                hasBack = true,
                onBackClick = {
                    performUIHaptic(view)
                    onBack()
                },
                isSmall = true,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                actions = {
                    val pickImageButton = @Composable {
                         // Pick Image Button
                        if (initialUri == null) {
                            // Primary when no image
                            Button(onClick = {
                                performUIHaptic(view)
                                onPickImage()
                            }) {
                                 Icon(
                                     painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
                                     contentDescription = stringResource(R.string.watermark_pick_image),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.watermark_pick_image))
                            }
                        } else {
                            // Secondary when image is there
                            OutlinedButton(onClick = {
                                performUIHaptic(view)
                                onPickImage()
                            }) {
                                 Icon(
                                     painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
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
                        var showMenu by remember { mutableStateOf(false) }
                        
                        Box {
                             // Save Button (Primary)
                             Button(onClick = {
                                 performUIHaptic(view)
                                 showMenu = true 
                             }) {
                                 Icon(
                                     painter = painterResource(R.drawable.rounded_save_24),
                                     contentDescription = stringResource(R.string.action_save),
                                     modifier = Modifier.size(18.dp)
                                 )
                                 Spacer(Modifier.size(8.dp))
                                 Text(stringResource(R.string.action_save))
                             }
                             
                             SegmentedDropdownMenu(
                                 expanded = showMenu,
                                 onDismissRequest = { showMenu = false },
                             ) {
                                 // Share Option
                                 SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_share)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = painterResource(R.drawable.rounded_share_24),
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
                                 SegmentedDropdownMenuItem(
                                     text = { Text(stringResource(R.string.action_save)) },
                                     leadingIcon = {
                                         Icon(
                                             painter = painterResource(R.drawable.rounded_save_24),
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
        floatingActionButton = {
            if (initialUri != null) {
                FloatingActionButton(
                    onClick = { 
                        performUIHaptic(view)
                        showEditSheet = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_edit_24),
                        contentDescription = stringResource(R.string.action_edit)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenHeightDp = configuration.screenHeightDp.dp
        
        val maxPreviewHeightDp = screenHeightDp * 0.6f
        val minPreviewHeightDp = screenHeightDp * 0.3f
        
        val maxPx = with(density) { maxPreviewHeightDp.toPx() }
        val minPx = with(density) { minPreviewHeightDp.toPx() }
        
        val previewHeightPxState = remember { mutableFloatStateOf(maxPx) }
        var previewHeightPx by previewHeightPxState
        
        val nestedScrollConnection = remember {
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
                    .clip(if (initialUri == null) RoundedCornerShape(24.dp) else RectangleShape)
                    .background(if (initialUri == null) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                    .clickable { 
                        performUIHaptic(view)
                        if (initialUri == null) {
                            onPickImage()
                        }
                    }
                    .padding(if (initialUri == null) 32.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (initialUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(
                             painter = painterResource(R.drawable.rounded_add_photo_alternate_24),
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
                                     painter = painterResource(id = iconRes),
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
                                painter = painterResource(id = R.drawable.rounded_image_search_24),
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
                                painter = painterResource(id = R.drawable.rounded_chevron_right_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }



                        // Spacing Slider
                        var paddingValue by remember(options.padding) { mutableFloatStateOf(options.padding.toFloat()) }
                        
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
                    

                    // Font Size Section
                    val showFontSection = options.showDeviceBrand || options.showExif || options.showCustomText
                    
                    if (showFontSection) {
                        Text(
                            text = stringResource(R.string.watermark_font_options),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        RoundedCardContainer(
                             modifier = Modifier.fillMaxWidth()
                        ) {
                            if (options.showDeviceBrand) {
                                var brandSize by remember(options.brandTextSize) { mutableFloatStateOf(options.brandTextSize.toFloat()) }
                                
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
                                var dataSize by remember(options.dataTextSize) { mutableFloatStateOf(options.dataTextSize.toFloat()) }
                                
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
                            
                            if (options.showCustomText) {
                                var customSize by remember(options.customTextSize) { mutableFloatStateOf(options.customTextSize.toFloat()) }
                                
                                ConfigSliderItem(
                                    title = stringResource(R.string.watermark_text_size_custom),
                                    value = customSize,
                                    onValueChange = { 
                                        customSize = it 
                                        performSliderHaptic(view)
                                    },
                                    onValueChangeFinished = { viewModel.setCustomTextSize(customSize.toInt()) },
                                    valueRange = 0f..100f,
                                    increment = 5f,
                                    valueFormatter = { "${it.toInt()}%" }
                                )
                            }
                        }
                    }
                    
                    
                    // Logo Section
                    Text(
                        text = stringResource(R.string.watermark_logo_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val logoResId by viewModel.logoResId.collectAsState()
                    val showLogo by viewModel.showLogo.collectAsState()

                    RoundedCardContainer(
                         modifier = Modifier.fillMaxWidth()
                    ) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_image_24,
                            title = stringResource(R.string.watermark_logo_show),
                            isChecked = showLogo,
                            onCheckedChange = { checked -> viewModel.setShowLogo(checked) }
                        )
                        
                        if (showLogo) {
                            LogoCarouselPicker(
                                selectedResId = logoResId,
                                onLogoSelected = { resId -> viewModel.setLogoResId(resId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                            
                            var logoSizeValue by remember(options.logoSize) { mutableFloatStateOf(options.logoSize.toFloat()) }
                            ConfigSliderItem(
                                title = stringResource(R.string.watermark_logo_size),
                                value = logoSizeValue,
                                onValueChange = { 
                                    logoSizeValue = it 
                                    performSliderHaptic(view)
                                },
                                onValueChangeFinished = { viewModel.setLogoSize(logoSizeValue.toInt()) },
                                valueRange = 1f..100f,
                                increment = 1f,
                                valueFormatter = { "${it.toInt()}%" }
                            )
                        }
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
                        var strokeValue by remember(options.borderStroke) { mutableFloatStateOf(options.borderStroke.toFloat()) }
                        
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
                        
                        var cornerValue by remember(options.borderCorner) { mutableFloatStateOf(options.borderCorner.toFloat()) }
                        
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
                    
                    // Color Section
                    Text(
                        text = stringResource(R.string.watermark_color_section),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    RoundedCardContainer(
                         modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ColorModeOption(
                                mode = ColorMode.LIGHT,
                                isSelected = options.colorMode == ColorMode.LIGHT,
                                onClick = { viewModel.setColorMode(ColorMode.LIGHT) }
                            )
                            ColorModeOption(
                                mode = ColorMode.DARK,
                                isSelected = options.colorMode == ColorMode.DARK,
                                onClick = { viewModel.setColorMode(ColorMode.DARK) }
                            )
                            ColorModeOption(
                                mode = ColorMode.ACCENT_LIGHT,
                                accentColor = options.accentColor,
                                isSelected = options.colorMode == ColorMode.ACCENT_LIGHT,
                                onClick = { viewModel.setColorMode(ColorMode.ACCENT_LIGHT) }
                            )
                            ColorModeOption(
                                mode = ColorMode.ACCENT_DARK,
                                accentColor = options.accentColor,
                                isSelected = options.colorMode == ColorMode.ACCENT_DARK,
                                onClick = { viewModel.setColorMode(ColorMode.ACCENT_DARK) }
                            )
                        }
                    }

                    // Bottom spacing for scrolling
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
        
        if (showExifSheet) {
            val view = LocalView.current
            ModalBottomSheet(
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
        
        if (showEditSheet) {
            val view = LocalView.current
            val currentBrand by viewModel.currentBrandText.collectAsState()
            val currentCustom by viewModel.currentCustomText.collectAsState()
            val currentDate by viewModel.currentDateText.collectAsState()
            
            var draftBrand by remember { mutableStateOf(currentBrand ?: "") }
            var draftCustom by remember { mutableStateOf(currentCustom) }
            var draftDate by remember { mutableStateOf(currentDate) }
            var showBrandToggle by remember { mutableStateOf(options.showDeviceBrand) }
            
            var showDatePicker by remember { mutableStateOf(false) }
            var showTimePicker by remember { mutableStateOf(false) }
            
            val displayDate = draftDate?.let { viewModel.formatDate(it) } ?: stringResource(R.string.watermark_no_date)

            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.watermark_edit_texts),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    RoundedCardContainer {
                        Column(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(4.dp)
                            )
                        ) {
                            IconToggleItem(
                                iconRes = R.drawable.rounded_mobile_text_2_24,
                                title = stringResource(R.string.watermark_show_brand),
                                isChecked = showBrandToggle,
                                onCheckedChange = {
                                    performUIHaptic(view)
                                    showBrandToggle = it
                                }
                            )

                            AnimatedVisibility(visible = showBrandToggle) {
                                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                    OutlinedTextField(
                                        value = draftBrand,
                                        onValueChange = { draftBrand = it },
                                        label = { Text(stringResource(R.string.watermark_device_brand)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(vertical = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = draftCustom,
                                onValueChange = { draftCustom = it },
                                label = { Text(stringResource(R.string.watermark_custom_text)) },
                                placeholder = { Text(stringResource(R.string.watermark_custom_text_hint)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceBright,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    performUIHaptic(view)
                                    showDatePicker = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Spacer(modifier = Modifier.size(2.dp))
                            Icon(
                                painter = painterResource(R.drawable.rounded_date_range_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(2.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.watermark_date_time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = displayDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.rounded_chevron_right_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                showEditSheet = false 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        
                        Button(
                            onClick = {
                                performUIHaptic(view)
                                viewModel.setShowBrand(showBrandToggle)
                                viewModel.updateOverriddenTexts(draftBrand, draftCustom, draftDate)
                                showEditSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_save_changes))
                        }
                    }
                }
            }
            
            if (showDatePicker) {
                val initialDateMillis = remember(draftDate) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy:MM:dd", java.util.Locale.US)
                        sdf.parse(draftDate?.split(" ")?.getOrNull(0) ?: "")?.time
                    } catch (e: Exception) {
                        null
                    }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = millis }
                                val sdf = java.text.SimpleDateFormat("yyyy:MM:dd", java.util.Locale.US)
                                val datePart = sdf.format(calendar.time)
                                val timePart = draftDate?.split(" ")?.getOrNull(1) ?: "00:00:00"
                                draftDate = "$datePart $timePart"
                            }
                            showDatePicker = false
                            showTimePicker = true
                        }) { Text(stringResource(R.string.action_next)) }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            if (showTimePicker) {
                val (initialHour, initialMinute) = remember(draftDate) {
                    try {
                        val timePart = draftDate?.split(" ")?.getOrNull(1) ?: "00:00:00"
                        val parts = timePart.split(":")
                        parts.getOrNull(0)?.toInt()!! to parts.getOrNull(1)?.toInt()!!
                    } catch (e: Exception) {
                        0 to 0
                    }
                }
                val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val timePart = String.format("%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                            val datePart = draftDate?.split(" ")?.getOrNull(0) ?: "2024:01:01"
                            draftDate = "$datePart $timePart"
                            showTimePicker = false
                        }) { Text(stringResource(R.string.action_ok)) }
                    },
                    text = { TimePicker(state = timePickerState) }
                )
            }
        }
    }
}

@Composable
private fun ColorModeOption(
    mode: ColorMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Int? = null
) {
    val view = LocalView.current
    val color = when (mode) {
        ColorMode.LIGHT -> Color.White
        ColorMode.DARK -> Color.Black
        ColorMode.ACCENT_LIGHT, ColorMode.ACCENT_DARK -> {
            // Derive a preview color for the circle
            val base = accentColor ?: android.graphics.Color.GRAY
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(base, hsl)
            if (mode == ColorMode.ACCENT_LIGHT) {
                hsl[2] = 0.8f // Light shade
            } else {
                hsl[2] = 0.2f // Dark shade
            }
            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        }
    }
    
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .clickable { 
                performUIHaptic(view)
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        if (mode == ColorMode.ACCENT_LIGHT || mode == ColorMode.ACCENT_DARK) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_image_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (mode == ColorMode.ACCENT_LIGHT) Color.Black else Color.White
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LogoCarouselPicker(
    selectedResId: Int?,
    onLogoSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val logos = listOf(
        R.drawable.apple,
        R.drawable.cmf,
        R.drawable.google,
        R.drawable.moto,
        R.drawable.nothing,
        R.drawable.oppo,
        R.drawable.samsung,
        R.drawable.sony,
        R.drawable.vivo,
        R.drawable.xiaomi
    )
    
    val carouselState = rememberCarouselState { logos.size }
    val view = LocalView.current
    
    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = 80.dp,
        minSmallItemWidth = 5.dp,
        maxSmallItemWidth = 200.dp,
        itemSpacing = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .height(84.dp),
        contentPadding = PaddingValues(4.dp)
    ) { index ->
        val resId = logos[index]
        val isSelected = selectedResId == resId
        val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)
                .clickable {
                    performUIHaptic(view)
                    onLogoSelected(resId)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = contentColor
            )
        }
    }
}
