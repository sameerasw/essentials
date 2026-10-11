/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: NestedWidgetSupport.kt
 * Description: Helpers for showing another app's widget inside one of our own widgets.
 */

package com.sameerasw.essentials.services.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import android.util.SizeF
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.sameerasw.essentials.R

object NestedWidgetSupport {
    private const val TAG = "NestedWidgetSupport"

    fun canRender(
        context: Context,
        remoteViews: RemoteViews,
    ): Boolean {
        val parcel = Parcel.obtain()
        return try {
            val page = RemoteViews(context.packageName, R.layout.widget_stack_page)
            page.addView(R.id.stack_page, remoteViews)
            val probe = RemoteViews(context.packageName, R.layout.widget_stack)
            probe.addView(R.id.stack_flipper, page)
            probe.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            RemoteViews.CREATOR.createFromParcel(parcel).apply(context, FrameLayout(context))
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Widget can't be nested: $e")
            false
        } finally {
            parcel.recycle()
        }
    }

    fun resolveSized(
        context: Context,
        remoteViews: RemoteViews,
        size: SizeF?,
    ): RemoteViews {
        if (size == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return remoteViews
        return try {
            RemoteViews::class.java
                .getMethod("getRemoteViewsToApply", Context::class.java, SizeF::class.java)
                .invoke(remoteViews, context, size) as? RemoteViews ?: remoteViews
        } catch (e: Throwable) {
            Log.w(TAG, "Couldn't pick a layout for $size", e)
            remoteViews
        }
    }

    fun sizedOptions(
        base: Bundle,
        widthDp: Int,
        heightDp: Int,
    ): Bundle =
        Bundle(base).apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(widthDp.toFloat(), heightDp.toFloat())),
                )
            }
        }

    fun requestUpdate(
        context: Context,
        awm: AppWidgetManager,
        hostedId: Int,
    ) {
        val provider = awm.getAppWidgetInfo(hostedId)?.provider ?: return
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .setComponent(provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(hostedId)),
        )
    }
}
