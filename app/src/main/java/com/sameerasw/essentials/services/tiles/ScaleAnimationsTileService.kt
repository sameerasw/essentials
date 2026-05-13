package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ScaleAnimationsProfile

class ScaleAnimationsTileService : BaseTileService() {

    private val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun getTileLabel(): String = getString(R.string.tile_scale_animations)

    override fun getTileSubtitle(): String {
        val mode = settingsRepository.getScaleAnimationsMode()
        return if (mode == "glove") getString(R.string.label_enabled) else getString(R.string.label_disabled)
    }

    override fun hasFeaturePermission(): Boolean {
        return true
    }

    override fun getTileIcon(): Icon {
        val mode = settingsRepository.getScaleAnimationsMode()
        val iconRes =
            if (mode == "glove") R.drawable.round_front_hand_24 else R.drawable.rounded_front_hand_24
        return Icon.createWithResource(this, iconRes)
    }

    override fun getTileState(): Int {
        val mode = settingsRepository.getScaleAnimationsMode()
        return if (mode == "glove") Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val currentMode = settingsRepository.getScaleAnimationsMode()
        val oldMode = currentMode
        val newMode = if (currentMode == "glove") "default" else "glove"

        // 1. Save current active values to old profile
        val currentProfile = ScaleAnimationsProfile(
            fontScale = settingsRepository.getFontScale(),
            fontWeight = settingsRepository.getFontWeight(),
            animatorDurationScale = settingsRepository.getAnimationScale(android.provider.Settings.Global.ANIMATOR_DURATION_SCALE),
            transitionAnimationScale = settingsRepository.getAnimationScale(android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE),
            windowAnimationScale = settingsRepository.getAnimationScale(android.provider.Settings.Global.WINDOW_ANIMATION_SCALE),
            smallestWidth = settingsRepository.getSmallestWidth(),
            touchSensitivityEnabled = settingsRepository.getTouchSensitivityEnabled(),
            autoRotateEnabled = settingsRepository.getAutoRotateEnabled(),
            screenTimeout = settingsRepository.getScreenTimeout()
        )
        settingsRepository.saveScaleAnimationsProfile(oldMode, currentProfile)

        // 2. Load and Apply new profile
        val newProfile = settingsRepository.getScaleAnimationsProfile(newMode)
        settingsRepository.setScaleAnimationsMode(newMode)

        applyProfile(newProfile)

    }

    private fun applyProfile(profile: ScaleAnimationsProfile) {
        settingsRepository.setFontScale(profile.fontScale)
        settingsRepository.setFontWeight(profile.fontWeight)
        settingsRepository.setAnimationScale(
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            profile.animatorDurationScale
        )
        settingsRepository.setAnimationScale(
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
            profile.transitionAnimationScale
        )
        settingsRepository.setAnimationScale(
            android.provider.Settings.Global.WINDOW_ANIMATION_SCALE,
            profile.windowAnimationScale
        )
        settingsRepository.setSmallestWidth(profile.smallestWidth)
        settingsRepository.setTouchSensitivityEnabled(profile.touchSensitivityEnabled)
        settingsRepository.setAutoRotateEnabled(profile.autoRotateEnabled)
        settingsRepository.setScreenTimeout(profile.screenTimeout)
    }
}
