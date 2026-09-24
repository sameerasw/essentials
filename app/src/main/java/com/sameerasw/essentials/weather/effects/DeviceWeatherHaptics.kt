package com.sameerasw.essentials.weather.effects

import android.content.Context
import com.sameerasw.essentials.utils.HapticUtil

class DeviceWeatherHaptics(private val context: Context) : WeatherEffectHaptics {
    override fun onDrop(intensity: Float) = HapticUtil.performCustomHaptic(context, 0.2f + 0.2f * intensity)

    override fun onHail(intensity: Float) = HapticUtil.performCustomHaptic(context, 0.4f + 0.2f * intensity)

    override fun onStrike(intensity: Float) = HapticUtil.performThunderRumble(context, intensity)
}
