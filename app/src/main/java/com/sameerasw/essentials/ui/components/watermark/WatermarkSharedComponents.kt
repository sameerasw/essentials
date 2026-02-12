package com.sameerasw.essentials.ui.components.watermark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.ColorMode
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic

@Composable
fun ColorModeOption(
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
            val base = accentColor ?: android.graphics.Color.GRAY
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(base, hsl)
            if (mode == ColorMode.ACCENT_LIGHT) {
                hsl[2] = 0.8f
            } else {
                hsl[2] = 0.2f
            }
            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        }
    }

    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoCarouselPicker(
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
        val containerColor =
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
        val contentColor =
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

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
