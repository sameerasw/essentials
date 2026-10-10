/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackWidgetProvider.kt
 * Description: Homescreen widget that shows one widget of a stack at a time.
 */

package com.sameerasw.essentials.services.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.ui.activities.WidgetStackConfigureActivity

class StackWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { render(context, it) }
        StackHost.refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        WidgetStackRepository(context).get(appWidgetId)?.let { forwardSize(context, it) }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val repository = WidgetStackRepository(context)
        val host = AppWidgetHost(context, StackHost.HOST_ID)
        for (stackId in appWidgetIds) {
            repository.get(stackId)?.hostedWidgetIds?.forEach { hostedId ->
                try {
                    host.deleteAppWidgetId(hostedId)
                } catch (_: Exception) {
                }
                StackHost.contents.remove(hostedId)
            }
            repository.delete(stackId)
            StackHost.positions.remove(stackId)
            StackHost.visibleOnlyStacks.remove(stackId)
            StackHost.revealedStacks.remove(stackId)
        }
        StackHost.refresh(context)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            StackHost.startIfNeeded(context)
            return
        }
        if (intent.action == ACTION_REVEAL) {
            val stackId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (stackId != AppWidgetManager.INVALID_APPWIDGET_ID) StackHost.revealControls(context, stackId)
            return
        }
        if (intent.action == ACTION_STEP) {
            val stackId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val delta = intent.getIntExtra(EXTRA_DELTA, 1)
            if (stackId != AppWidgetManager.INVALID_APPWIDGET_ID) StackHost.step(context, stackId, delta)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val TAG = "StackWidgetProvider"
        private const val ACTION_STEP = "com.sameerasw.essentials.action.WIDGET_STACK_STEP"
        private const val ACTION_REVEAL = "com.sameerasw.essentials.action.WIDGET_STACK_REVEAL"
        private const val EXTRA_DELTA = "delta"

        // Control bar height plus its top margin, in dp.
        private const val CONTROLS_SPACE_DP = 32

        fun stackIds(context: Context): IntArray =
            AppWidgetManager
                .getInstance(context)
                .getAppWidgetIds(ComponentName(context, StackWidgetProvider::class.java))

        /** Whether the strip below the stack takes up space, even while its bar is auto-hidden. */
        private fun reservesControls(config: WidgetStackConfig) =
            config.controls != WidgetStackConfig.ControlsMode.HIDDEN && config.hostedWidgetIds.size > 1

        fun render(
            context: Context,
            stackWidgetId: Int,
        ) {
            val awm = AppWidgetManager.getInstance(context)
            if (stackWidgetId !in StackHost.visibleOnlyStacks) {
                try {
                    awm.updateAppWidget(stackWidgetId, build(context, stackWidgetId, visibleOnly = false))
                    return
                } catch (e: IllegalArgumentException) {
                    // Every widget's images together went over the launcher's memory limit for one widget.
                    Log.w(TAG, "Stack $stackWidgetId is too large, sending only the visible widget", e)
                    StackHost.visibleOnlyStacks.add(stackWidgetId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update stack $stackWidgetId", e)
                    return
                }
            }
            try {
                awm.updateAppWidget(stackWidgetId, build(context, stackWidgetId, visibleOnly = true))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update stack $stackWidgetId", e)
            }
        }

        /**
         * With [visibleOnly], the hidden pages are left empty. Every switch renders the stack again, so
         * the page being shown always has its content.
         */
        private fun build(
            context: Context,
            stackWidgetId: Int,
            visibleOnly: Boolean,
        ): RemoteViews {
            val awm = AppWidgetManager.getInstance(context)
            val config = WidgetStackRepository(context).get(stackWidgetId)
            val hosted = config?.hostedWidgetIds.orEmpty()
            val views = RemoteViews(context.packageName, R.layout.widget_stack)
            views.removeAllViews(R.id.stack_flipper)
            views.removeAllViews(R.id.stack_dots)

            if (config == null || hosted.isEmpty()) {
                views.setViewVisibility(R.id.stack_flipper, View.GONE)
                views.setViewVisibility(R.id.stack_controls, View.GONE)
                views.setViewVisibility(R.id.stack_empty, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.stack_empty, configurePendingIntent(context, stackWidgetId))
                return views
            }

            val index = (StackHost.positions[stackWidgetId] ?: 0).coerceIn(0, hosted.lastIndex)
            StackHost.positions[stackWidgetId] = index

            views.setViewVisibility(R.id.stack_empty, View.GONE)
            views.setViewVisibility(R.id.stack_flipper, View.VISIBLE)
            hosted.forEachIndexed { i, hostedId ->
                val page = RemoteViews(context.packageName, R.layout.widget_stack_page)
                if (!visibleOnly || i == index) {
                    page.addView(R.id.stack_page, StackHost.contents[hostedId] ?: placeholder(context, awm, hostedId))
                }
                views.addView(R.id.stack_flipper, page)
            }
            views.setDisplayedChild(R.id.stack_flipper, index)

            if (!reservesControls(config)) {
                views.setViewVisibility(R.id.stack_controls, View.GONE)
                return views
            }
            views.setViewVisibility(R.id.stack_controls, View.VISIBLE)
            val autoHide = config.controls == WidgetStackConfig.ControlsMode.AUTO_HIDE
            if (autoHide) views.setOnClickPendingIntent(R.id.stack_controls, revealPendingIntent(context, stackWidgetId))

            if (autoHide && stackWidgetId !in StackHost.revealedStacks) {
                // Kept invisible rather than gone so the stacked widget doesn't resize when the bar returns.
                views.setViewVisibility(R.id.stack_controls_bar, View.INVISIBLE)
                views.setViewVisibility(R.id.stack_controls_handle, View.VISIBLE)
                return views
            }
            views.setViewVisibility(R.id.stack_controls_bar, View.VISIBLE)
            views.setViewVisibility(R.id.stack_controls_handle, View.GONE)
            hosted.indices.forEach { i ->
                val dot = if (i == index) R.layout.widget_stack_dot_active else R.layout.widget_stack_dot
                views.addView(R.id.stack_dots, RemoteViews(context.packageName, dot))
            }
            views.setOnClickPendingIntent(R.id.stack_prev, stepPendingIntent(context, stackWidgetId, -1))
            views.setOnClickPendingIntent(R.id.stack_next, stepPendingIntent(context, stackWidgetId, 1))
            return views
        }

        /** Tells every widget in the stack how much space it has inside the stack. */
        fun forwardSize(
            context: Context,
            config: WidgetStackConfig,
        ) {
            val awm = AppWidgetManager.getInstance(context)
            val options = awm.getAppWidgetOptions(config.stackWidgetId)
            if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) == 0) return

            // The launcher can't tell a nested widget which of its size variants to show and falls back
            // to the smallest, so each widget is given only the space it has right now.
            val reserved = if (reservesControls(config)) CONTROLS_SPACE_DP else 0
            val portrait =
                context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val width =
                options.getInt(
                    if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                )
            val height =
                (
                    options.getInt(
                        if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                    ) - reserved
                ).coerceAtLeast(0)

            val forwarded = Bundle(options)
            forwarded.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            forwarded.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
            forwarded.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
            forwarded.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                forwarded.putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(width.toFloat(), height.toFloat())),
                )
            }

            config.hostedWidgetIds.forEach { hostedId ->
                try {
                    awm.updateAppWidgetOptions(hostedId, forwarded)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to forward size to $hostedId", e)
                }
            }
        }

        private fun placeholder(
            context: Context,
            awm: AppWidgetManager,
            hostedWidgetId: Int,
        ): RemoteViews {
            val label =
                awm.getAppWidgetInfo(hostedWidgetId)?.loadLabel(context.packageManager)
                    ?: context.getString(R.string.widget_stack_unknown_widget)
            val text =
                if (hostedWidgetId in StackHost.unsupported) {
                    context.getString(R.string.widget_stack_unsupported, label)
                } else {
                    context.getString(R.string.widget_stack_loading, label)
                }
            return RemoteViews(context.packageName, R.layout.widget_stack_placeholder).apply {
                setTextViewText(R.id.stack_placeholder_label, text)
            }
        }

        private fun stepPendingIntent(
            context: Context,
            stackWidgetId: Int,
            delta: Int,
        ): PendingIntent {
            val intent =
                Intent(context, StackWidgetProvider::class.java).apply {
                    action = ACTION_STEP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, stackWidgetId)
                    putExtra(EXTRA_DELTA, delta)
                }
            return PendingIntent.getBroadcast(
                context,
                stackWidgetId * 2 + if (delta > 0) 1 else 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun revealPendingIntent(
            context: Context,
            stackWidgetId: Int,
        ): PendingIntent {
            val intent =
                Intent(context, StackWidgetProvider::class.java).apply {
                    action = ACTION_REVEAL
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, stackWidgetId)
                }
            return PendingIntent.getBroadcast(
                context,
                stackWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun configurePendingIntent(
            context: Context,
            stackWidgetId: Int,
        ): PendingIntent {
            val intent =
                Intent(context, WidgetStackConfigureActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, stackWidgetId)
                }
            return PendingIntent.getActivity(
                context,
                stackWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
