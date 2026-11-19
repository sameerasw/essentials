package com.sameerasw.essentials.ui.core.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun CheckboxListItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val shape =
        RoundedCornerShape(
            topStart = if (index == 0) 24.dp else 4.dp,
            topEnd = if (index == 0) 24.dp else 4.dp,
            bottomStart = if (index == count - 1) 24.dp else 4.dp,
            bottomEnd = if (index == count - 1) 24.dp else 4.dp,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .alpha(if (enabled) 1f else 0.38f)
                .clickable(enabled = enabled) {
                    HapticUtil.performUIHaptic(view)
                    onCheckedChange(!checked)
                }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
