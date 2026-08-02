package com.sameerasw.essentials.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.Wearable

import com.sameerasw.essentials.data.repository.SettingsRepository

class WatchViewModel : ViewModel() {
    val isWatchDetected = mutableStateOf(false)
    val connectedWatchName = mutableStateOf<String?>(null)
    val remoteLockMode = mutableStateOf(0) // 0: Screen off, 1: Lock

    val watchVersionCode = mutableStateOf(0)
    val isWearUpdateRequired = mutableStateOf(false)

    fun load(repository: SettingsRepository) {
        remoteLockMode.value = repository.getInt(SettingsRepository.KEY_REMOTE_LOCK_MODE, 0)
    }

    fun setRemoteLockMode(mode: Int, repository: SettingsRepository) {
        remoteLockMode.value = mode
        repository.putInt(SettingsRepository.KEY_REMOTE_LOCK_MODE, mode)
    }

    fun check(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val storedVersion = prefs.getInt("watch_version_code", 0)
        watchVersionCode.value = storedVersion

        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val detected = nodes.isNotEmpty()
            isWatchDetected.value = detected
            connectedWatchName.value = nodes.firstOrNull()?.displayName

            // Wear app version is lower than required (or not yet reported)
            isWearUpdateRequired.value = detected && storedVersion < com.sameerasw.essentials.BuildConfig.REQUIRED_WEAR_VERSION_CODE

            if (detected) {
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/request_watch_status", byteArrayOf())
                }
            }
        }.addOnFailureListener {
            isWatchDetected.value = false
            connectedWatchName.value = null
            isWearUpdateRequired.value = false
        }
    }

    fun openPlayStoreOnWatch(context: Context) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setData(android.net.Uri.parse("market://details?id=com.sameerasw.essentials"))
            .addCategory(android.content.Intent.CATEGORY_BROWSABLE)

        val remoteActivityHelper = androidx.wear.remote.interactions.RemoteActivityHelper(context)
        remoteActivityHelper.startRemoteActivity(intent)
    }
}
