package com.sameerasw.essentials.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCareParsingTest {
    @Test
    fun parseProcessPackagesDropsHeaderBlankLinesAndSuffixes() {
        val output = "NAME\ncom.a\ncom.a:remote\n\ncom.b:x\ninit\n"
        val packages = DeviceCare.parseProcessPackages(output)
        assertTrue(packages.contains("com.a"))
        assertTrue(packages.contains("com.b"))
        assertFalse(packages.contains("NAME"))
        assertFalse(packages.contains(""))
    }

    @Test
    fun parseCompanionPackagesFindsPackagesInVariousFormats() {
        val output1 = "Association{id=1, packageName='com.watch', ...}"
        val packages1 = DeviceCare.parseCompanionPackages(output1)
        assertTrue(packages1.contains("com.watch"))

        val output2 = "id=2 pkg=com.watch.two"
        val packages2 = DeviceCare.parseCompanionPackages(output2)
        assertTrue(packages2.contains("com.watch.two"))

        val output3 = "id=2 pkg=com.watch"
        val packages3 = DeviceCare.parseCompanionPackages(output3)
        assertTrue(packages3.contains("com.watch"))
    }
}
