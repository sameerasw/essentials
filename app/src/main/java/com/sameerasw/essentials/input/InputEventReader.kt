package com.sameerasw.essentials.input

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.InputStream

import rikka.shizuku.Shizuku
import com.sameerasw.essentials.shizuku.ShizukuProcessHelper

class InputEventReader(private val devicePath: String) {
    companion object {
        const val INPUT_EVENT_SIZE = 24
        const val EV_KEY = 1
        const val KEY_VOLUMEUP = 115
        const val KEY_VOLUMEDOWN = 114
    }

    private var process: Process? = null
    private var inputStream: InputStream? = null
    private val buffer = ByteArray(INPUT_EVENT_SIZE)

    fun open(): Boolean = try {
        process = com.sameerasw.essentials.utils.ShellUtils.newProcess(
            com.sameerasw.essentials.EssentialsApp.context,
            arrayOf("cat", devicePath)
        )
        inputStream = process?.inputStream
        inputStream != null
    } catch (e: Exception) { 
        e.printStackTrace()
        false 
    }

    fun readEvent(): InputEvent? {
        return try {
            // Read loop to ensure full buffer is filled
            var bytesRead = 0
            while (bytesRead < INPUT_EVENT_SIZE) {
                val result = inputStream?.read(buffer, bytesRead, INPUT_EVENT_SIZE - bytesRead) ?: -1
                if (result == -1) return null
                bytesRead += result
            }

            val bb = ByteBuffer.wrap(buffer).apply { order(ByteOrder.LITTLE_ENDIAN) }
            InputEvent(
                timeSecond = bb.long,
                timeMicro = bb.long,
                type = bb.short.toInt() and 0xFFFF,
                code = bb.short.toInt() and 0xFFFF,
                value = bb.int
            )
        } catch (e: Exception) { null }
    }

    fun close() { 
        try {
            process?.destroy()
            inputStream?.close()
        } catch (e: Exception) {}
    }
}
