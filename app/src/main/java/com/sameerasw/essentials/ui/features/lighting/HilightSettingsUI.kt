/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Lighting Features
 * File: HilightSettingsUI.kt
 * Description: Hilight settings: the hub page, per-app notification effects and live notification progress.
 */

package com.sameerasw.essentials.ui.features.system

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.controller.HilightController
import com.sameerasw.essentials.domain.model.HilightEffect
import com.sameerasw.essentials.domain.model.HilightPattern
import com.sameerasw.essentials.domain.model.HilightProgressColorMode
import com.sameerasw.essentials.domain.model.HilightProgressFrames
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.HilightEffectSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.hardware.HilightLights
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

// Shown below the hub's two entries: the conditions both of them follow
@Composable
fun HilightSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                title = stringResource(R.string.hilight_only_screen_off_title),
                isChecked = viewModel.isHilightOnlyWhenScreenOff.value,
                onCheckedChange = { viewModel.setHilightOnlyWhenScreenOff(it) },
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_do_not_disturb_on_24,
                title = stringResource(R.string.flashlight_pulse_disable_on_dnd_title),
                isChecked = viewModel.isHilightSkipDnd.value,
                onCheckedChange = { viewModel.setHilightSkipDnd(it) },
                modifier = Modifier.highlight(highlightSetting == "hilight_skip_dnd"),
            )
        }

        RoundedCardContainer {
            Text(
                text = stringResource(R.string.hilight_hint),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun HilightNotificationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val isHilightDevice = remember { DeviceUtils.isHilightDevice() }
    var showAppPicker by remember { mutableStateOf(false) }
    // Package whose effect sheet is open; a new app goes straight from the picker to its sheet
    var editingPackage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.hilight_notifications_title),
                description = stringResource(R.string.hilight_notifications_desc),
                isChecked = viewModel.isHilightNotificationsEnabled.value,
                onCheckedChange = { viewModel.setHilightNotificationsEnabled(it) },
                enabled = isHilightDevice,
                onDisabledClick = {
                    Toast.makeText(context, R.string.hilight_not_supported_toast, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.highlight(highlightSetting == "hilight_notifications"),
            )
            ConfigSliderItem(
                title = stringResource(R.string.hilight_cooldown_title),
                description = stringResource(R.string.hilight_cooldown_desc),
                value = viewModel.hilightCooldownSeconds.intValue.toFloat(),
                onValueChange = { viewModel.setHilightCooldownSeconds(it.toInt()) },
                valueRange = 0f..300f,
                steps = 19,
                increment = 15f,
                valueFormatter = { formatCooldown(context, it.toInt()) },
                iconRes = R.drawable.rounded_timer_24,
                modifier = Modifier.highlight(highlightSetting == "hilight_cooldown"),
            )
        }

        Text(
            text = stringResource(R.string.hilight_apps_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RoundedCardContainer(spacing = 2.dp) {
            val apps = viewModel.hilightAppEffects.entries.sortedBy { it.key }
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.hilight_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                            ).padding(16.dp),
                )
            }
            apps.forEach { (packageName, effect) ->
                HilightAppItem(
                    packageName = packageName,
                    effect = effect,
                    onClick = { editingPackage = packageName },
                    onRemove = { viewModel.removeHilightApp(packageName) },
                )
            }
            HilightAddAppItem(onClick = { showAppPicker = true })
        }
    }

    if (showAppPicker) {
        SingleAppSelectionSheet(
            onDismissRequest = { showAppPicker = false },
            onAppSelected = { app -> editingPackage = app.packageName },
        )
    }

    editingPackage?.let { packageName ->
        HilightEffectSheet(
            title = appLabel(packageName),
            initialEffect = viewModel.hilightAppEffects[packageName] ?: HilightEffect(),
            onDismiss = { editingPackage = null },
            onSave = { effect ->
                viewModel.setHilightAppEffect(packageName, effect)
                editingPackage = null
            },
            canTry = isHilightDevice,
        )
    }
}

@Composable
fun HilightProgressSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HilightProgressSection(
            viewModel = viewModel,
            isHilightDevice = remember { DeviceUtils.isHilightDevice() },
            highlightSetting = highlightSetting,
        )
    }
}

// Shown above the hub's two entries when the LEDs can't be used
@Composable
fun HilightStatus(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isHilightDevice = remember { DeviceUtils.isHilightDevice() }
    var isArrayAvailable by remember { mutableStateOf<Boolean?>(null) }
    var hasShizukuAccess by remember { mutableStateOf(true) }
    var shizukuBinderEvents by remember { mutableIntStateOf(0) }

    // Shizuku's binder can arrive after the screen opens, so check again once it does
    DisposableEffect(Unit) {
        val listener = Shizuku.OnBinderReceivedListener { shizukuBinderEvents++ }
        Shizuku.addBinderReceivedListenerSticky(listener)
        onDispose { Shizuku.removeBinderReceivedListener(listener) }
    }

    // Binder calls through Shizuku, so keep them off the main thread
    LaunchedEffect(shizukuBinderEvents) {
        if (!isHilightDevice) return@LaunchedEffect
        hasShizukuAccess = HilightLights.isAccessGranted()
        isArrayAvailable = withContext(Dispatchers.IO) { HilightController.isAvailable() }
    }

    Box(modifier = modifier) {
        if (!isHilightDevice) {
            StatusText(stringResource(R.string.hilight_status_no_device), isError = false)
        } else if (!HilightLights.isModeSupported(context)) {
            StatusText(stringResource(R.string.hilight_status_unsupported_mode), isError = true)
        } else if (isArrayAvailable == false) {
            StatusText(
                stringResource(if (hasShizukuAccess) R.string.hilight_status_no_leds else R.string.hilight_status_no_access),
                isError = true,
            )
        }
    }
}

