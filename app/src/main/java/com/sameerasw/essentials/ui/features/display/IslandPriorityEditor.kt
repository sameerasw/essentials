package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.utils.HapticUtil
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val PRIORITY_ANIM_MS = 140

@Composable
fun IslandPriorityActions(
    editing: Boolean,
    onStart: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    val view = LocalView.current
    val pill = 25.dp
    val inner = 6.dp
    val endCorner by animateDpAsState(if (editing) inner else pill, tween(PRIORITY_ANIM_MS), label = "priorityEndCorner")
    val container by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceBright,
        tween(PRIORITY_ANIM_MS),
        label = "priorityContainer",
    )
    val content by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        tween(PRIORITY_ANIM_MS),
        label = "priorityContent",
    )
    val padding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                if (editing) onSave() else onStart()
            },
            shape = RoundedCornerShape(topStart = pill, topEnd = endCorner, bottomEnd = endCorner, bottomStart = pill),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            contentPadding = padding,
        ) {
            Icon(
                painter = painterResource(if (editing) R.drawable.rounded_save_24 else R.drawable.rounded_swap_vert_24),
                contentDescription = stringResource(if (editing) R.string.action_save else R.string.island_reorder_title),
                modifier = Modifier.size(22.dp),
            )
            AnimatedVisibility(
                visible = !editing,
                enter = fadeIn(tween(PRIORITY_ANIM_MS)) + expandHorizontally(tween(PRIORITY_ANIM_MS)),
                exit = fadeOut(tween(PRIORITY_ANIM_MS)) + shrinkHorizontally(tween(PRIORITY_ANIM_MS)),
            ) {
                Row {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.island_reorder_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = editing,
            enter = fadeIn(tween(PRIORITY_ANIM_MS)) + expandHorizontally(tween(PRIORITY_ANIM_MS)),
            exit = fadeOut(tween(PRIORITY_ANIM_MS)) + shrinkHorizontally(tween(PRIORITY_ANIM_MS)),
        ) {
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onReset()
                },
                shape = RoundedCornerShape(topStart = inner, topEnd = pill, bottomEnd = pill, bottomStart = inner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = padding,
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_refresh_24),
                    contentDescription = stringResource(R.string.action_reset),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
