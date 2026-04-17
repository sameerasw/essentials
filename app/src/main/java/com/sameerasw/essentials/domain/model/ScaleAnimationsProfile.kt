package com.sameerasw.essentials.domain.model

data class ScaleAnimationsProfile(
    val fontScale: Float = 1.0f,
    val fontWeight: Int = 0,
    val animatorDurationScale: Float = 1.0f,
    val transitionAnimationScale: Float = 1.0f,
    val windowAnimationScale: Float = 1.0f,
    val smallestWidth: Int = 360,
    val touchSensitivityEnabled: Boolean = false
)
