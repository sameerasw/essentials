/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: AppTour.kt
 * Description: First-run guided tour state.
 */

package com.sameerasw.essentials.ui.state

import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.DIYTabs

enum class TourStep(
    @StringRes val textRes: Int,
) {
    TOOLBAR(R.string.tour_toolbar),
    FEATURE_ITEM(R.string.tour_feature_item),
    SEARCH_FAB(R.string.tour_search_fab),
    BUG_REPORT_FAB(R.string.tour_bug_report_fab),
    HELP_FAB(R.string.tour_help_fab),
    DESIGN_SWITCH(R.string.tour_design_switch),
}

object AppTour {
    private var repository: SettingsRepository? = null
    private var seen by mutableStateOf(emptySet<TourStep>())
    private val anchors = mutableStateMapOf<TourStep, Int>()

    var enabled by mutableStateOf(false)
    var homeTab by mutableStateOf<DIYTabs?>(null)

    val current: TourStep? by derivedStateOf {
        if (!enabled) null else TourStep.entries.firstOrNull { it !in seen && (anchors[it] ?: 0) > 0 }
    }

    fun init(repository: SettingsRepository) {
        this.repository = repository
        seen =
            repository
                .getString(SettingsRepository.KEY_TOUR_SEEN_STEPS)
                .orEmpty()
                .split(',')
                .mapNotNull { name -> TourStep.entries.find { it.name == name } }
                .toSet()
    }

    fun markSeen(step: TourStep) {
        if (step in seen) return
        seen = seen + step
        repository?.putString(SettingsRepository.KEY_TOUR_SEEN_STEPS, seen.joinToString(",") { it.name })
    }

    fun reset() {
        seen = emptySet()
        repository?.putString(SettingsRepository.KEY_TOUR_SEEN_STEPS, null)
    }

    internal fun attach(step: TourStep) {
        anchors[step] = (anchors[step] ?: 0) + 1
    }

    internal fun detach(step: TourStep) {
        val count = (anchors[step] ?: 0) - 1
        if (count > 0) anchors[step] = count else anchors.remove(step)
    }
}
