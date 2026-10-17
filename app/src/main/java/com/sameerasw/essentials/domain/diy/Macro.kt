/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Macro.kt
 * Description: Domain model and business logic entry for Macro.kt.
 */

package com.sameerasw.essentials.domain.diy

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R

@Keep
data class Macro(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("actions") val actions: List<Action> = emptyList(),
) {
    // Gson leaves missing lists null despite the default
    @Suppress("USELESS_ELVIS")
    val actionList: List<Action> get() = actions ?: emptyList()
}

// RunMacro shows the macro's name; every other action shows its title
fun Action.displayName(context: Context): String =
    if (this is Action.RunMacro) {
        DIYRepository.getMacro(macroId)?.name ?: context.getString(R.string.diy_macro_deleted_label)
    } else {
        context.getString(title)
    }

// Macros match only the same macro; other actions match by type so their saved settings carry over
fun Action.matches(other: Action?): Boolean = if (this is Action.RunMacro) this == other else other != null && this::class == other::class
