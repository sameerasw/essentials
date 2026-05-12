package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LockScreenClockSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentClockId by viewModel.lockScreenClockId
    val isDark = isSystemInDarkTheme()

    val inversionMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    val clockOptions = remember {
        listOf(
            ClockOption("DEFAULT", R.string.lock_screen_clock_default, R.drawable.clock_flex),
            ClockOption("ANALOG_CLOCK_BIGNUM", R.string.lock_screen_clock_bignum, R.drawable.clock_bignum),
            ClockOption("DIGITAL_CLOCK_CALLIGRAPHY", R.string.lock_screen_clock_calligraphy, R.drawable.clock_calligraphy),
            ClockOption("DIGITAL_CLOCK_GROWTH", R.string.lock_screen_clock_growth, R.drawable.clock_growth),
            ClockOption("DIGITAL_CLOCK_HANDWRITTEN", R.string.lock_screen_clock_handwritten, R.drawable.clock_handwritten),
            ClockOption("DIGITAL_CLOCK_INFLATE", R.string.lock_screen_clock_inflate, R.drawable.clock_inflate),
            ClockOption("DIGITAL_CLOCK_METRO", R.string.lock_screen_clock_metro, R.drawable.clock_metro),
            ClockOption("DIGITAL_CLOCK_NUMBEROVERLAP", R.string.lock_screen_clock_numoverlap, R.drawable.clock_overlap),
            ClockOption("DIGITAL_CLOCK_WEATHER", R.string.lock_screen_clock_weather, R.drawable.clock_weather)
        )
    }

    val colorOptions = remember {
        listOf(
            ClockColorOption("DEFAULT", Color.Transparent, 0, R.string.color_default),
            ClockColorOption("RED", Color(0xFFE57373), -23641, R.string.color_red),
            ClockColorOption("GREEN", Color(0xFF81C784), -14057967, R.string.color_green),
            ClockColorOption("BLUE", Color(0xFF64B5F6), -14575885, R.string.color_blue),
            ClockColorOption("YELLOW", Color(0xFFFFF176), -5317, R.string.color_yellow),
            ClockColorOption("ORANGE", Color(0xFFFFB74D), -18611, R.string.color_orange),
            ClockColorOption("PURPLE", Color(0xFFBA68C8), -4560702, R.string.color_purple),
            ClockColorOption("PINK", Color(0xFFF06292), -1023342, R.string.color_pink),
            ClockColorOption("TEAL", Color(0xFF4DB6AC), -11684180, R.string.color_teal)
        )
    }

    val isDefaultStyleSelected = currentClockId == "DEFAULT" || currentClockId == "DIGITAL_CLOCK_FLEX"

    val carouselState = rememberCarouselState { clockOptions.size }

    LaunchedEffect(carouselState) {
        var isFirst = true
        snapshotFlow { carouselState.currentItem }
            .collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    HapticUtil.performSliderHaptic(view)
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.lock_screen_clock_select_label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 180.dp,
            itemSpacing = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) { index ->
            val option = clockOptions[index]
            val isSelected = if (option.id == "DEFAULT") isDefaultStyleSelected else currentClockId == option.id

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .maskClip(MaterialTheme.shapes.large)
                    .background(if (isDark) Color.White else MaterialTheme.colorScheme.surfaceBright)
                    .pointerInput(option) {
                        detectTapGestures {
                            HapticUtil.performUIHaptic(view)
                            if (option.id == "DEFAULT") {
                                if (!isDefaultStyleSelected) {
                                    viewModel.setLockScreenClockId("DEFAULT", context)
                                }
                            } else {
                                viewModel.setLockScreenClockId(option.id, context)
                            }
                        }
                    }
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = option.imageRes),
                    contentDescription = stringResource(option.nameRes),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    colorFilter = if (isDark) ColorFilter.colorMatrix(inversionMatrix) else null
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Color & Tone Section
        Text(
            text = stringResource(R.string.label_color),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            Column(
                modifier = Modifier.fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            ).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Multi-row Color Picker
                val rows = colorOptions.chunked(5)
                rows.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowOptions.forEach { colorOption ->
                            ColorCircle(
                                colorOption = colorOption,
                                isSelected = viewModel.lockScreenClockSelectedColorId.value == colorOption.id,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    viewModel.setLockScreenClockColor(colorOption.id, colorOption.seedColor, context)
                                }
                            )
                        }
                    }
                }
            }

            ConfigSliderItem(
                title = stringResource(R.string.label_color_tone),
                value = viewModel.lockScreenClockColorTone.intValue.toFloat(),
                onValueChange = { viewModel.setLockScreenClockColorTone(it.toInt(), context) },
                valueRange = 0f..100f,
                valueFormatter = { "${it.toInt()}%" },
                iconRes = R.drawable.rounded_palette_24,
                enabled = viewModel.lockScreenClockSelectedColorId.value != "DEFAULT"
            )
        }

        if (isDefaultStyleSelected) {
            Text(
                text = stringResource(R.string.label_style_font),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer {
                SegmentedPicker(
                    items = listOf("DEFAULT", "DIGITAL_CLOCK_FLEX"),
                    selectedItem = currentClockId ?: "DEFAULT",
                    onItemSelected = { viewModel.setLockScreenClockId(it, context) },
                    labelProvider = { if (it == "DEFAULT") "Default" else "Flex" }
                )

                ConfigSliderItem(
                    title = stringResource(R.string.label_weight),
                    value = viewModel.lockScreenClockWeight.intValue.toFloat(),
                    onValueChange = { viewModel.setLockScreenClockWeight(it.toInt(), context) },
                    valueRange = 100f..1000f,
                    increment = 10f,
                    valueFormatter = { it.toInt().toString() },
                    iconRes = R.drawable.rounded_line_weight_24
                )

                ConfigSliderItem(
                    title = stringResource(R.string.label_width),
                    value = viewModel.lockScreenClockWidth.intValue.toFloat(),
                    onValueChange = { viewModel.setLockScreenClockWidth(it.toInt(), context) },
                    valueRange = 25f..200f,
                    increment = 5f,
                    valueFormatter = { it.toInt().toString() },
                    iconRes = R.drawable.rounded_arrows_outward_24
                )

                ConfigSliderItem(
                    title = stringResource(R.string.label_roundness),
                    value = viewModel.lockScreenClockRoundness.intValue.toFloat(),
                    onValueChange = { viewModel.setLockScreenClockRoundness(it.toInt(), context) },
                    valueRange = 0f..100f,
                    increment = 5f,
                    valueFormatter = { it.toInt().toString() },
                    iconRes = R.drawable.rounded_rounded_corner_24
                )
            }
        }

        // About Section
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_info_24,
                title = stringResource(R.string.about_title),
                description = stringResource(R.string.about_desc_lock_screen_clock),
                isChecked = false,
                onCheckedChange = {},
                showToggle = false
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ColorCircle(
    colorOption: ClockColorOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (colorOption.id == "DEFAULT") MaterialTheme.colorScheme.surfaceVariant else colorOption.color
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .pointerInput(colorOption.id) {
                detectTapGestures {
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (colorOption.id == "DEFAULT") {
            Icon(
                painter = painterResource(id = R.drawable.rounded_palette_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (colorOption.id == "DEFAULT") MaterialTheme.colorScheme.primary else Color.White)
            )
        }
    }
}

data class ClockOption(
    val id: String,
    val nameRes: Int,
    val imageRes: Int
)

data class ClockColorOption(
    val id: String,
    val color: Color,
    val seedColor: Int,
    val nameRes: Int
)

