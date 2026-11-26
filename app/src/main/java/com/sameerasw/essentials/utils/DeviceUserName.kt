package com.sameerasw.essentials.utils

import android.content.Context
import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceUserName {
    private val userInfoRegex = Regex("""UserInfo\{(\d+):([^:}]+):""")

    // UserManager.getUserName() is restricted to privileged callers, so read it through Shizuku/root
    fun firstName(context: Context): String? {
        if (!ShellUtils.isAvailable(context) || !ShellUtils.hasPermission(context)) return null
        val output = ShellUtils.runCommandWithOutput(context, "pm list users", notifyOnError = false) ?: return null
        val myUserId = Process.myUserHandle().hashCode().toString()
        return userInfoRegex
            .findAll(output)
            .firstOrNull { it.groupValues[1] == myUserId }
            ?.groupValues
            ?.get(2)
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}

@Composable
fun rememberDeviceUserFirstName(): String? {
    val context = LocalContext.current
    val name by produceState<String?>(null, context) {
        value = runCatching { withContext(Dispatchers.IO) { DeviceUserName.firstName(context) } }.getOrNull()
    }
    return name
}
