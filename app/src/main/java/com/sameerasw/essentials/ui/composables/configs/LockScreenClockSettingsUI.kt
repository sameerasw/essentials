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
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
            ClockOption("ANALOG_CLOCK_BIGNUM", R.string.lock_screen_clock_bignum, R.drawable.clock_bignum),
            ClockOption("DIGITAL_CLOCK_CALLIGRAPHY", R.string.lock_screen_clock_calligraphy, R.drawable.clock_calligraphy),
            ClockOption("DIGITAL_CLOCK_FLEX", R.string.lock_screen_clock_flex, R.drawable.clock_flex),
            ClockOption("DIGITAL_CLOCK_GROWTH", R.string.lock_screen_clock_growth, R.drawable.clock_growth),
            ClockOption("DIGITAL_CLOCK_HANDWRITTEN", R.string.lock_screen_clock_handwritten, R.drawable.clock_handwritten),
            ClockOption("DIGITAL_CLOCK_INFLATE", R.string.lock_screen_clock_inflate, R.drawable.clock_inflate),
            ClockOption("DIGITAL_CLOCK_METRO", R.string.lock_screen_clock_metro, R.drawable.clock_metro),
            ClockOption("DIGITAL_CLOCK_NUMBEROVERLAP", R.string.lock_screen_clock_numoverlap, R.drawable.clock_overlap),
            ClockOption("DIGITAL_CLOCK_WEATHER", R.string.lock_screen_clock_weather, R.drawable.clock_weather)
        )
    }

    val carouselState = rememberCarouselState { clockOptions.size }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(200.dp)
            ) { index ->
                val option = clockOptions[index]
                val isSelected = currentClockId == option.id

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .maskClip(MaterialTheme.shapes.large)
                    .background(if (isDark) Color.White else MaterialTheme.colorScheme.surfaceBright)
                    .pointerInput(option) {
                        detectTapGestures {
                            HapticUtil.performUIHaptic(view)
                            viewModel.setLockScreenClockId(option.id, context)
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

        RoundedCardContainer(modifier = Modifier.padding(horizontal = 8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_desc_lock_screen_clock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

data class ClockOption(
    val id: String,
    val nameRes: Int,
    val imageRes: Int
)
