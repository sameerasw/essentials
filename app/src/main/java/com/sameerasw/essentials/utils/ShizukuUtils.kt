package com.sameerasw.essentials.utils

import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

object ShizukuUtils {

    private var binder: IBinder? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        binder = Shizuku.getBinder()
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1003)
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binder = null
    }

    private val isBinderAlive: Boolean
        get() = binder?.isBinderAlive == true

    fun initialize() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    fun hasPermission(): Boolean {
        if (!isBinderAlive) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
            true
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            false
        }
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(1003)
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            // Permission request failed
        }
    }

    fun runCommand(command: String) {
        if (!hasPermission() || !isBinderAlive) return

        val service = IShizukuService.Stub.asInterface(binder)
        try {
            val process = service.newProcess(arrayOf("sh", "-c", command), null, "/")
            process?.waitFor()
        } catch (e: RemoteException) {
            // Command execution failed
        }
    }

    fun grantWriteSecureSettingsPermission(): Boolean {
        if (!hasPermission() || !isBinderAlive) return false

        return try {
            runCommand("pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS")
            true
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            false
        }
    }
}
