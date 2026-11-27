/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: App Ops
 * File: AppOpsViewModel.kt
 * Description: Loads installed apps and their app op modes, and applies mode changes.
 */

package com.sameerasw.essentials.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.appops.AppOpEntry
import com.sameerasw.essentials.domain.appops.AppOpInfo
import com.sameerasw.essentials.domain.appops.AppOpMode
import com.sameerasw.essentials.domain.appops.AppOpsCatalog
import com.sameerasw.essentials.domain.appops.AppOpsParser
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class AppOpsViewModel : ViewModel() {
    val apps = mutableStateOf<List<NotificationApp>?>(null)
    val ops = mutableStateOf<List<AppOpInfo>>(emptyList())
    val entries = mutableStateOf<Map<String, Map<String, AppOpEntry>>>(emptyMap())
    val isScanning = mutableStateOf(false)
    val hasScanned = mutableStateOf(false)
    val showSystemApps = mutableStateOf(false)

    private var catalog: List<AppOpInfo> = emptyList()

    fun load(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            if (apps.value == null) {
                val loaded = AppUtil.getInstalledApps(appContext, includeSelf = true)
                catalog = withContext(Dispatchers.Default) { AppOpsCatalog.load() }
                apps.value = loaded
                ops.value = catalog
            }
            if (!hasScanned.value && ShellUtils.hasPermission(appContext)) scan(appContext)
        }
    }

    fun rescan(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch { if (ShellUtils.hasPermission(appContext)) scan(appContext) }
    }

    private suspend fun scan(context: Context) {
        if (isScanning.value) return
        val packages = apps.value?.map { it.packageName } ?: return
        isScanning.value = true
        val gate = Semaphore(SCAN_PARALLELISM)
        val scanned =
            withContext(Dispatchers.IO) {
                packages
                    .chunked(SCAN_CHUNK_SIZE)
                    .map { chunk ->
                        async {
                            gate.withPermit {
                                val output =
                                    ShellUtils.runCommandWithOutput(
                                        context,
                                        AppOpsParser.batchCommand(chunk),
                                        notifyOnError = false,
                                    )
                                output?.let(AppOpsParser::parseBatch).orEmpty()
                            }
                        }
                    }.awaitAll()
                    .fold(emptyMap<String, Map<String, AppOpEntry>>()) { acc, map -> acc + map }
            }
        entries.value = scanned
        ops.value = mergeCatalog(scanned)
        hasScanned.value = true
        isScanning.value = false
    }

    private fun mergeCatalog(scanned: Map<String, Map<String, AppOpEntry>>): List<AppOpInfo> {
        val known = catalog.map { it.name }.toSet()
        val extra =
            scanned.values
                .flatMap { it.keys }
                .filter { it !in known }
                .distinct()
                .map { AppOpInfo(it, null, AppOpMode.DEFAULT) }
        return (catalog + extra).sortedBy { it.name }
    }

    private suspend fun refreshPackage(
        context: Context,
        packageName: String,
    ) {
        val output =
            withContext(Dispatchers.IO) {
                ShellUtils.runCommandWithOutput(context, AppOpsParser.batchCommand(listOf(packageName)), notifyOnError = false)
            } ?: return
        val parsed = AppOpsParser.parseBatch(output)[packageName] ?: emptyMap()
        entries.value = entries.value + (packageName to parsed)
    }

    fun setMode(
        context: Context,
        packageName: String,
        op: String,
        mode: AppOpMode,
        onResult: (Boolean) -> Unit = {},
    ) {
        if (!AppOpsParser.isValidPackage(packageName) || !AppOpsCatalog.isValidOpName(op)) {
            onResult(false)
            return
        }
        val appContext = context.applicationContext
        val useUid = entries.value[packageName]?.get(op)?.isUidMode == true
        viewModelScope.launch {
            val output =
                withContext(Dispatchers.IO) {
                    ShellUtils.runCommandWithOutput(
                        appContext,
                        "appops set ${if (useUid) "--uid " else ""}$packageName $op ${mode.shell} 2>&1",
                        featureName = appContext.getString(R.string.feat_app_ops_title),
                    )
                }
            refreshPackage(appContext, packageName)
            onResult(output != null && !output.looksLikeError())
        }
    }

    fun resetApp(
        context: Context,
        packageName: String,
        onResult: (Boolean) -> Unit = {},
    ) {
        if (!AppOpsParser.isValidPackage(packageName)) {
            onResult(false)
            return
        }
        val appContext = context.applicationContext
        viewModelScope.launch {
            val output =
                withContext(Dispatchers.IO) {
                    ShellUtils.runCommandWithOutput(
                        appContext,
                        "appops reset $packageName 2>&1",
                        featureName = appContext.getString(R.string.feat_app_ops_title),
                    )
                }
            refreshPackage(appContext, packageName)
            onResult(output != null && !output.looksLikeError())
        }
    }

    private fun String.looksLikeError(): Boolean {
        val text = lowercase()
        return "error" in text || "exception" in text || "unknown" in text || "usage:" in text
    }

    private companion object {
        const val SCAN_CHUNK_SIZE = 30
        const val SCAN_PARALLELISM = 4
    }
}
