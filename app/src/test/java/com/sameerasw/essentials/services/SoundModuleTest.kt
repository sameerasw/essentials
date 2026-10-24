/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Services
 * File: SoundModuleTest.kt
 * Description: Unit tests for SoundModule ringer mode mapping.
 */

package com.sameerasw.essentials.services

import android.media.AudioManager
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.modules.SoundModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundModuleTest {
    @Test
    fun mapsNormalRingerModeToSound() {
        assertEquals(Action.SoundModeType.SOUND, SoundModule.soundModeOf(AudioManager.RINGER_MODE_NORMAL))
    }

    @Test
    fun mapsVibrateRingerModeToVibrate() {
        assertEquals(Action.SoundModeType.VIBRATE, SoundModule.soundModeOf(AudioManager.RINGER_MODE_VIBRATE))
    }

    @Test
    fun mapsSilentRingerModeToSilent() {
        assertEquals(Action.SoundModeType.SILENT, SoundModule.soundModeOf(AudioManager.RINGER_MODE_SILENT))
    }

    @Test
    fun returnsNullForUnknownRingerMode() {
        assertNull(SoundModule.soundModeOf(-1))
        assertNull(SoundModule.soundModeOf(999))
    }
}
