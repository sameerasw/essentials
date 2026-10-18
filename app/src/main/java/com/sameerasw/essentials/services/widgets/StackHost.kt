/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackHost.kt
 * Description: Hosts the widgets placed in widget stacks and rotates each stack on its timer.
 */

package com.sameerasw.essentials.services.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import com.sameerasw.essentials.domain.model.WidgetStackConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object StackHost {
    private const val TAG = "StackHost"
    const val HOST_ID = 1026
    private const val KEEP_ALIVE_WORK = "widget_stack_keep_alive"

    val contents = ConcurrentHashMap<Int, RemoteViews>()

    val unsupported: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    val visibleOnlyStacks: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    val revealedStacks: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private const val REVEAL_MILLIS = 4000L
    private val hideRunnables = mutableMapOf<Int, Runnable>()

    val positions = ConcurrentHashMap<Int, Int>()
    val rawContents = ConcurrentHashMap<Int, RemoteViews>()
    private val checkedLayouts = ConcurrentHashMap<Int, Int>()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var appContext: Context
    private lateinit var repository: WidgetStackRepository
    private var host: CapturingWidgetHost? = null
    private val hostViews = mutableMapOf<Int, CapturingHostView>()
    private var overlayContainer: FrameLayout? = null
    private val timers = mutableMapOf<Int, Runnable>()
    private val pendingRenders = mutableMapOf<Int, Runnable>()

    private class CapturingHostView(
        context: Context,
    ) : AppWidgetHostView(context) {
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            val id = appWidgetId
            if (remoteViews == null) {
                rawContents.remove(id)
                contents.remove(id)
                unsupported.remove(id)
                checkedLayouts.remove(id)
            } else {
                rawContents[id] = remoteViews
                accept(context, id, remoteViews)
            }
            onHostedWidgetChanged(id)
        }
    }

    private class CapturingWidgetHost(
        context: Context,
    ) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = CapturingHostView(context)
    }

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        cancelAllTimers()
                        repository.savePositions(positions)
                    }

                    Intent.ACTION_SCREEN_ON -> repository.getAll().forEach { scheduleTimer(it.stackWidgetId) }
                }
            }
        }

    private val configurationCallbacks =
        object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {
                repository.getAll().forEach { StackWidgetProvider.forwardSize(appContext, it) }
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {}

            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) repository.savePositions(positions)
            }
        }

    fun startIfNeeded(context: Context) {
        if (WidgetStackRepository(context).getAll().isNotEmpty()) refresh(context)
    }

    fun refresh(context: Context) {
        val app = context.applicationContext
        handler.post {
            if (host == null) start(app)
            sync()
        }
    }

    fun step(
        context: Context,
        stackWidgetId: Int,
        delta: Int,
    ) {
        val app = context.applicationContext
        handler.post {
            if (host == null) start(app)
            reveal(stackWidgetId)
            advance(stackWidgetId, delta)
            positions[stackWidgetId]?.let { pos ->
                repository.savePositions(mapOf(stackWidgetId to pos))
            }
        }
    }

    fun revealControls(
        context: Context,
        stackWidgetId: Int,
    ) {
        val app = context.applicationContext
        handler.post {
            if (host == null) start(app)
            reveal(stackWidgetId)
            StackWidgetProvider.render(appContext, stackWidgetId)
        }
    }

    private fun reveal(stackWidgetId: Int) {
        if (repository.get(stackWidgetId)?.controls != WidgetStackConfig.ControlsMode.AUTO_HIDE) return
        revealedStacks.add(stackWidgetId)
        hideRunnables.remove(stackWidgetId)?.let { handler.removeCallbacks(it) }
        val hide =
            Runnable {
                hideRunnables.remove(stackWidgetId)
                revealedStacks.remove(stackWidgetId)
                StackWidgetProvider.render(appContext, stackWidgetId)
            }
        hideRunnables[stackWidgetId] = hide
        handler.postDelayed(hide, REVEAL_MILLIS)
    }

    private fun start(context: Context) {
        appContext = context
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            KEEP_ALIVE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StackHostWorker>(15, TimeUnit.MINUTES).build(),
        )
        repository = WidgetStackRepository(context)
        positions.putAll(StackPositions.restore(repository.getPositions(), repository.getAll()))
        host = CapturingWidgetHost(context).also { it.startListening() }
        removeOrphanedWidgets()
        attachOverlay()
        ContextCompat.registerReceiver(
            context,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        context.registerComponentCallbacks(configurationCallbacks)
    }

    private fun stop() {
        WorkManager.getInstance(appContext).cancelUniqueWork(KEEP_ALIVE_WORK)
        handler.removeCallbacksAndMessages(null)
        timers.clear()
        pendingRenders.clear()
        hideRunnables.clear()
        revealedStacks.clear()
        positions.clear()
        visibleOnlyStacks.clear()
        contents.clear()
        rawContents.clear()
        unsupported.clear()
        checkedLayouts.clear()
        try {
            appContext.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        appContext.unregisterComponentCallbacks(configurationCallbacks)
        detachOverlay()
        hostViews.clear()
        host?.stopListening()
        host = null
    }

    private fun sync() {
        val stacks = repository.getAll()
        if (stacks.isEmpty()) {
            stop()
            return
        }

        if (overlayContainer == null) {
            attachOverlay()
            hostViews.values.forEach { overlayContainer?.addView(it, FrameLayout.LayoutParams(1, 1)) }
        }

        val awm = AppWidgetManager.getInstance(appContext)
        val wanted = stacks.flatMap { it.hostedWidgetIds }.toSet()

        (hostViews.keys - wanted).forEach { id ->
            hostViews.remove(id)?.let { overlayContainer?.removeView(it) }
            contents.remove(id)
            rawContents.remove(id)
            unsupported.remove(id)
            checkedLayouts.remove(id)
        }

        for (id in wanted) {
            if (hostViews.containsKey(id)) continue
            val info = awm.getAppWidgetInfo(id) ?: continue
            try {
                val view = host?.createView(appContext, id, info) as? CapturingHostView ?: continue
                hostViews[id] = view
                overlayContainer?.addView(view, FrameLayout.LayoutParams(1, 1))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to host widget $id", e)
            }
        }

        for (stack in stacks) {
            StackWidgetProvider.forwardSize(appContext, stack)
            StackWidgetProvider.render(appContext, stack.stackWidgetId)
            scheduleTimer(stack.stackWidgetId)
        }
        (timers.keys - stacks.map { it.stackWidgetId }.toSet()).forEach { cancelTimer(it) }
    }

    private fun removeOrphanedWidgets() {
        val currentHost = host ?: return
        val used = repository.getAll().flatMap { it.hostedWidgetIds }.toSet()
        try {
            currentHost.appWidgetIds
                .filterNot { it in used }
                .forEach { currentHost.deleteAppWidgetId(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean up orphaned widgets", e)
        }
    }

    private fun accept(
        context: Context,
        id: Int,
        raw: RemoteViews,
    ) {
        val resolved = NestedWidgetSupport.resolveSized(context, raw, StackWidgetProvider.innerSize(id))
        if (checkedLayouts[id] == resolved.layoutId || NestedWidgetSupport.canRender(context, resolved)) {
            checkedLayouts[id] = resolved.layoutId
            contents[id] = resolved
            unsupported.remove(id)
        } else {
            checkedLayouts.remove(id)
            contents.remove(id)
            unsupported.add(id)
        }
    }

    fun onInnerSizeChanged(
        context: Context,
        hostedId: Int,
    ) {
        val raw = rawContents[hostedId] ?: return
        accept(context, hostedId, raw)
        onHostedWidgetChanged(hostedId)
    }

    private fun onHostedWidgetChanged(hostedWidgetId: Int) {
        repository
            .getAll()
            .filter { hostedWidgetId in it.hostedWidgetIds }
            .forEach { stack ->
                val stackId = stack.stackWidgetId
                pendingRenders.remove(stackId)?.let { handler.removeCallbacks(it) }
                val render =
                    Runnable {
                        pendingRenders.remove(stackId)
                        StackWidgetProvider.render(appContext, stackId)
                    }
                pendingRenders[stackId] = render
                handler.postDelayed(render, 100L)
            }
    }

    private fun advance(
        stackWidgetId: Int,
        delta: Int,
    ) {
        val count = repository.get(stackWidgetId)?.hostedWidgetIds?.size ?: 0
        if (count > 0) {
            val current = positions[stackWidgetId] ?: 0
            positions[stackWidgetId] = Math.floorMod(current + delta, count)
        }
        StackWidgetProvider.render(appContext, stackWidgetId, animate = true, reverse = delta < 0)
        scheduleTimer(stackWidgetId)
    }

    private fun scheduleTimer(stackWidgetId: Int) {
        cancelTimer(stackWidgetId)
        val stack = repository.get(stackWidgetId) ?: return
        if (stack.intervalSeconds <= 0 || stack.hostedWidgetIds.size < 2) return
        val power = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!power.isInteractive) return

        val tick = Runnable { advance(stackWidgetId, 1) }
        timers[stackWidgetId] = tick
        handler.postDelayed(tick, stack.intervalSeconds * 1000L)
    }

    private fun cancelTimer(stackWidgetId: Int) {
        timers.remove(stackWidgetId)?.let { handler.removeCallbacks(it) }
    }

    private fun cancelAllTimers() {
        timers.values.forEach { handler.removeCallbacks(it) }
        timers.clear()
    }

    private fun attachOverlay() {
        if (!Settings.canDrawOverlays(appContext)) return
        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val container = FrameLayout(appContext)
            val params =
                WindowManager.LayoutParams(
                    1,
                    1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = -100
                    y = -100
                }
            wm.addView(container, params)
            overlayContainer = container
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
        }
    }

    private fun detachOverlay() {
        val container = overlayContainer ?: return
        try {
            (appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(container)
        } catch (_: Exception) {
        }
        overlayContainer = null
    }
}
