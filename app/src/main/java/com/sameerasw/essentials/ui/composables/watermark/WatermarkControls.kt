package com.sameerasw.essentials.ui.composables.watermark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.ColorMode
import com.sameerasw.essentials.domain.watermark.WatermarkOptions
import com.sameerasw.essentials.domain.watermark.WatermarkStyle
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil.performSliderHaptic
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic

@Composable
fun WatermarkControls(
    options: WatermarkOptions,
    showLogo: Boolean,
    logoResId: Int?,
    onStyleChange: (WatermarkStyle) -> Unit,
    onMoveToTopChange: (Boolean) -> Unit,
    onLeftAlignChange: (Boolean) -> Unit,
    onPaddingChange: (Int) -> Unit,
    onBrandTextSizeChange: (Int) -> Unit,
    onDataTextSizeChange: (Int) -> Unit,
    onCustomTextSizeChange: (Int) -> Unit,
    onShowLogoChange: (Boolean) -> Unit,
    onLogoResIdChange: (Int) -> Unit,
    onLogoSizeChange: (Int) -> Unit,
    onBorderStrokeChange: (Int) -> Unit,
    onBorderCornerChange: (Int) -> Unit,
    onColorModeChange: (ColorMode) -> Unit,
    onShowExifClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                    onStyleChange(it) 
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
                 IconToggleItem(
                    iconRes = R.drawable.rounded_top_panel_close_24,
                    title = stringResource(R.string.watermark_move_to_top),
                    isChecked = options.moveToTop,
                    onCheckedChange = onMoveToTopChange
                )
            } else {
                IconToggleItem(
                    iconRes = R.drawable.rounded_position_bottom_left_24,
                    title = stringResource(R.string.watermark_left_align),
                    isChecked = options.leftAlignOverlay,
                    onCheckedChange = onLeftAlignChange
                )
            }

            // Show EXIF Settings Row
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
                        onShowExifClick() 
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
                onValueChangeFinished = { onPaddingChange(paddingValue.toInt()) },
                valueRange = 0f..100f,
                increment = 5f,
                valueFormatter = { "${it.toInt()}%" }
            )
        }

        // Font Size Section
        if (options.showDeviceBrand || options.showExif || options.showCustomText) {
            Text(
                text = stringResource(R.string.watermark_font_options),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                if (options.showDeviceBrand) {
                    var brandSize by remember(options.brandTextSize) { mutableFloatStateOf(options.brandTextSize.toFloat()) }
                    ConfigSliderItem(
                        title = stringResource(R.string.watermark_text_size_brand),
                        value = brandSize,
                        onValueChange = { 
                            brandSize = it 
                            performSliderHaptic(view)
                        },
                        onValueChangeFinished = { onBrandTextSizeChange(brandSize.toInt()) },
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
                        onValueChangeFinished = { onDataTextSizeChange(dataSize.toInt()) },
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
                        onValueChangeFinished = { onCustomTextSizeChange(customSize.toInt()) },
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
        
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            IconToggleItem(
                iconRes = R.drawable.rounded_image_24,
                title = stringResource(R.string.watermark_logo_show),
                isChecked = showLogo,
                onCheckedChange = onShowLogoChange
            )
            
            if (showLogo) {
                LogoCarouselPicker(
                    selectedResId = logoResId,
                    onLogoSelected = onLogoResIdChange,
                    modifier = Modifier.fillMaxWidth()
                )
                
                var logoSizeValue by remember(options.logoSize) { mutableFloatStateOf(options.logoSize.toFloat()) }
                ConfigSliderItem(
                    title = stringResource(R.string.watermark_logo_size),
                    value = logoSizeValue,
                    onValueChange = { 
                        logoSizeValue = it 
                        performSliderHaptic(view)
                    },
                    onValueChangeFinished = { onLogoSizeChange(logoSizeValue.toInt()) },
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
        
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            var strokeValue by remember(options.borderStroke) { mutableFloatStateOf(options.borderStroke.toFloat()) }
            ConfigSliderItem(
                title = stringResource(R.string.watermark_border_width),
                value = strokeValue,
                onValueChange = { 
                    strokeValue = it 
                    performSliderHaptic(view)
                },
                onValueChangeFinished = { onBorderStrokeChange(strokeValue.toInt()) },
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
                onValueChangeFinished = { onBorderCornerChange(cornerValue.toInt()) },
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
        
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
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
                    onClick = { onColorModeChange(ColorMode.LIGHT) }
                )
                ColorModeOption(
                    mode = ColorMode.DARK,
                    isSelected = options.colorMode == ColorMode.DARK,
                    onClick = { onColorModeChange(ColorMode.DARK) }
                )
                ColorModeOption(
                    mode = ColorMode.ACCENT_LIGHT,
                    accentColor = options.accentColor,
                    isSelected = options.colorMode == ColorMode.ACCENT_LIGHT,
                    onClick = { onColorModeChange(ColorMode.ACCENT_LIGHT) }
                )
                ColorModeOption(
                    mode = ColorMode.ACCENT_DARK,
                    accentColor = options.accentColor,
                    isSelected = options.colorMode == ColorMode.ACCENT_DARK,
                    onClick = { onColorModeChange(ColorMode.ACCENT_DARK) }
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
