package com.sameerasw.essentials.external

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExternalActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_EXTERNAL_CONTROL = "com.sameerasw.essentials.action.EXTERNAL_CONTROL"
        const val EXTRA_PATH = "path"
        const val EXTRA_ACTION = "action"
        const val EXTRA_VALUE = "value"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXTERNAL_CONTROL) return

        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION)
        val value = intent.getStringExtra(EXTRA_VALUE)

        Log.d("ExternalActionReceiver", "Received external control request: path=$path, action=$action, value=$value")

        if (action == "update") {
            ExternalRouter.update(context, path, value, intent.extras)
        } else if (action != null) {
            ExternalRouter.action(context, path, action, intent.extras)
        }
    }
}
