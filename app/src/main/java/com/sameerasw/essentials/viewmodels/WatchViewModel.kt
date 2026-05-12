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

    fun load(repository: SettingsRepository) {
        remoteLockMode.value = repository.getInt(SettingsRepository.KEY_REMOTE_LOCK_MODE, 0)
    }

    fun setRemoteLockMode(mode: Int, repository: SettingsRepository) {
        remoteLockMode.value = mode
        repository.putInt(SettingsRepository.KEY_REMOTE_LOCK_MODE, mode)
    }

    fun check(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            isWatchDetected.value = nodes.isNotEmpty()
            connectedWatchName.value = nodes.firstOrNull()?.displayName
        }.addOnFailureListener {
            isWatchDetected.value = false
            connectedWatchName.value = null
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
