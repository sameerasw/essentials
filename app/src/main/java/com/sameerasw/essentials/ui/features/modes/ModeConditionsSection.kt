/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeConditionsSection.kt
 * Description: Editor section for the conditions that turn a mode on by itself.
 */

package com.sameerasw.essentials.ui.features.modes

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.domain.modes.ModeCondition
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.BluetoothDeviceSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.CalendarStateSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.TimeSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.WifiNetworkSelectionSheet
import com.sameerasw.essentials.ui.features.location.sheets.LocationTriggerSheet
import com.sameerasw.essentials.utils.HapticUtil
import java.text.DateFormatSymbols
import java.util.Calendar
import com.sameerasw.essentials.domain.diy.State as DIYState

private val CONDITION_TEMPLATES: List<ModeCondition> =
    listOf(
        ModeCondition.Schedule(DIYState.TimePeriod(startHour = 9, endHour = 17)),
        ModeCondition.Calendar(),
        ModeCondition.Wifi(),
        ModeCondition.Bluetooth(),
        ModeCondition.Place(),
        ModeCondition.Charging,
        ModeCondition.AndroidAuto,
    )

@Composable
fun ModeConditionsSection(
    conditions: List<ModeCondition>,
    onConditionsChange: (List<ModeCondition>) -> Unit,
    askBeforeActivating: Boolean,
    onAskBeforeActivatingChange: (Boolean) -> Unit,
    onMissingPermissions: (List<AppPermission>) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showKindPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<Int?, ModeCondition>?>(null) }

    fun requirePermissions(keys: List<String>) {
        val missing = keys.mapNotNull { AppPermission.fromKey(it) }.distinct().filterNot { it.isGranted(context) }
        if (missing.isNotEmpty()) onMissingPermissions(missing)
    }

    fun save(
        index: Int?,
        condition: ModeCondition,
    ) {
        onConditionsChange(if (index == null) conditions + condition else conditions.toMutableList().also { it[index] = condition })
        requirePermissions(condition.permissions)
        editing = null
    }

    ModeSectionTitle(R.string.mode_section_conditions)
    RoundedCardContainer(spacing = 2.dp) {
        conditions.forEachIndexed { index, condition ->
            IconToggleItem(
                iconRes = condition.icon,
                title = conditionSummary(context, condition),
                showToggle = false,
                onClick = { if (condition.isConfigurable) editing = index to condition },
                trailingContent = {
                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onConditionsChange(conditions.toMutableList().also { it.removeAt(index) })
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_close_24),
                            contentDescription = stringResource(R.string.action_remove),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
        }
        IconToggleItem(
            iconRes = R.drawable.rounded_add_24,
            title = stringResource(R.string.mode_condition_add),
            showToggle = false,
            onClick = { showKindPicker = true },
        )
        if (conditions.isNotEmpty()) {
            IconToggleItem(
                iconRes = R.drawable.rounded_notifications_unread_24,
                title = stringResource(R.string.mode_ask_before_activating),
                isChecked = askBeforeActivating,
                onCheckedChange = { checked ->
                    onAskBeforeActivatingChange(checked)
                    if (checked) requirePermissions(listOf("POST_NOTIFICATIONS"))
                },
            )
        }
    }

    if (showKindPicker) {
        ConditionKindSheet(
            onDismiss = { showKindPicker = false },
            onPick = { template ->
                showKindPicker = false
                if (template.isConfigurable) editing = null to template else save(null, template)
            },
        )
    }

    editing?.let { (index, condition) ->
        ConditionConfigSheet(
            condition = condition,
            onDismiss = { editing = null },
            onSave = { save(index, it) },
        )
    }
}

private val ModeCondition.isConfigurable: Boolean
    get() = this !is ModeCondition.Charging && this !is ModeCondition.AndroidAuto

@Composable
private fun ConditionConfigSheet(
    condition: ModeCondition,
    onDismiss: () -> Unit,
    onSave: (ModeCondition) -> Unit,
) {
    when (condition) {
        is ModeCondition.Schedule ->
            TimeSelectionSheet(
                initialState = condition.period,
                onDismiss = onDismiss,
                onSaveState = { onSave(ModeCondition.Schedule(it)) },
            )

        is ModeCondition.Calendar -> {
            var draft by remember { mutableStateOf(condition.event) }
            CalendarStateSheet(
                initialState = condition.event,
                onDismiss = { onSave(ModeCondition.Calendar(draft)) },
                onStateChange = { draft = it },
            )
        }

        is ModeCondition.Wifi ->
            WifiNetworkSelectionSheet(
                initialSsid = condition.ssid.ifBlank { null },
                onDismiss = onDismiss,
                onSave = { onSave(ModeCondition.Wifi(it)) },
            )

        is ModeCondition.Bluetooth ->
            BluetoothDeviceSelectionSheet(
                onDismiss = onDismiss,
                onSave = { address, name -> onSave(ModeCondition.Bluetooth(address, name)) },
            )

        is ModeCondition.Place ->
            LocationTriggerSheet(
                title = stringResource(condition.title),
                initialLatitude = condition.latitude,
                initialLongitude = condition.longitude,
                initialRadiusMeters = condition.radiusMeters,
                initialPlaceName = condition.placeName,
                onDismiss = onDismiss,
                onSave = { latitude, longitude, radius, name -> onSave(ModeCondition.Place(latitude, longitude, radius, name)) },
            )

        else -> Unit
    }
}

@Composable
private fun ConditionKindSheet(
    onDismiss: () -> Unit,
    onPick: (ModeCondition) -> Unit,
) {
    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.mode_condition_add,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RoundedCardContainer(spacing = 2.dp) {
                CONDITION_TEMPLATES.forEach { template ->
                    IconToggleItem(
                        iconRes = template.icon,
                        title = stringResource(template.title),
                        showToggle = false,
                        onClick = { onPick(template) },
                    )
                }
            }
        }
    }
}

private fun conditionSummary(
    context: Context,
    condition: ModeCondition,
): String =
    when (condition) {
        is ModeCondition.Schedule -> scheduleSummary(context, condition.period)
        is ModeCondition.Calendar -> {
            val event = condition.event
            val keyword = event.titleContains.trim()
            when {
                event.workingLocationOnly && keyword.isNotEmpty() -> context.getString(R.string.mode_condition_working_location_named, keyword)
                event.workingLocationOnly -> context.getString(R.string.diy_calendar_working_location)
                keyword.isNotEmpty() -> context.getString(R.string.mode_condition_event_named, keyword)
                else -> context.getString(condition.title)
            }
        }
        is ModeCondition.Wifi -> condition.ssid.ifBlank { context.getString(condition.title) }
        is ModeCondition.Bluetooth -> condition.deviceName.ifBlank { condition.deviceAddress.ifBlank { context.getString(condition.title) } }
        is ModeCondition.Place -> condition.placeName.ifBlank { context.getString(condition.title) }
        else -> context.getString(condition.title)
    }

private fun scheduleSummary(
    context: Context,
    period: DIYState.TimePeriod,
): String {
    val format = DateFormat.getTimeFormat(context)
    fun time(
        hour: Int,
        minute: Int,
    ): String =
        format.format(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }.time,
        )

    val range = "${time(period.startHour, period.startMinute)} – ${time(period.endHour, period.endMinute)}"
    if (period.days.isEmpty() || period.days.size == 7) return range
    val names = DateFormatSymbols.getInstance().shortWeekdays
    val days = period.days.sortedBy { (it + 5) % 7 }.joinToString(", ") { names[it] }
    return "$range · $days"
}
