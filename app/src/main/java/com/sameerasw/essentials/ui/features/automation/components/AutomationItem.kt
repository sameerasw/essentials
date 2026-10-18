/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: AutomationItem.kt
 * Description: UI component and settings composable for Automation feature domain.
 */

package com.sameerasw.essentials.ui.components.diy

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.diy.State as DIYState
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import java.text.DateFormatSymbols
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutomationItem(
    automation: Automation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggle: () -> Unit = {},
    onTest: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
            ),
        shape = MaterialTheme.shapes.extraSmall,
        modifier =
            modifier
                .combinedClickable(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (selectionMode) onSelectedChange(!selected) else onClick()
                    },
                    onLongClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (selectionMode) onSelectedChange(!selected) else showMenu = true
                    },
                ).alpha(if (automation.isEnabled) 1f else 0.5f),
    ) {
        Box {
            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = DpOffset(0.dp, 0.dp),
            ) {
                val toggleText =
                    if (automation.isEnabled) {
                        stringResource(R.string.action_disable)
                    } else {
                        stringResource(
                            R.string.action_enable,
                        )
                    }
                val toggleIcon =
                    if (automation.isEnabled) R.drawable.rounded_close_24 else R.drawable.rounded_check_24

                SegmentedDropdownMenuItem(
                    text = { Text(toggleText) },
                    onClick = {
                        showMenu = false
                        onToggle()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(toggleIcon),
                            contentDescription = null,
                        )
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_test)) },
                    onClick = {
                        showMenu = false
                        onTest()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_play_arrow_24),
                            contentDescription = null,
                        )
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit)) },
                    onClick = {
                        showMenu = false
                        onClick()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_edit_24),
                            contentDescription = null,
                        )
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_select)) },
                    onClick = {
                        showMenu = false
                        onSelectedChange(true)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_check_circle_24),
                            contentDescription = null,
                        )
                    },
                )
                SegmentedDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.rounded_delete_24),
                            contentDescription = null,
                        )
                    },
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = selectionMode,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onSelectedChange(it)
                        },
                    )
                }
                RoundedCardContainer(
                    cornerRadius = 18.dp,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    val icon =
                        when (automation.type) {
                            Automation.Type.TRIGGER -> automation.trigger?.icon
                            Automation.Type.ACTION_SHORTCUT -> R.drawable.rounded_rocket_launch_24
                            Automation.Type.ACCESSIBILITY_SHORTCUT,
                            Automation.Type.ACCESSIBILITY_SHORTCUT_1 -> R.drawable.rounded_circle_24
                            Automation.Type.ACCESSIBILITY_SHORTCUT_2 -> R.drawable.rounded_pentagon_24
                            Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> R.drawable.rounded_square_24
                            Automation.Type.PIXEL_SEARCHBAR -> R.drawable.rounded_search_24
                            Automation.Type.STATE -> automation.state?.icon
                            Automation.Type.APP -> R.drawable.rounded_apps_24
                        }

                    val titleString =
                        when (automation.type) {
                            Automation.Type.TRIGGER ->
                                when (val trigger = automation.trigger) {
                                    is Trigger.ArriveAtLocation ->
                                        trigger.placeName.takeIf { it.isNotBlank() }?.let { stringResource(R.string.diy_trigger_arrive_location_named, it) }
                                            ?: stringResource(trigger.title)
                                    is Trigger.LeaveLocation ->
                                        trigger.placeName.takeIf { it.isNotBlank() }?.let { stringResource(R.string.diy_trigger_leave_location_named, it) }
                                            ?: stringResource(trigger.title)
                                    else -> trigger?.title?.let { stringResource(it) }
                                }

                            Automation.Type.ACTION_SHORTCUT -> stringResource(R.string.diy_create_action_shortcut_title)
                            Automation.Type.ACCESSIBILITY_SHORTCUT -> stringResource(R.string.diy_create_accessibility_shortcut_1_title)
                            Automation.Type.ACCESSIBILITY_SHORTCUT_1 -> stringResource(R.string.diy_create_accessibility_shortcut_1_title)
                            Automation.Type.ACCESSIBILITY_SHORTCUT_2 -> stringResource(R.string.diy_create_accessibility_shortcut_2_title)
                            Automation.Type.ACCESSIBILITY_SHORTCUT_3 -> stringResource(R.string.diy_create_accessibility_shortcut_3_title)
                            Automation.Type.PIXEL_SEARCHBAR -> stringResource(R.string.diy_create_pixel_searchbar_title)
                            Automation.Type.STATE -> automation.state?.title?.let { stringResource(it) }
                            Automation.Type.APP -> stringResource(R.string.diy_create_app_title) + " (${automation.selectedApps.size})"
                        }

                    if (icon != null && titleString != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val context = LocalContext.current
                                val validatedIcon =
                                    remember(icon) {
                                        try {
                                            if (context.resources.getResourceTypeName(icon) == "drawable") {
                                                icon
                                            } else {
                                                R.drawable.rounded_do_not_disturb_on_24
                                            }
                                        } catch (e: Exception) {
                                            R.drawable.rounded_do_not_disturb_on_24
                                        }
                                    }
                                Icon(
                                    painter = painterResource(id = validatedIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = titleString,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    automationSummary(automation)?.let { summary ->
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (automation.type == Automation.Type.TRIGGER ||
                    automation.type == Automation.Type.ACTION_SHORTCUT ||
                    automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT ||
                    automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_1 ||
                    automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_2 ||
                    automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_3 ||
                    automation.type == Automation.Type.PIXEL_SEARCHBAR
                ) {
                    // Separator Icon
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .padding(horizontal = 3.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_arrow_forward_24),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .padding(horizontal = 3.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_arrow_forward_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .padding(horizontal = 3.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_arrow_back_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }

                // Right Side: Actions (Weight 1 to fill space)

                RoundedCardContainer(
                    cornerRadius = 18.dp,
                    modifier =
                        Modifier
                            .weight(1f),
                ) {
                    if (automation.type == Automation.Type.TRIGGER ||
                        automation.type == Automation.Type.ACTION_SHORTCUT ||
                        automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT ||
                        automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_1 ||
                        automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_2 ||
                        automation.type == Automation.Type.ACCESSIBILITY_SHORTCUT_3 ||
                        automation.type == Automation.Type.PIXEL_SEARCHBAR
                    ) {
                        automation.actionList.forEach { action ->
                            ActionItem(action = action)
                        }
                    } else {
                        // One row per side keeps them level with the in and out arrows
                        ActionItem(action = automation.entryActionList.firstOrNull(), moreCount = automation.entryActionList.size - 1)
                        ActionItem(action = automation.exitActionList.firstOrNull(), moreCount = automation.exitActionList.size - 1)
                    }
                }
            }
        }
    }
}

// Short description of the configured trigger/state values, so users don't have to open the editor to recall them
@Composable
private fun automationSummary(automation: Automation): String? {
    val context = LocalContext.current
    val everyDay = stringResource(R.string.diy_summary_every_day)
    val allCalendars = stringResource(R.string.diy_summary_all_calendars)

    fun time(
        hour: Int,
        minute: Int,
    ): String {
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    fun days(days: Set<Int>): String {
        if (days.isEmpty() || days.size == 7) return everyDay
        val firstDay = Calendar.getInstance().firstDayOfWeek
        val names = DateFormatSymbols.getInstance().shortWeekdays
        return (0 until 7)
            .map { (firstDay - 1 + it) % 7 + 1 }
            .filter { it in days }
            .joinToString(", ") { names[it] }
    }

    return when (automation.type) {
        Automation.Type.TRIGGER ->
            when (val trigger = automation.trigger) {
                is Trigger.Schedule -> "${time(trigger.hour, trigger.minute)} · ${days(trigger.days)}"
                is Trigger.BluetoothConnected -> trigger.deviceName.ifBlank { null }
                is Trigger.BluetoothDisconnected -> trigger.deviceName.ifBlank { null }
                is Trigger.WifiConnected -> trigger.ssid.ifBlank { null }
                is Trigger.WifiDisconnected -> trigger.ssid.ifBlank { null }
                is Trigger.NotificationReceived ->
                    listOfNotNull(
                        trigger.appName.ifBlank { null },
                        trigger.textContains.ifBlank { null }?.let { "\"$it\"" },
                    ).joinToString(" · ").ifBlank { null }
                else -> null
            }

        Automation.Type.STATE ->
            when (val state = automation.state) {
                is DIYState.TimePeriod ->
                    "${time(state.startHour, state.startMinute)} – ${time(state.endHour, state.endMinute)} · ${days(state.days)}"
                is DIYState.BatteryLevel -> "${state.minLevel}% – ${state.maxLevel}%"
                is DIYState.CalendarEvent ->
                    if (state.calendarIds.isEmpty()) {
                        allCalendars
                    } else {
                        stringResource(R.string.diy_summary_calendars_count, state.calendarIds.size)
                    }
                else -> null
            }

        Automation.Type.APP ->
            remember(automation.selectedApps) {
                val pm = context.packageManager
                automation.selectedApps
                    .map { packageName ->
                        try {
                            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                        } catch (e: Exception) {
                            packageName
                        }
                    }.joinToString(", ")
                    .ifBlank { null }
            }

        else -> null
    }
}

@Composable
fun ActionItem(
    action: Action?,
    modifier: Modifier = Modifier,
    moreCount: Int = 0,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current
            val iconId = action?.icon ?: R.drawable.rounded_do_not_disturb_on_24
            val validatedIcon =
                remember(iconId) {
                    try {
                        if (context.resources.getResourceTypeName(iconId) == "drawable") {
                            iconId
                        } else {
                            R.drawable.rounded_do_not_disturb_on_24
                        }
                    } catch (e: Exception) {
                        R.drawable.rounded_do_not_disturb_on_24
                    }
                }
            Icon(
                painter = painterResource(id = validatedIcon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = action?.title ?: R.string.haptic_none),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (action is Action.OpenApp && action.packageName.isNotBlank()) {
                    val appName =
                        remember(action.packageName) {
                            try {
                                val pm = context.packageManager
                                pm.getApplicationInfo(action.packageName, 0).loadLabel(pm).toString()
                            } catch (e: Exception) {
                                action.packageName
                            }
                        }
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (moreCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.diy_more_actions, moreCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Automation summaries", showBackground = true, heightDp = 1400)
@Composable
private fun AutomationItemSummariesPreview() {
    val weekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    val actions = listOf(Action.ToggleFlashlight)
    val samples =
        listOf(
            Automation("1", Automation.Type.TRIGGER, trigger = Trigger.Schedule(hour = 7, minute = 30, days = weekdays), actions = actions),
            Automation("2", Automation.Type.TRIGGER, trigger = Trigger.Schedule(hour = 22, minute = 0), actions = actions),
            Automation("3", Automation.Type.TRIGGER, trigger = Trigger.BluetoothConnected("00:11:22:33:44:55", "Pixel Buds Pro"), actions = actions),
            Automation("4", Automation.Type.TRIGGER, trigger = Trigger.BluetoothDisconnected("AA:BB:CC:DD:EE:FF", ""), actions = actions),
            Automation("13", Automation.Type.TRIGGER, trigger = Trigger.NotificationReceived(packageName = "com.example.chat", appName = "Messages", textContains = "code"), actions = actions),
            Automation("5", Automation.Type.TRIGGER, trigger = Trigger.WifiConnected("Home Wi-Fi"), actions = actions),
            Automation("6", Automation.Type.TRIGGER, trigger = Trigger.WifiDisconnected("Office"), actions = actions),
            Automation(
                "7",
                Automation.Type.STATE,
                state = DIYState.TimePeriod(startHour = 22, startMinute = 0, endHour = 7, endMinute = 0, days = setOf(Calendar.SATURDAY, Calendar.SUNDAY)),
                entryActions = listOf(Action.TurnOnLowPower),
                exitActions = listOf(Action.TurnOffLowPower),
            ),
            Automation(
                "8",
                Automation.Type.STATE,
                state = DIYState.BatteryLevel(minLevel = 0, maxLevel = 20),
                entryActions = listOf(Action.TurnOnLowPower),
                exitActions = listOf(Action.TurnOffLowPower),
            ),
            Automation(
                "9",
                Automation.Type.STATE,
                state = DIYState.CalendarEvent(),
                entryActions = listOf(Action.ToggleDoNotDisturb),
                exitActions = listOf(Action.ToggleDoNotDisturb),
            ),
            Automation(
                "10",
                Automation.Type.STATE,
                state = DIYState.CalendarEvent(calendarIds = setOf(1L, 2L, 3L)),
                entryActions = listOf(Action.ToggleDoNotDisturb),
                exitActions = listOf(Action.ToggleDoNotDisturb),
            ),
            Automation(
                "11",
                Automation.Type.APP,
                selectedApps = listOf("com.google.android.apps.maps", "com.spotify.music"),
                entryActions = listOf(Action.TurnOnLowPower),
                exitActions = listOf(Action.TurnOffLowPower),
            ),
            // No configurable values, or no name to show, so no summary line
            Automation("12", Automation.Type.TRIGGER, trigger = Trigger.ChargerConnected, actions = actions),
        )

    EssentialsTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            samples.forEach { AutomationItem(automation = it) }
        }
    }
}
