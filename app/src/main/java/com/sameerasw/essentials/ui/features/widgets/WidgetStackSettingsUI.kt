/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Widget Settings
 * File: WidgetStackSettingsUI.kt
 * Description: Lists the widget stacks on the homescreen and adds new ones.
 */

package com.sameerasw.essentials.ui.features.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.services.widgets.StackWidgetProvider
import com.sameerasw.essentials.ui.activities.WidgetStackConfigureActivity
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun WidgetStackSettingsUI(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var stacks by remember { mutableStateOf(placedStacks(context)) }

    // Stacks are added and edited outside this screen, so reload whenever it comes back.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) stacks = placedStacks(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer {
            ListItem(
                modifier =
                    Modifier.clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        requestPinStack(context)
                    },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_add_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text(stringResource(R.string.widget_stack_add_to_home)) },
                supportingContent = { Text(stringResource(R.string.widget_stack_add_to_home_desc)) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
            )
        }

        if (stacks.isNotEmpty()) {
            Text(
                text = stringResource(R.string.widget_stack_your_stacks),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
            RoundedCardContainer {
                stacks.forEach { stack ->
                    ListItem(
                        modifier =
                            Modifier.clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                context.startActivity(
                                    Intent(context, WidgetStackConfigureActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, stack.stackWidgetId)
                                    },
                                )
                            },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.rounded_widgets_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        headlineContent = { Text(stackTitle(context, stack)) },
                        supportingContent = { Text(stackCount(context, stack)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    )
                }
            }
        }
    }
}

/** Stacks still on the homescreen, in the order they were added. */
private fun placedStacks(context: Context): List<WidgetStackConfig> {
    val placed = StackWidgetProvider.stackIds(context).toSet()
    return WidgetStackRepository(context).getAll().filter { it.stackWidgetId in placed }
}

private fun stackTitle(
    context: Context,
    stack: WidgetStackConfig,
): String {
    val awm = AppWidgetManager.getInstance(context)
    val labels =
        stack.hostedWidgetIds.mapNotNull { id ->
            awm.getAppWidgetInfo(id)?.loadLabel(context.packageManager)
        }
    return labels.joinToString().ifEmpty { context.getString(R.string.feat_widget_stack_title) }
}

private fun stackCount(
    context: Context,
    stack: WidgetStackConfig,
): String =
    when (val count = stack.hostedWidgetIds.size) {
        0 -> context.getString(R.string.widget_stack_count_none)
        1 -> context.getString(R.string.widget_stack_count_one)
        else -> context.getString(R.string.widget_stack_count_many, count)
    }

private fun requestPinStack(context: Context) {
    val awm = AppWidgetManager.getInstance(context)
    // Once placed, the system fills in the new stack's id and opens its settings to pick widgets.
    val openSettings =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, WidgetStackConfigureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    val pinned =
        awm.isRequestPinAppWidgetSupported &&
            awm.requestPinAppWidget(ComponentName(context, StackWidgetProvider::class.java), null, openSettings)
    if (!pinned) Toast.makeText(context, R.string.widget_stack_pin_unsupported, Toast.LENGTH_LONG).show()
}
