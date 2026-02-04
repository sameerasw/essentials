package com.sameerasw.essentials.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.Wearable

class WatchViewModel : ViewModel() {
    val isWatchDetected = mutableStateOf(false)

    fun check(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            isWatchDetected.value = nodes.isNotEmpty()
        }.addOnFailureListener {
            isWatchDetected.value = false
        }
    }
}
