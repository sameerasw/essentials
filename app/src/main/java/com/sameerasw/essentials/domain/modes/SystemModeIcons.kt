/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: SystemModeIcons.kt
 * Description: Resolves the icon a system mode shows, from its owner app or the platform default for its type.
 */

package com.sameerasw.essentials.domain.modes

import android.annotation.SuppressLint
import android.app.AutomaticZenRule
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

data class SystemMode(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val type: Int,
    val iconPackage: String?,
    val iconName: String?,
)

object SystemModeIcons {
    private const val FRAMEWORK = "android"

    @SuppressLint("InlinedApi")
    private val typeIcons =
        mapOf(
            AutomaticZenRule.TYPE_SCHEDULE_TIME to "ic_zen_mode_type_schedule_time",
            AutomaticZenRule.TYPE_SCHEDULE_CALENDAR to "ic_zen_mode_type_schedule_calendar",
            AutomaticZenRule.TYPE_BEDTIME to "ic_zen_mode_type_bedtime",
            AutomaticZenRule.TYPE_DRIVING to "ic_zen_mode_type_driving",
            AutomaticZenRule.TYPE_IMMERSIVE to "ic_zen_mode_type_immersive",
            AutomaticZenRule.TYPE_THEATER to "ic_zen_mode_type_theater",
            AutomaticZenRule.TYPE_MANAGED to "ic_zen_mode_type_managed",
        )
    private const val DEFAULT_TYPE_ICON = "ic_zen_mode_type_other"

    fun iconName(
        context: Context,
        packageName: String?,
        iconResId: Int,
    ): String? {
        if (packageName == null || iconResId == 0) return null
        return runCatching { resourcesContext(context, packageName).resources.getResourceEntryName(iconResId) }.getOrNull()
    }

    fun load(
        context: Context,
        packageName: String?,
        iconName: String?,
        type: Int?,
    ): Drawable? {
        if (packageName != null && iconName != null) {
            drawable(context, packageName, iconName)?.let { return it }
        }
        return drawable(context, FRAMEWORK, typeIcons[type] ?: DEFAULT_TYPE_ICON)
    }

    private fun drawable(
        context: Context,
        packageName: String,
        name: String,
    ): Drawable? =
        runCatching {
            val owner = resourcesContext(context, packageName)
            val id = owner.resources.getIdentifier(name, "drawable", packageName)
            if (id == 0) null else ResourcesCompat.getDrawable(owner.resources, id, owner.theme)
        }.getOrNull()

    // Framework drawables resolve through our own resources, so they pick up a usable theme
    private fun resourcesContext(
        context: Context,
        packageName: String,
    ): Context = if (packageName == FRAMEWORK) context else context.createPackageContext(packageName, 0)
}
