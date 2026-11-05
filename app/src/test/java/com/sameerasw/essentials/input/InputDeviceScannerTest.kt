package com.sameerasw.essentials.input

import org.junit.Assert.assertEquals
import org.junit.Test

class InputDeviceScannerTest {
    @Test
    fun parsePowerDevicesExtractsPowerDevices() {
        val output =
            """
            add device 1: /dev/input/event12
              name:     "qwerty2"
              events:
                KEY (0001): KEY_ESC               KEY_1                 KEY_VOLUMEDOWN        KEY_VOLUMEUP
                            KEY_POWER             KEY_KPEQUAL
            add device 2: /dev/input/event11
              name:     "virtio_input_multi_touch_11"
              events:
                ABS (0003): ABS_MT_SLOT           : value 0, min 0, max 9, fuzz 0, flat 0, resolution 0
            add device 13: /dev/input/event0
              name:     "gpio-keys"
              events:
                KEY (0001): KEY_POWER
            """.trimIndent()

        val devices = InputDeviceScanner.parsePowerDevices(output)
        assertEquals(listOf("/dev/input/event12", "/dev/input/event0"), devices)
    }

    @Test
    fun parsePowerDevicesIgnoresSimilarKeyNames() {
        val output =
            """
            add device 1: /dev/input/event5
              name:     "remote_control"
              events:
                KEY (0001): KEY_POWER2
            """.trimIndent()

        val devices = InputDeviceScanner.parsePowerDevices(output)
        assertEquals(emptyList<String>(), devices)
    }

    @Test
    fun parsePowerDevicesEmptyOutputReturnsEmptyList() {
        assertEquals(emptyList<String>(), InputDeviceScanner.parsePowerDevices(""))
    }
}
