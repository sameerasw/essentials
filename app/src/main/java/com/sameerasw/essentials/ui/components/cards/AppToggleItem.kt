package com.sameerasw.essentials.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun AppToggleItem(
    icon: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
    showToggle: Boolean = true
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .clickable(enabled = !showToggle && enabled) {
                if (enabled) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onCheckedChange(!isChecked)
                } else if (onDisabledClick != null) {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDisabledClick()
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.size(2.dp))
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback placeholder if needed, or just space
            Spacer(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.size(2.dp))

        if (description != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        if (showToggle) {
            Box {
                Switch(
                    checked = if (enabled) isChecked else false,
                    onCheckedChange = { checked ->
                        if (enabled) {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onCheckedChange(checked)
                        }
                    },
                    enabled = enabled
                )

                if (!enabled && onDisabledClick != null) {
                    Box(modifier = Modifier.matchParentSize().clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDisabledClick()
                    })
                }
            }
        }
    }
}
