package com.sameerasw.essentials.ui.components.sliders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ConfigSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    increment: Float = 0.1f,
    valueFormatter: (Float) -> String = { "%.0f".format(it) },
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    iconRes: Int = 0,
    description: String? = null,
    icon: Int? = null,
    subtitle: String? = null
) {
    val view = LocalView.current
    val finalIconRes = icon ?: iconRes
    val finalDescription = subtitle ?: description

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (finalIconRes != 0) {
                Icon(
                    painter = painterResource(id = finalIconRes),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$title: ${valueFormatter(value)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
                if (finalDescription != null) {
                    Text(
                        text = finalDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (BigDecimal.valueOf(value.toDouble())
                        .subtract(BigDecimal.valueOf(increment.toDouble()))
                        .setScale(2, RoundingMode.HALF_UP))
                        .toFloat()
                    onValueChange(newValue.coerceIn(valueRange))
                    onValueChangeFinished?.invoke()
                },
                modifier = Modifier.padding(end = 4.dp),
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_remove_24),
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value,
                onValueChange = {
                    if (it != value) {
                        HapticUtil.performSliderHaptic(view)
                    }
                    onValueChange(it)
                },
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )

            IconButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val newValue = (BigDecimal.valueOf(value.toDouble())
                        .add(BigDecimal.valueOf(increment.toDouble()))
                        .setScale(2, RoundingMode.HALF_UP))
                        .toFloat()
                    onValueChange(newValue.coerceIn(valueRange))
                    onValueChangeFinished?.invoke()
                },
                modifier = Modifier.padding(start = 4.dp),
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_add_24),
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
