package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.modifiers.highlight
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.min

@Composable
fun SoundModeTileSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE) }
    val defaultOrder = listOf("Sound", "Vibrate", "Silent")
    val orderString = prefs.getString("sound_mode_order", defaultOrder.joinToString(",")) ?: defaultOrder.joinToString(",")
    var activeModes by remember { mutableStateOf(orderString.split(",")) }
    val disabledString = prefs.getString("sound_mode_disabled", "") ?: ""
    var disabledModes by remember { mutableStateOf(if (disabledString.isEmpty()) emptyList() else disabledString.split(",")) }
    val modeIcons = mapOf(
        "Sound" to R.drawable.rounded_volume_up_24,
        "Vibrate" to R.drawable.rounded_mobile_vibrate_24,
        "Silent" to R.drawable.rounded_volume_off_24
    )
    
    val soundModeNames = mapOf(
        "Sound" to R.string.sound_mode_sound,
        "Vibrate" to R.string.sound_mode_vibrate,
        "Silent" to R.string.sound_mode_silent
    )

    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val originalActiveSize = activeModes.size
        val fromMode: String = when {
            from.index < originalActiveSize -> activeModes.getOrNull(from.index) ?: return@rememberReorderableLazyListState
            from.index == originalActiveSize -> "separator"
            else -> disabledModes.getOrNull(from.index - originalActiveSize - 1) ?: return@rememberReorderableLazyListState
        }
        if (fromMode == "separator") return@rememberReorderableLazyListState
        // remove
        if (from.index < originalActiveSize) {
            activeModes = activeModes.toMutableList().apply { removeAt(from.index) }
        } else {
            disabledModes = disabledModes.toMutableList().apply { removeAt(from.index - originalActiveSize - 1) }
        }
        // add
        val newActiveSize = activeModes.size
        val newDisabledSize = disabledModes.size
        if (to.index < newActiveSize) {
            activeModes = activeModes.toMutableList().apply { add(to.index, fromMode) }
        } else if (to.index == newActiveSize) {
            activeModes = activeModes.toMutableList().apply { add(newActiveSize, fromMode) }
        } else {
            val pos = min(to.index - newActiveSize - 1, newDisabledSize)
            disabledModes = disabledModes.toMutableList().apply { add(pos, fromMode) }
        }
        // ensure at least 2 active only if moved from active
        if (from.index < originalActiveSize && activeModes.size < 2 && disabledModes.isNotEmpty()) {
            val mode = disabledModes[0]
            activeModes = activeModes + mode
            disabledModes = disabledModes.drop(1)
        }
        // save
        prefs.edit {
            putString("sound_mode_order", activeModes.joinToString(","))
            putString("sound_mode_disabled", disabledModes.joinToString(","))
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    // Notification Category
    Text(
        text = stringResource(R.string.sound_mode_reorder_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LazyColumn(
        modifier = modifier.fillMaxSize()
            .padding(horizontal = 16.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(activeModes.size, key = { activeModes[it] }) { index ->
            val mode = activeModes[index]
            ReorderableItem(reorderableLazyListState, key = mode) { _ ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                if (activeModes.size > 2) {
                                    // disable
                                    val modeToDisable = activeModes[index]
                                    activeModes = activeModes.toMutableList().apply { removeAt(index) }
                                    disabledModes = disabledModes + modeToDisable
                                } else if (disabledModes.isNotEmpty()) {
                                    // activate another
                                    val modeToActivate = disabledModes[0]
                                    disabledModes = disabledModes.drop(1)
                                    activeModes = activeModes + modeToActivate
                                }
                                // save
                                prefs.edit {
                                    putString("sound_mode_order", activeModes.joinToString(","))
                                    putString("sound_mode_disabled", disabledModes.joinToString(","))
                                }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = modeIcons[mode] ?: R.drawable.rounded_volume_up_24),
                            contentDescription = mode,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(soundModeNames[mode] ?: R.string.tile_sound_mode), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                }
                            ),
                            onClick = {}
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_drag_handle_24),
                                contentDescription = stringResource(R.string.content_desc_drag_reorder),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item {
            ReorderableItem(reorderableLazyListState, key = "separator") { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.sound_mode_long_press_hint),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(disabledModes.size, key = { disabledModes[it] }) { index ->
            val mode = disabledModes[index]
            ReorderableItem(reorderableLazyListState, key = mode) { _ ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .highlight(highlightSetting == mode)
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                // enable
                                val modeToEnable = disabledModes[index]
                                disabledModes = disabledModes.toMutableList().apply { removeAt(index) }
                                activeModes = activeModes + modeToEnable
                                // save
                                prefs.edit {
                                    putString("sound_mode_order", activeModes.joinToString(","))
                                    putString("sound_mode_disabled", disabledModes.joinToString(","))
                                }
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = modeIcons[mode] ?: R.drawable.rounded_volume_up_24),
                            contentDescription = mode,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(soundModeNames[mode] ?: R.string.tile_sound_mode), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                }
                            ),
                            onClick = {}
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_drag_handle_24),
                                contentDescription = stringResource(R.string.content_desc_drag_reorder),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}