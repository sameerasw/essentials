package com.sameerasw.essentials.weather.effects

import android.content.Context
import com.sameerasw.essentials.utils.HapticUtil

class DeviceWeatherHaptics(private val context: Context) : WeatherEffectHaptics {
    override fun onDrop(intensity: Float, weight: Float) =
        HapticUtil.performCustomHaptic(context, (0.22f + 0.22f * intensity) * (0.7f + 0.6f * weight))

    override fun onHail(intensity: Float, weight: Float) =
        HapticUtil.performCustomHaptic(context, (0.4f + 0.2f * intensity) * (0.8f + 0.4f * weight))

    override fun onStrike(intensity: Float) = HapticUtil.performThunderRumble(context, intensity)
}
