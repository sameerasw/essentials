package com.sameerasw.essentials.ui.core.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun IconGridPicker(
    icons: List<String>,
    selectedIconName: String?,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val resolved = remember(icons) {
        icons.mapNotNull { name ->
            context.resources.getIdentifier(name, "drawable", context.packageName)
                .takeIf { it != 0 }
                ?.let { name to it }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(52.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(resolved, key = { it.first }) { (name, res) ->
            val selected = name == selectedIconName
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(if (selected) 24.dp else 16.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onIconSelected(name)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(res),
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
