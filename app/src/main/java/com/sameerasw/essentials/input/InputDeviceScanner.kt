/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Input
 * File: InputDeviceScanner.kt
 * Description: Component file for InputDeviceScanner.kt.
 */

package com.sameerasw.essentials.input

import android.content.Context
import com.sameerasw.essentials.shizuku.ShizukuProcessHelper
import com.sameerasw.essentials.utils.ShellUtils
import java.io.BufferedReader
import java.io.InputStreamReader

class InputDeviceScanner {
    fun scanForVolumeDevices(): List<InputDevice> {
        val devices = mutableListOf<InputDevice>()
        try {
            val process =
                ShizukuProcessHelper.newProcess(arrayOf("ls", "/dev/input")) ?: return devices

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.matches(Regex("event\\d+"))) {
                    val path = "/dev/input/$line"
                    devices.add(
                        InputDevice(
                            path = path,
                            name = line,
                            bus = 0,
                            vendor = 0,
                            product = 0,
                        ),
                    )
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return devices
    }

    fun scanForPowerDevices(context: Context): List<String> {
        val output = ShellUtils.runCommandWithOutput(context, "getevent -pl", notifyOnError = false)
        return if (output != null) parsePowerDevices(output) else emptyList()
    }

    companion object {
        fun parsePowerDevices(output: String): List<String> {
            val result = mutableListOf<String>()
            var currentPath: String? = null
            var currentPathAdded = false

            output.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("add device")) {
                    currentPath = line.substringAfterLast(": ").trim()
                    currentPathAdded = false
                } else if (currentPath != null && !currentPathAdded) {
                    val tokens = line.split(Regex("\\s+"))
                    if (tokens.contains("KEY_POWER")) {
                        result.add(currentPath!!)
                        currentPathAdded = true
                    }
                }
            }
            return result
        }
    }
}
