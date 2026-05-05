package com.sameerasw.essentials.services

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class EssentialsWearableListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "EssentialsWearableListener"
        private const val PATH_REQUEST_SYNC = "/request_device_info_sync"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: ${messageEvent.path}")
        if (messageEvent.path == PATH_REQUEST_SYNC) {
            DeviceInfoSyncManager.forceSync(this)
        }
    }
}
