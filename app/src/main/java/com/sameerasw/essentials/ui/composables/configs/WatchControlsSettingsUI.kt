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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.sameerasw.essentials.services.DeviceInfoSyncManager
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.min

@Composable
fun WatchControlsSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
    }

    val defaultLayout = "LOCK,SOUND,FLASHLIGHT,FLASHLIGHT_PULSE,AOD,TAP_TO_WAKE"
    val savedLayout = prefs.getString("watch_controls_layout", defaultLayout) ?: defaultLayout
    var activeControls by remember { mutableStateOf(savedLayout.split(",").filter { it.isNotBlank() }) }

    val allPossible = listOf("LOCK", "SOUND", "FLASHLIGHT", "FLASHLIGHT_PULSE", "AOD", "TAP_TO_WAKE")
    var disabledControls by remember {
        mutableStateOf(allPossible.filter { it !in activeControls })
    }

    val controlIcons = mapOf(
        "LOCK" to R.drawable.rounded_lock_24,
        "SOUND" to R.drawable.rounded_volume_up_24,
        "FLASHLIGHT" to R.drawable.rounded_flashlight_on_24,
        "FLASHLIGHT_PULSE" to R.drawable.outline_backlight_high_24,
        "AOD" to R.drawable.rounded_mobile_text_2_24,
        "TAP_TO_WAKE" to R.drawable.rounded_touch_app_24
    )

    val controlNames = mapOf(
        "LOCK" to R.string.feat_lock_from_watch_title,
        "SOUND" to R.string.tile_sound_mode,
        "FLASHLIGHT" to R.string.tile_flashlight,
        "FLASHLIGHT_PULSE" to R.string.tile_flashlight_pulse,
        "AOD" to R.string.feat_always_on_display_title,
        "TAP_TO_WAKE" to R.string.tile_tap_to_wake
    )

    fun save() {
        prefs.edit {
            putString("watch_controls_layout", activeControls.joinToString(","))
        }
        DeviceInfoSyncManager.forceSync(context)
    }

    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val originalActiveSize = activeControls.size
        val fromKey: String = when {
            from.index < originalActiveSize -> activeControls.getOrNull(from.index)
                ?: return@rememberReorderableLazyListState
            from.index == originalActiveSize -> "separator"
            else -> disabledControls.getOrNull(from.index - originalActiveSize - 1)
                ?: return@rememberReorderableLazyListState
        }
        if (fromKey == "separator") return@rememberReorderableLazyListState

        if (from.index < originalActiveSize) {
            activeControls = activeControls.toMutableList().apply { removeAt(from.index) }
        } else {
            disabledControls = disabledControls.toMutableList()
                .apply { removeAt(from.index - originalActiveSize - 1) }
        }

        val newActiveSize = activeControls.size
        val newDisabledSize = disabledControls.size
        if (to.index < newActiveSize) {
            activeControls = activeControls.toMutableList().apply { add(to.index, fromKey) }
        } else if (to.index == newActiveSize) {
            activeControls = activeControls.toMutableList().apply { add(newActiveSize, fromKey) }
        } else {
            val pos = min(to.index - newActiveSize - 1, newDisabledSize)
            disabledControls = disabledControls.toMutableList().apply { add(pos, fromKey) }
        }

        // Enforce at least 1 active
        if (from.index < originalActiveSize && activeControls.isEmpty() && disabledControls.isNotEmpty()) {
            val restore = disabledControls[0]
            activeControls = listOf(restore)
            disabledControls = disabledControls.drop(1)
        }

        save()
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    Text(
        text = stringResource(R.string.watch_controls_reorder_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(activeControls.size, key = { activeControls[it] }) { index ->
            val key = activeControls[index]
            ReorderableItem(reorderableLazyListState, key = key) { _ ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                if (activeControls.size > 1) {
                                    val toDisable = activeControls[index]
                                    activeControls = activeControls.toMutableList().apply { removeAt(index) }
                                    disabledControls = disabledControls + toDisable
                                }
                                save()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = controlIcons[key] ?: R.drawable.rounded_watch_24),
                            contentDescription = key,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(controlNames[key] ?: R.string.feat_watch_controls_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                                contentDescription = null,
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.watch_controls_long_press_hint),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(disabledControls.size, key = { disabledControls[it] }) { index ->
            val key = disabledControls[index]
            ReorderableItem(reorderableLazyListState, key = key) { _ ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                val toEnable = disabledControls[index]
                                disabledControls = disabledControls.toMutableList().apply { removeAt(index) }
                                activeControls = activeControls + toEnable
                                save()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            })
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = controlIcons[key] ?: R.drawable.rounded_watch_24),
                            contentDescription = key,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(controlNames[key] ?: R.string.feat_watch_controls_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}
