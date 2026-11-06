/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Media
 * File: AppVolumeController.kt
 * Description: Applies saved per-app volume levels to each app's audio players through Shizuku.
 */

package com.sameerasw.essentials.utils.media

import android.content.Context
import android.media.AudioPlaybackConfiguration
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.util.Log
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ShizukuUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object AppVolumeController {
    private const val TAG = "AppVolumeController"
    private const val AUDIO_SERVICE = "android.media.IAudioService"
    private const val DISPATCHER = "android.media.IPlaybackConfigDispatcher"
    private const val PLAYER_STATE_STARTED = 2
    private const val PER_USER_RANGE = 100000

    private val handler = Handler(Looper.getMainLooper())
    private val appliedVolumes = mutableMapOf<Int, Float>()
    private var configs: List<AudioPlaybackConfiguration> = emptyList()
    private var levels: Map<String, Float> = emptyMap()
    private var enabled = false
    private var appContext: Context? = null
    private var dispatcher: Any? = null
    private var exemptionsApplied = false
    private var waitingForShizuku = false

    private val _playingPackages = MutableStateFlow<List<String>>(emptyList())
    val playingPackages: StateFlow<List<String>> = _playingPackages.asStateFlow()

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            handler.post { appContext?.let { updateState(it) } }
        }

    fun updateState(context: Context) {
        val repository = SettingsRepository(context)
        appContext = context.applicationContext
        enabled = repository.isAppVolumeEnabled()
        levels = repository.getAppVolumeLevels()
        if (!enabled) {
            stop()
            return
        }
        if (dispatcher == null && !register()) {
            if (!waitingForShizuku) {
                waitingForShizuku = true
                Shizuku.addBinderReceivedListener(binderReceivedListener)
            }
            return
        }
        apply()
    }

    fun preview(
        packageName: String,
        level: Float,
    ) {
        levels = levels + (packageName to level)
        apply()
    }

    fun refresh() {
        appContext?.let { updateState(it) }
    }

    fun stop() {
        enabled = false
        apply()
        dispatcher?.let { callback ->
            runCatching {
                val service = audioService()
                service.javaClass.getMethod("unregisterPlaybackCallback", Class.forName(DISPATCHER)).invoke(service, callback)
            }.onFailure { Log.w(TAG, "Could not unregister playback callback", it) }
        }
        dispatcher = null
        configs = emptyList()
        _playingPackages.value = emptyList()
        if (waitingForShizuku) {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            waitingForShizuku = false
        }
    }

    private fun register(): Boolean {
        if (!ShizukuUtils.hasPermission()) return false
        return try {
            applyHiddenApiExemptions()
            val service = audioService()
            val callback =
                Class
                    .forName("$DISPATCHER\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                    .invoke(null, PlaybackDispatcher())
            service.javaClass.getMethod("registerPlaybackCallback", Class.forName(DISPATCHER)).invoke(service, callback)
            dispatcher = callback
            @Suppress("UNCHECKED_CAST")
            onConfigs(service.javaClass.getMethod("getActivePlaybackConfigurations").invoke(service) as List<AudioPlaybackConfiguration>)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register for audio playback changes", e)
            false
        }
    }

    private fun onConfigs(updated: List<AudioPlaybackConfiguration>) {
        configs = updated
        apply(force = true)
        _playingPackages.value = updated.filter { invoke(it, "getPlayerState") == PLAYER_STATE_STARTED }.mapNotNull { packageOf(it) }.distinct()
    }

    private fun apply(force: Boolean = false) {
        val active = if (enabled) levels else emptyMap()
        val ids = mutableSetOf<Int>()
        configs.forEach { config ->
            val id = invoke(config, "getPlayerInterfaceId") as? Int ?: return@forEach
            ids += id
            val target = packageOf(config)?.let { active[it] }?.let { it * it } ?: 1f
            if (!force && (appliedVolumes[id] ?: 1f) == target) return@forEach
            try {
                val proxy = invoke(config, "getPlayerProxy") ?: return@forEach
                proxy.javaClass.getMethod("setVolume", Float::class.javaPrimitiveType).invoke(proxy, target)
                appliedVolumes[id] = target
            } catch (e: Exception) {
                Log.w(TAG, "Could not set volume for player $id", e)
            }
        }
        appliedVolumes.keys.retainAll(ids)
    }

    private fun packageOf(config: AudioPlaybackConfiguration): String? {
        val uid = invoke(config, "getClientUid") as? Int ?: return null
        if (uid < Process.FIRST_APPLICATION_UID) return null
        if (uid / PER_USER_RANGE != Process.myUid() / PER_USER_RANGE) return null
        val packages = try {
            appContext?.packageManager?.getPackagesForUid(uid)
        } catch (e: Exception) {
            null
        } ?: return null
        return packages.firstOrNull { it in levels } ?: packages.firstOrNull()
    }

    private fun invoke(
        config: AudioPlaybackConfiguration,
        method: String,
    ): Any? = runCatching { AudioPlaybackConfiguration::class.java.getMethod(method).invoke(config) }.getOrNull()

    private fun audioService(): Any {
        val binder = requireNotNull(SystemServiceHelper.getSystemService("audio")) { "audio service unavailable" }
        return Class
            .forName("$AUDIO_SERVICE\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(binder))!!
    }

    private fun applyHiddenApiExemptions() {
        if (Build.VERSION.SDK_INT >= 28 && !exemptionsApplied) {
            exemptionsApplied = HiddenApiBypass.addHiddenApiExemptions("Landroid/media/")
            if (!exemptionsApplied) Log.w(TAG, "Hidden API exemptions were not applied")
        }
    }

    private class PlaybackDispatcher : Binder() {
        private val dispatchCode =
            Class
                .forName("$DISPATCHER\$Stub")
                .getDeclaredField("TRANSACTION_dispatchPlaybackConfigChange")
                .apply { isAccessible = true }
                .getInt(null)

        override fun getInterfaceDescriptor(): String = DISPATCHER

        override fun onTransact(
            code: Int,
            data: Parcel,
            reply: Parcel?,
            flags: Int,
        ): Boolean {
            if (code != dispatchCode) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(DISPATCHER)
            val updated = data.createTypedArrayList(AudioPlaybackConfiguration.CREATOR).orEmpty()
            handler.post { onConfigs(updated) }
            return true
        }
    }
}