private fun formatCooldown(
    context: Context,
    seconds: Int,
): String =
    when {
        seconds == 0 -> context.getString(R.string.hilight_cooldown_off)
        seconds % 60 == 0 -> context.getString(R.string.hilight_cooldown_minutes, seconds / 60)
        else -> context.getString(R.string.hilight_duration_value, seconds)
    }

@Composable
private fun HilightProgressSection(
    viewModel: MainViewModel,
    isHilightDevice: Boolean,
    highlightSetting: String?,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val mode = viewModel.hilightProgressColorMode.value
    val color = viewModel.hilightProgressColor.intValue

    RoundedCardContainer(spacing = 2.dp) {
        IconToggleItem(
            iconRes = R.drawable.rounded_downloading_24,
            title = stringResource(R.string.hilight_progress_toggle_title),
            description = stringResource(R.string.hilight_progress_toggle_desc),
            isChecked = viewModel.isHilightProgressEnabled.value,
            onCheckedChange = { viewModel.setHilightProgressEnabled(it) },
            enabled = isHilightDevice,
            onDisabledClick = {
                Toast.makeText(context, R.string.hilight_not_supported_toast, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.highlight(highlightSetting == "hilight_progress"),
        )
        SegmentedPicker(
            items = HilightProgressColorMode.entries,
            selectedItem = mode,
            onItemSelected = {
                HapticUtil.performUIHaptic(view)
                viewModel.setHilightProgressColorMode(it)
            },
            labelProvider = { context.getString(it.title) },
        )
        HilightProgressPreview(mode = mode, color = color)
        ConfigSliderItem(
            title = stringResource(R.string.hilight_duration_title),
            value = viewModel.hilightProgressDurationMs.longValue / 1000f,
            onValueChange = { viewModel.setHilightProgressDurationMs((it * 1000).toLong()) },
            valueRange = HilightEffect.MIN_DURATION_MS / 1000f..HilightEffect.MAX_DURATION_MS / 1000f,
            increment = 1f,
            steps = ((HilightEffect.MAX_DURATION_MS - HilightEffect.MIN_DURATION_MS) / 1000 - 1).toInt(),
            valueFormatter = { context.getString(R.string.hilight_duration_value, it.toInt()) },
            iconRes = R.drawable.rounded_timer_24,
        )
    }

    // VIBGYOR gives every LED its own fixed hue, so a colour choice would have no effect
    if (mode != HilightProgressColorMode.VIBGYOR) {
        ColorSwatchPicker(
            selectedColorHex = String.format("#%06X", color and 0xFFFFFF),
            onColorSelected = { hex -> viewModel.setHilightProgressColor(hex.toColorInt()) },
        )
    }

    OutlinedButton(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            val played =
                HilightController.play(durationMs = HilightProgressFrames.demoDurationMs(LED_COUNT)) { elapsed, ledCount ->
                    HilightProgressFrames.demo(elapsed, mode, color, ledCount)
                }
            if (!played) Toast.makeText(context, R.string.hilight_unavailable_toast, Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = isHilightDevice,
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_auto_awesome_24),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(stringResource(R.string.hilight_try_it))
    }
}

// The eight LEDs as they look at 100%
@Composable
private fun HilightProgressPreview(
    mode: HilightProgressColorMode,
    color: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(LED_COUNT) { i ->
            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(HilightProgressFrames.ledColor(mode, color, i, LED_COUNT))),
            )
        }
    }
}

private const val LED_COUNT = 8

@Composable
private fun StatusText(
    text: String,
    isError: Boolean,
) {
    RoundedCardContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun HilightAppItem(
    packageName: String,
    effect: HilightEffect,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val icon by produceState<ImageBitmap?>(null, packageName) {
        value =
            withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }
                    .getOrNull()
            }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
            )
        } ?: Box(modifier = Modifier.size(36.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLabel(packageName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (effect.pattern != HilightPattern.RAINBOW) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(effect.color)),
                    )
                }
                Text(
                    text =
                        stringResource(
                            R.string.hilight_app_effect_summary,
                            stringResource(effect.pattern.title),
                            (effect.durationMs / 1000).toInt(),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                onRemove()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_close_24),
                contentDescription = stringResource(R.string.action_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HilightAddAppItem(onClick: () -> Unit) {
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                }.background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_add_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.hilight_add_app),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun appLabel(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}
