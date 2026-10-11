package com.sameerasw.essentials.island.remote

import android.content.Context
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.IslandPriorityEntries

object ProviderPriorities {
    private const val FALLBACK = 58

    fun entries(context: Context, settings: SettingsRepository): List<IslandPriorityEntries.Entry> {
        val trust = ProviderTrustStore(settings)
        return ProviderDiscovery.query(context)
            .filter { trust.isEnabled(it.packageName) }
            .map { IslandPriorityEntries.providerEntry(it.packageName, (it.priority ?: FALLBACK).coerceIn(IslandPriority.NOTIFICATION + 1, IslandPriority.DEFAULT)) }
    }
}
