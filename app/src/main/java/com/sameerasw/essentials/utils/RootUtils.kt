package com.sameerasw.essentials.utils

import java.io.DataOutputStream
import java.io.IOException

object RootUtils {

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "which su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun isRootPermissionGranted(): Boolean {
        // In many root managers, 'su -c id' will return 0 if granted
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: IOException) {
            false
        } catch (e: InterruptedException) {
            false
        } finally {
            try { os?.close() } catch (e: Exception) {}
            try { process?.destroy() } catch (e: Exception) {}
        }
    }

    fun newProcess(cmd: Array<String>): Process? {
        return try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd.joinToString(" ")))
        } catch (e: Exception) {
            null
        }
    }
}
