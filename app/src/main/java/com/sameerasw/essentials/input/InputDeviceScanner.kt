package com.sameerasw.essentials.input

import com.sameerasw.essentials.shizuku.ShizukuProcessHelper
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
                            product = 0
                        )
                    )
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return devices
    }
}
