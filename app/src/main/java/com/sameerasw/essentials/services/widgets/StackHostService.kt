/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackHostService.kt
 * Description: Hosts the widgets placed in widget stacks and rotates each stack on its timer.
 */

package com.sameerasw.essentials.services.widgets

import android.app.Service
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.WidgetStackRepository
import java.util.concurrent.ConcurrentHashMap

class StackHostService : Service() {
    private inner class CapturingHostView(
        context: Context,
    ) : AppWidgetHostView(context) {
        // The launcher renders the content inside the stack, so it is captured here instead of shown.
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            val id = appWidgetId
            when {
                remoteViews == null -> {
                    contents.remove(id)
                    unsupported.remove(id)
                }
                canRender(remoteViews) -> {
                    contents[id] = remoteViews
                    unsupported.remove(id)
                }
                else -> {
                    contents.remove(id)
                    unsupported.add(id)
                }
            }
            onHostedWidgetChanged(id)
        }

        // Some content (such as a list backed by another app's service) breaks once it is nested in
        // the stack and sent to the launcher, which then fails to show the whole stack. Nesting it the
        // same way and rendering the delivered copy here catches that before it reaches the launcher.
        private fun canRender(remoteViews: RemoteViews): Boolean {
            val parcel = Parcel.obtain()
            return try {
                val probe = RemoteViews(packageName, R.layout.widget_stack)
                probe.addView(R.id.stack_flipper, remoteViews)
                probe.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                RemoteViews.CREATOR.createFromParcel(parcel).apply(context, FrameLayout(context))
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Widget $appWidgetId can't be shown in a stack", e)
                false
            } finally {
                parcel.recycle()
            }
        }
    }

    private inner class CapturingWidgetHost(
        context: Context,
    ) : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = CapturingHostView(context)
    }

    companion object {
        private const val TAG = "StackHostService"
        const val HOST_ID = 1026

        private const val ACTION_REFRESH = "com.sameerasw.essentials.action.WIDGET_STACK_REFRESH"

        /** Latest content of every hosted widget, keyed by its app widget id. */
        val contents = ConcurrentHashMap<Int, RemoteViews>()

        /** Hosted widgets whose content can't be shown inside a stack. */
        val unsupported: MutableSet<Int> = ConcurrentHashMap.newKeySet()

        /** Index of the visible widget in each stack, keyed by the stack's app widget id. */
        val positions = ConcurrentHashMap<Int, Int>()

        @Volatile
        private var instance: StackHostService? = null

        fun refresh(context: Context) {
            try {
                context.startService(
                    Intent(context, StackHostService::class.java).setAction(ACTION_REFRESH),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not start stack host", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StackHostService::class.java))
        }

        /** Moves a stack by [delta] widgets and restarts its timer. */
        fun step(
            context: Context,
            stackWidgetId: Int,
            delta: Int,
        ) {
            val service = instance
            if (service != null) {
                service.handler.post { service.advance(stackWidgetId, delta) }
            } else {
                moveIndex(context, stackWidgetId, delta)
                StackWidgetProvider.render(context, stackWidgetId)
                refresh(context)
            }
        }

        private fun moveIndex(
            context: Context,
            stackWidgetId: Int,
            delta: Int,
        ) {
            val count = WidgetStackRepository(context).get(stackWidgetId)?.hostedWidgetIds?.size ?: 0
            if (count == 0) return
            val current = positions[stackWidgetId] ?: 0
            positions[stackWidgetId] = Math.floorMod(current + delta, count)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: WidgetStackRepository
    private var host: CapturingWidgetHost? = null
    private val hostViews = mutableMapOf<Int, CapturingHostView>()
    private var overlayContainer: FrameLayout? = null
    private val timers = mutableMapOf<Int, Runnable>()
    private val pendingRenders = mutableMapOf<Int, Runnable>()

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> cancelAllTimers()
                    Intent.ACTION_SCREEN_ON -> repository.getAll().forEach { scheduleTimer(it.stackWidgetId) }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = WidgetStackRepository(this)
        host = CapturingWidgetHost(this).also { it.startListening() }
        removeOrphanedWidgets()
        attachOverlay()
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        syncHostedWidgets()
        return START_STICKY
    }

    private fun syncHostedWidgets() {
        val stacks = repository.getAll()
        if (stacks.isEmpty()) {
            stopSelf()
            return
        }

        val awm = AppWidgetManager.getInstance(this)
        val wanted = stacks.flatMap { it.hostedWidgetIds }.toSet()

        (hostViews.keys - wanted).forEach { id ->
            hostViews.remove(id)?.let { overlayContainer?.removeView(it) }
            contents.remove(id)
            unsupported.remove(id)
        }

        for (id in wanted) {
            if (hostViews.containsKey(id)) continue
            val info = awm.getAppWidgetInfo(id) ?: continue
            try {
                val view = host?.createView(this, id, info) as? CapturingHostView ?: continue
                hostViews[id] = view
                overlayContainer?.addView(view, FrameLayout.LayoutParams(1, 1))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to host widget $id", e)
            }
        }

        for (stack in stacks) {
            StackWidgetProvider.forwardSize(this, stack)
            StackWidgetProvider.render(this, stack.stackWidgetId)
            scheduleTimer(stack.stackWidgetId)
        }
        (timers.keys - stacks.map { it.stackWidgetId }.toSet()).forEach { cancelTimer(it) }
    }

    /** Releases widget ids this host still holds that no stack uses any more. */
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
                        StackWidgetProvider.render(this, stackId)
                    }
                pendingRenders[stackId] = render
                handler.postDelayed(render, 100L)
            }
    }

    private fun advance(
        stackWidgetId: Int,
        delta: Int,
    ) {
        moveIndex(this, stackWidgetId, delta)
        StackWidgetProvider.render(this, stackWidgetId)
        scheduleTimer(stackWidgetId)
    }

    private fun scheduleTimer(stackWidgetId: Int) {
        cancelTimer(stackWidgetId)
        val stack = repository.get(stackWidgetId) ?: return
        if (stack.intervalSeconds <= 0 || stack.hostedWidgetIds.size < 2) return
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
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

    // A tiny invisible overlay keeps the process important enough to keep receiving widget updates.
    private fun attachOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val container = FrameLayout(this)
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
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(container)
        } catch (_: Exception) {
        }
        overlayContainer = null
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        timers.clear()
        pendingRenders.clear()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        detachOverlay()
        hostViews.clear()
        host?.stopListening()
        host = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
