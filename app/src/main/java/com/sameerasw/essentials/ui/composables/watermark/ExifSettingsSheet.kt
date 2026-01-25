package com.sameerasw.essentials.ui.composables.watermark

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.WatermarkOptions
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifSettingsSheet(
    options: WatermarkOptions,
    onDismissRequest: () -> Unit,
    onShowExifChange: (Boolean) -> Unit,
    onExifSettingsChange: (focal: Boolean, aperture: Boolean, iso: Boolean, shutter: Boolean, date: Boolean) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                IconToggleItem(
                    iconRes = R.drawable.rounded_image_search_24,
                    title = stringResource(R.string.watermark_show_exif),
                    isChecked = options.showExif,
                    onCheckedChange = onShowExifChange
                )
            }
            
            if (options.showExif) {
                RoundedCardContainer {
                    val updateExif = { focal: Boolean, aperture: Boolean, iso: Boolean, shutter: Boolean, date: Boolean ->
                        onExifSettingsChange(focal, aperture, iso, shutter, date)
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
